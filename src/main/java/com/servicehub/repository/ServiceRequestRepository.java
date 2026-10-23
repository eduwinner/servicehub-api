package com.servicehub.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import com.servicehub.model.RequestStatus;
import com.servicehub.model.ServiceRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /** Trava a linha da ordem até o fim da transação: duas mudanças de status simultâneas viram fila (RN08). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM ServiceRequest r WHERE r.id = :id")
    Optional<ServiceRequest> findByIdForUpdate(@Param("id") Long id);

    boolean existsByService_IdAndStatusIn(Long serviceId, Collection<RequestStatus> statuses);

    boolean existsByClient_IdAndStatusIn(Long clientId, Collection<RequestStatus> statuses);

    boolean existsByService_Provider_IdAndStatusIn(Long providerId, Collection<RequestStatus> statuses);
}
