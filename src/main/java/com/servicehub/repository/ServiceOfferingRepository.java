package com.servicehub.repository;

import com.servicehub.model.ServiceCategory;
import com.servicehub.model.ServiceOffering;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface ServiceOfferingRepository extends JpaRepository<ServiceOffering, Long>, JpaSpecificationExecutor<ServiceOffering> {

    /** Carrega o prestador na mesma consulta (evita N+1 ao montar providerName na listagem). */
    @Override
    @EntityGraph(attributePaths = "provider")
    Page<ServiceOffering> findAll(Specification<ServiceOffering> spec, Pageable pageable);


    List<ServiceOffering> findByCategory(ServiceCategory category);

    List<ServiceOffering> findByProviderId(Long providerId);

    List<ServiceOffering> findByActiveTrue();
}
