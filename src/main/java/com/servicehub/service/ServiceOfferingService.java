package com.servicehub.service;

import com.servicehub.exception.BusinessRuleException;
import com.servicehub.exception.ResourceNotFoundException;
import com.servicehub.model.RequestStatus;
import com.servicehub.model.ServiceOffering;
import com.servicehub.model.User;
import com.servicehub.model.dto.ServiceFilter;
import com.servicehub.model.dto.ServiceOfferingDTO;
import com.servicehub.model.dto.ServiceOfferingResponseDTO;
import com.servicehub.model.dto.ServiceUpdateDTO;
import com.servicehub.repository.ServiceOfferingRepository;
import com.servicehub.repository.ServiceOfferingSpecifications;
import com.servicehub.repository.ServiceRequestRepository;
import com.servicehub.repository.UserRepository;
import com.servicehub.security.AuthUser;
import com.servicehub.security.CurrentUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ServiceOfferingService {

    private final ServiceOfferingRepository serviceOfferingRepository;
    private final UserRepository userRepository;
    private final CurrentUser currentUser;
    private final ServiceRequestRepository requestRepository;

    public ServiceOfferingService(ServiceOfferingRepository serviceOfferingRepository, UserRepository userRepository,
                                  CurrentUser currentUser, ServiceRequestRepository requestRepository) {
        this.serviceOfferingRepository = serviceOfferingRepository;
        this.userRepository = userRepository;
        this.currentUser = currentUser;
        this.requestRepository = requestRepository;
    }

    /** RN04: o prestador é sempre o usuário autenticado. */
    @PreAuthorize("hasRole('PROVIDER')")
    public ServiceOfferingResponseDTO createService(ServiceOfferingDTO dto) {
        User provider = userRepository.findById(currentUser.get().id())
                .orElseThrow(() -> new ResourceNotFoundException("Prestador não encontrado."));
        ServiceOffering service = new ServiceOffering();
        apply(service, dto);
        service.setProvider(provider);
        return toResponseDTO(serviceOfferingRepository.save(service));
    }

    /** RN12: serviço inativo responde 404 para quem não é o dono nem ADMIN. */
    @Transactional(readOnly = true)
    public ServiceOfferingResponseDTO findById(Long id) {
        ServiceOffering service = findEntityById(id);
        if (!service.isActive() && !canManage(service)) {
            throw notFound(id);
        }
        return toResponseDTO(service);
    }

    @Transactional(readOnly = true)
    public Page<ServiceOfferingResponseDTO> search(ServiceFilter filter, Pageable pageable) {
        boolean includeInactive = filter.includeInactive() && currentUser.find().map(AuthUser::isAdmin).orElse(false);
        return serviceOfferingRepository
                .findAll(ServiceOfferingSpecifications.fromFilter(filter, includeInactive), pageable)
                .map(this::toResponseDTO);
    }

    /** RN05: só o dono ou o ADMIN altera. */
    public ServiceOfferingResponseDTO updateService(Long id, ServiceUpdateDTO dto) {
        ServiceOffering service = findEntityForUpdate(id);
        requireOwnerOrAdmin(service);
        apply(service, dto);
        if (dto.getActive() != null) {
            if (!dto.getActive()) {
                requireNoActiveRequests(service);
            }
            service.setActive(dto.getActive());
        }
        return toResponseDTO(serviceOfferingRepository.saveAndFlush(service));
    }

    /** RN05 e exclusão lógica (o histórico de ordens e avaliações é preservado). */
    public void deleteService(Long id) {
        ServiceOffering service = findEntityForUpdate(id);
        requireOwnerOrAdmin(service);
        requireNoActiveRequests(service);
        service.setActive(false);
    }

    /** RN10: não se tira do ar um serviço com ordem em andamento. */
    private void requireNoActiveRequests(ServiceOffering service) {
        if (requestRepository.existsByService_IdAndStatusIn(service.getId(), RequestStatus.ACTIVE)) {
            throw new BusinessRuleException("O serviço tem ordens em andamento (PENDING, ACCEPTED ou IN_PROGRESS). "
                    + "Conclua ou cancele as ordens antes de excluí-lo.");
        }
    }

    private void apply(ServiceOffering service, ServiceOfferingDTO dto) {
        service.setTitle(dto.getTitle().trim());
        service.setDescription(TextSanitizer.clean(dto.getDescription()));
        service.setPrice(dto.getPrice());
        service.setCategory(dto.getCategory());
    }

    private boolean canManage(ServiceOffering service) {
        return currentUser.find()
                .map(u -> u.isAdmin() || u.id().equals(service.getProvider().getId()))
                .orElse(false);
    }

    private void requireOwnerOrAdmin(ServiceOffering service) {
        if (!canManage(service)) {
            throw new AccessDeniedException("Apenas o prestador dono ou um ADMIN pode alterar este serviço.");
        }
    }

    private ServiceOffering findEntityForUpdate(Long id) {
        return serviceOfferingRepository.findByIdForUpdate(id).orElseThrow(() -> notFound(id));
    }

    private ServiceOffering findEntityById(Long id) {
        return serviceOfferingRepository.findById(id).orElseThrow(() -> notFound(id));
    }

    private static ResourceNotFoundException notFound(Long id) {
        return new ResourceNotFoundException("Serviço não encontrado: " + id);
    }

    private ServiceOfferingResponseDTO toResponseDTO(ServiceOffering service) {
        User provider = service.getProvider();
        return new ServiceOfferingResponseDTO(
                service.getId(),
                service.getTitle(),
                service.getDescription(),
                service.getPrice(),
                service.getCategory(),
                service.isActive(),
                service.getCreatedAt(),
                service.getUpdatedAt(),
                provider.getId(),
                provider.getFullName()
        );
    }
}
