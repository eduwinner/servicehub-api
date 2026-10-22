package com.servicehub.model.dto;

import com.servicehub.model.ServiceCategory;

import java.math.BigDecimal;

/** Filtros opcionais de GET /api/v1/services. Campos nulos são ignorados. */
public record ServiceFilter(ServiceCategory category, Long providerId, BigDecimal minPrice, BigDecimal maxPrice,
                            String q) {
}
