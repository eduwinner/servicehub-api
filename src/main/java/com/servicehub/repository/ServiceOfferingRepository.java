package com.servicehub.repository;

import com.servicehub.model.ServiceCategory;
import com.servicehub.model.ServiceOffering;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface ServiceOfferingRepository extends JpaRepository<ServiceOffering, Long>, JpaSpecificationExecutor<ServiceOffering> {

    List<ServiceOffering> findByCategory(ServiceCategory category);

    List<ServiceOffering> findByProviderId(Long providerId);

    List<ServiceOffering> findByActiveTrue();
}
