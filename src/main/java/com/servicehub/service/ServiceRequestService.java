package com.servicehub.service;

import com.servicehub.exception.BusinessRuleException;
import com.servicehub.exception.InvalidRequestException;
import com.servicehub.exception.InvalidStateTransitionException;
import com.servicehub.exception.ResourceNotFoundException;
import com.servicehub.model.RequestStatus;
import com.servicehub.model.ServiceOffering;
import com.servicehub.model.ServiceRequest;
import com.servicehub.model.User;
import com.servicehub.model.dto.ServiceRequestCreateDTO;
import com.servicehub.model.dto.ServiceRequestResponseDTO;
import com.servicehub.repository.ServiceOfferingRepository;
import com.servicehub.repository.ServiceRequestRepository;
import com.servicehub.repository.UserRepository;
import com.servicehub.security.AuthUser;
import com.servicehub.security.CurrentUser;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static com.servicehub.repository.ServiceRequestSpecifications.asClient;
import static com.servicehub.repository.ServiceRequestSpecifications.asProvider;
import static com.servicehub.repository.ServiceRequestSpecifications.hasStatus;

/** Ordens de serviço: RN06, RN07, RN08 e RN13. */
@Service
@Transactional
public class ServiceRequestService {

    public static final String TRANSITIONS_METRIC = "servicehub.service_requests.transitions";

    private final ServiceRequestRepository requestRepository;
    private final ServiceOfferingRepository serviceRepository;
    private final UserRepository userRepository;
    private final CurrentUser currentUser;
    private final MeterRegistry meterRegistry;

    public ServiceRequestService(ServiceRequestRepository requestRepository, ServiceOfferingRepository serviceRepository,
                                 UserRepository userRepository, CurrentUser currentUser, MeterRegistry meterRegistry) {
        this.requestRepository = requestRepository;
        this.serviceRepository = serviceRepository;
        this.userRepository = userRepository;
        this.currentUser = currentUser;
        this.meterRegistry = meterRegistry;
    }

    /** RN06 (quem contrata, serviço ativo, não o próprio) e RN07 (preço congelado). */
    @PreAuthorize("hasAnyRole('CLIENT', 'PROVIDER')")
    public ServiceRequestResponseDTO create(ServiceRequestCreateDTO dto) {
        AuthUser me = currentUser.get();
        ServiceOffering service = serviceRepository.findById(dto.serviceId())
                .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado: " + dto.serviceId()));
        if (!service.isActive()) {
            throw new BusinessRuleException("Serviço inativo não aceita novas ordens.");
        }
        if (service.getProvider().getId().equals(me.id())) {
            throw new BusinessRuleException("Você não pode contratar o seu próprio serviço.");
        }
        User client = userRepository.getReferenceById(me.id());
        ServiceRequest request = new ServiceRequest();
        request.setService(service);
        request.setClient(client);
        request.setStatus(RequestStatus.PENDING);
        request.setTotalPrice(service.getPrice());
        request.setScheduledAt(dto.scheduledAt());
        request.setNotes(TextSanitizer.clean(dto.notes()));
        ServiceRequest saved = requestRepository.saveAndFlush(request);
        count(RequestStatus.PENDING);
        return toResponse(requestRepository.findById(saved.getId()).orElseThrow());
    }

    /** RN13: só as ordens em que o usuário participa; ADMIN vê todas. */
    @Transactional(readOnly = true)
    public Page<ServiceRequestResponseDTO> list(String role, RequestStatus status, Pageable pageable) {
        AuthUser me = currentUser.get();
        List<Specification<ServiceRequest>> specs = new ArrayList<>();
        if (!me.isAdmin() || role != null) {
            specs.add(participation(me.id(), role));
        }
        if (status != null) {
            specs.add(hasStatus(status));
        }
        Specification<ServiceRequest> spec = specs.isEmpty() ? Specification.unrestricted() : Specification.allOf(specs);
        return requestRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ServiceRequestResponseDTO findById(Long id) {
        ServiceRequest request = load(id);
        actorFor(request, currentUser.get());
        return toResponse(request);
    }

    /** RN08: valida a transição na máquina de estados e quem pode executá-la. */
    public ServiceRequestResponseDTO changeStatus(Long id, RequestStatus target) {
        ServiceRequest request = load(id);
        RequestStatus.Actor actor = actorFor(request, currentUser.get());
        RequestStatus from = request.getStatus();
        if (!from.canTransitionTo(target)) {
            throw new InvalidStateTransitionException(from, target);
        }
        if (!from.allowedFor(target, actor)) {
            throw new AccessDeniedException("Seu papel nesta ordem não permite mudar de " + from + " para " + target + ".");
        }
        request.setStatus(target);
        requestRepository.saveAndFlush(request);
        count(target);
        return toResponse(request);
    }

    private Specification<ServiceRequest> participation(Long userId, String role) {
        if (role == null) {
            return Specification.anyOf(asClient(userId), asProvider(userId));
        }
        return switch (role) {
            case "client" -> asClient(userId);
            case "provider" -> asProvider(userId);
            default -> throw new InvalidRequestException("Valor inválido para 'role'. Valores aceitos: [client, provider]");
        };
    }

    /** Papel de quem chama nesta ordem; terceiros recebem 403 (RN13). */
    static RequestStatus.Actor actorFor(ServiceRequest request, AuthUser me) {
        if (me.isAdmin()) {
            return RequestStatus.Actor.ADMIN;
        }
        if (request.getService().getProvider().getId().equals(me.id())) {
            return RequestStatus.Actor.PROVIDER;
        }
        if (request.getClient().getId().equals(me.id())) {
            return RequestStatus.Actor.CLIENT;
        }
        throw new AccessDeniedException("Você não participa desta ordem de serviço.");
    }

    private ServiceRequest load(Long id) {
        return requestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ordem de serviço não encontrada: " + id));
    }

    private void count(RequestStatus to) {
        meterRegistry.counter(TRANSITIONS_METRIC, "to", to.name()).increment();
    }

    ServiceRequestResponseDTO toResponse(ServiceRequest r) {
        ServiceOffering s = r.getService();
        return new ServiceRequestResponseDTO(r.getId(), s.getId(), s.getTitle(), r.getClient().getId(),
                r.getClient().getFullName(), s.getProvider().getId(), r.getStatus(), r.getTotalPrice(),
                r.getScheduledAt(), r.getNotes(), r.getCreatedAt(), r.getUpdatedAt());
    }
}
