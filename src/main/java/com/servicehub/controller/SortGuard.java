package com.servicehub.controller;

import com.servicehub.exception.InvalidRequestException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;
import java.util.TreeSet;

/**
 * Restringe os campos aceitos em ?sort=. Sem isso, qualquer caminho JPA seria aceito
 * (ex.: sort=passwordHash ou sort=provider.passwordHash), criando joins e um "oráculo"
 * de ordenação sobre colunas internas; e um campo inexistente viraria HTTP 500.
 */
final class SortGuard {

    private SortGuard() {
    }

    static Pageable requireAllowed(Pageable pageable, Set<String> allowed) {
        for (Sort.Order order : pageable.getSort()) {
            if (!allowed.contains(order.getProperty())) {
                throw new InvalidRequestException("Não é possível ordenar por '" + order.getProperty()
                        + "'. Campos aceitos: " + new TreeSet<>(allowed));
            }
        }
        return pageable;
    }
}
