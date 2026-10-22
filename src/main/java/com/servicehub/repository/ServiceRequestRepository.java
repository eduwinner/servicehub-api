package com.servicehub.repository;

import com.servicehub.model.RequestStatus;
import com.servicehub.model.ServiceRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.Optional;

public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, Long>, JpaSpecificationExecutor<ServiceRequest> {

    /** Serviço, prestador e cliente na mesma consulta (sem N+1 ao montar a resposta). */
    @Override
    @EntityGraph(attributePaths = {"service", "service.provider", "client"})
    Page<ServiceRequest> findAll(Specification<ServiceRequest> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"service", "service.provider", "client"})
    Optional<ServiceRequest> findById(Long id);

    boolean existsByService_IdAndStatusIn(Long serviceId, Collection<RequestStatus> statuses);

    boolean existsByClient_IdAndStatusIn(Long clientId, Collection<RequestStatus> statuses);

    boolean existsByService_Provider_IdAndStatusIn(Long providerId, Collection<RequestStatus> statuses);
}
