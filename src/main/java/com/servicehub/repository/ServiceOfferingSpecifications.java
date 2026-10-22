package com.servicehub.repository;

import com.servicehub.model.ServiceCategory;
import com.servicehub.model.ServiceOffering;
import com.servicehub.model.dto.ServiceFilter;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Critérios combináveis da busca de serviços (consultas parametrizadas, sem concatenação de SQL). */
public final class ServiceOfferingSpecifications {

    private ServiceOfferingSpecifications() {
    }

    public static Specification<ServiceOffering> isActive() {
        return (root, query, cb) -> cb.isTrue(root.get("active"));
    }

    public static Specification<ServiceOffering> hasCategory(ServiceCategory category) {
        return (root, query, cb) -> cb.equal(root.get("category"), category);
    }

    public static Specification<ServiceOffering> hasProvider(Long providerId) {
        return (root, query, cb) -> cb.equal(root.get("provider").get("id"), providerId);
    }

    public static Specification<ServiceOffering> priceGte(BigDecimal min) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("price"), min);
    }

    public static Specification<ServiceOffering> priceLte(BigDecimal max) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("price"), max);
    }

    /** Busca parcial no título. %, _ e ! digitados pelo usuário são escapados: valem como texto, não como curinga. */
    public static Specification<ServiceOffering> titleContains(String q) {
        String literal = q.toLowerCase(Locale.ROOT)
                .replace("!", "!!").replace("%", "!%").replace("_", "!_");
        String pattern = "%" + literal + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("title")), pattern, '!');
    }

    public static Specification<ServiceOffering> fromFilter(ServiceFilter f, boolean includeInactive) {
        List<Specification<ServiceOffering>> specs = new ArrayList<>();
        if (!includeInactive) specs.add(isActive());
        if (f.category() != null) specs.add(hasCategory(f.category()));
        if (f.providerId() != null) specs.add(hasProvider(f.providerId()));
        if (f.minPrice() != null) specs.add(priceGte(f.minPrice()));
        if (f.maxPrice() != null) specs.add(priceLte(f.maxPrice()));
        if (f.q() != null && !f.q().isBlank()) specs.add(titleContains(f.q().trim()));
        return specs.isEmpty() ? Specification.unrestricted() : Specification.allOf(specs);
    }
}
