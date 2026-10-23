package com.servicehub.service;

import com.servicehub.exception.BusinessRuleException;
import com.servicehub.exception.ResourceNotFoundException;
import com.servicehub.model.RequestStatus;
import com.servicehub.model.Review;
import com.servicehub.model.ServiceOffering;
import com.servicehub.model.ServiceRequest;
import com.servicehub.model.dto.ReviewCreateDTO;
import com.servicehub.model.dto.ReviewResponseDTO;
import com.servicehub.repository.ReviewRepository;
import com.servicehub.repository.ServiceOfferingRepository;
import com.servicehub.repository.ServiceRequestRepository;
import com.servicehub.security.AuthUser;
import com.servicehub.security.CurrentUser;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Avaliações (RN09). */
@Service
@Transactional
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ServiceRequestRepository requestRepository;
    private final ServiceOfferingRepository serviceRepository;
    private final CurrentUser currentUser;

    public ReviewService(ReviewRepository reviewRepository, ServiceRequestRepository requestRepository,
                         ServiceOfferingRepository serviceRepository, CurrentUser currentUser) {
        this.reviewRepository = reviewRepository;
        this.requestRepository = requestRepository;
        this.serviceRepository = serviceRepository;
        this.currentUser = currentUser;
    }

    /** Só quem abriu a ordem, só depois de COMPLETED, uma única vez. */
    public ReviewResponseDTO create(Long requestId, ReviewCreateDTO dto) {
        AuthUser me = currentUser.get();
        ServiceRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Ordem de serviço não encontrada: " + requestId));
        if (!request.getClient().getId().equals(me.id())) {
            throw new AccessDeniedException("Só quem contratou o serviço pode avaliá-lo.");
        }
        if (request.getStatus() != RequestStatus.COMPLETED) {
            throw new BusinessRuleException("Só é possível avaliar ordens concluídas (status atual: "
                    + request.getStatus() + ").");
        }
        if (reviewRepository.existsByRequest_Id(requestId)) {
            throw new BusinessRuleException("Esta ordem já foi avaliada.");
        }
        Review review = new Review();
        review.setRequest(request);
        review.setReviewer(request.getClient());
        review.setRating(dto.rating().shortValue());
        review.setComment(TextSanitizer.clean(dto.comment()));
        try {
            return toResponse(reviewRepository.saveAndFlush(review));
        } catch (DataIntegrityViolationException e) {
            throw new BusinessRuleException("Esta ordem já foi avaliada.");
        }
    }

    /** Público; serviço inativo responde 404, exceto para o dono e o ADMIN (RN12). */
    @Transactional(readOnly = true)
    public Page<ReviewResponseDTO> byService(Long serviceId, Pageable pageable) {
        ServiceOffering service = serviceRepository.findById(serviceId)
                .filter(s -> s.isActive() || canManage(s))
                .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado: " + serviceId));
        return reviewRepository.findByRequest_Service_Id(service.getId(), pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ReviewResponseDTO findById(Long id) {
        return reviewRepository.findById(id).map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Avaliação não encontrada: " + id));
    }

    private boolean canManage(ServiceOffering service) {
        return currentUser.find()
                .map(u -> u.isAdmin() || u.id().equals(service.getProvider().getId()))
                .orElse(false);
    }

    private ReviewResponseDTO toResponse(Review r) {
        return new ReviewResponseDTO(r.getId(), r.getRequest().getId(), r.getRequest().getService().getId(),
                r.getReviewer().getId(), r.getReviewer().getFullName(), r.getRating().intValue(), r.getComment(),
                r.getCreatedAt());
    }
}
