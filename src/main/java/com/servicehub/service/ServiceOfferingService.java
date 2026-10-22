package com.servicehub.service;

import com.servicehub.exception.ResourceNotFoundException;
import com.servicehub.model.ServiceOffering;
import com.servicehub.model.User;
import com.servicehub.model.dto.ServiceOfferingDTO;
import com.servicehub.model.dto.ServiceOfferingResponseDTO;
import com.servicehub.repository.ServiceOfferingRepository;
import com.servicehub.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ServiceOfferingService {

    private final ServiceOfferingRepository serviceOfferingRepository;
    private final UserRepository userRepository;

    public ServiceOfferingService(ServiceOfferingRepository serviceOfferingRepository, UserRepository userRepository) {
        this.serviceOfferingRepository = serviceOfferingRepository;
        this.userRepository = userRepository;
    }

    public ServiceOfferingResponseDTO createService(ServiceOfferingDTO dto) {
        User provider = userRepository.findById(dto.getProviderId())
                .orElseThrow(() -> new ResourceNotFoundException("Prestador não encontrado: " + dto.getProviderId()));

        ServiceOffering service = new ServiceOffering();
        service.setTitle(dto.getTitle());
        service.setDescription(dto.getDescription());
        service.setPrice(dto.getPrice());
        service.setCategory(dto.getCategory());
        service.setProvider(provider);

        return toResponseDTO(serviceOfferingRepository.save(service));
    }

    @Transactional(readOnly = true)
    public ServiceOfferingResponseDTO findById(Long id) {
        return toResponseDTO(findEntityById(id));
    }

    @Transactional(readOnly = true)
    public List<ServiceOfferingResponseDTO> findAll() {
        return serviceOfferingRepository.findAll().stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public ServiceOfferingResponseDTO updateService(Long id, ServiceOfferingDTO dto) {
        ServiceOffering service = findEntityById(id);
        service.setTitle(dto.getTitle());
        service.setDescription(dto.getDescription());
        service.setPrice(dto.getPrice());
        service.setCategory(dto.getCategory());
        return toResponseDTO(serviceOfferingRepository.save(service));
    }

    public void deleteService(Long id) {
        ServiceOffering service = findEntityById(id);
        serviceOfferingRepository.delete(service);
    }

    private ServiceOffering findEntityById(Long id) {
        return serviceOfferingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado: " + id));
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
                provider.getId(),
                provider.getFullName()
        );
    }
}
