package com.servicehub.repository;

import com.servicehub.model.RequestStatus;
import com.servicehub.model.ServiceRequest;
import org.springframework.data.jpa.domain.Specification;

/** Filtros da listagem de ordens: participação do usuário (RN13) e status. */
public final class ServiceRequestSpecifications {

    private ServiceRequestSpecifications() {
    }

    public static Specification<ServiceRequest> asClient(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("client").get("id"), userId);
    }

    public static Specification<ServiceRequest> asProvider(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("service").get("provider").get("id"), userId);
    }

    public static Specification<ServiceRequest> hasStatus(RequestStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }
}
