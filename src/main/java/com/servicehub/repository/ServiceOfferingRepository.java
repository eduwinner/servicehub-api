package com.servicehub.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
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

import java.util.List;
import java.util.Optional;

public interface ServiceOfferingRepository extends JpaRepository<ServiceOffering, Long>, JpaSpecificationExecutor<ServiceOffering> {

    /** Carrega o prestador na mesma consulta (evita N+1 ao montar providerName na listagem). */
    @Override
    @EntityGraph(attributePaths = "provider")
    Page<ServiceOffering> findAll(Specification<ServiceOffering> spec, Pageable pageable);

    /** Trava o serviço: contratar e excluir o mesmo serviço ao mesmo tempo viram fila (RN10). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM ServiceOffering s WHERE s.id = :id")
    Optional<ServiceOffering> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM ServiceOffering s WHERE s.provider.id = :providerId")
    List<ServiceOffering> lockAllByProvider(@Param("providerId") Long providerId);

    /** Cascata lógica: desativar um usuário tira todos os serviços dele do catálogo (RN11). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE ServiceOffering s SET s.active = false, s.updatedAt = CURRENT_TIMESTAMP WHERE s.provider.id = :providerId")
    int deactivateAllByProvider(@Param("providerId") Long providerId);
}
