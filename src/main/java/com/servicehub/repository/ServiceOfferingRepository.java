package com.servicehub.repository;

import com.servicehub.model.ServiceOffering;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ServiceOfferingRepository extends JpaRepository<ServiceOffering, Long>, JpaSpecificationExecutor<ServiceOffering> {

    /** Carrega o prestador na mesma consulta (evita N+1 ao montar providerName na listagem). */
    @Override
    @EntityGraph(attributePaths = "provider")
    Page<ServiceOffering> findAll(Specification<ServiceOffering> spec, Pageable pageable);

    /** Cascata lógica: desativar um usuário tira todos os serviços dele do catálogo (RN11). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE ServiceOffering s SET s.active = false WHERE s.provider.id = :providerId")
    int deactivateAllByProvider(@Param("providerId") Long providerId);
}
