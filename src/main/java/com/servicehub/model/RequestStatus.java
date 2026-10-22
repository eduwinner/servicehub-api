package com.servicehub.model;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Ciclo de vida da ordem de serviço (RN08, ADR-0006). A tabela de transições é o coração
 * da regra de negócio: o CHECK do banco só garante que o valor existe.
 *
 * <pre>
 * PENDING ──aceita──▶ ACCEPTED ──inicia──▶ IN_PROGRESS ──conclui──▶ COMPLETED
 *    │                   │
 *    └──cancela──▶ CANCELLED ◀──cancela──┘
 * </pre>
 */
public enum RequestStatus {
    PENDING, ACCEPTED, IN_PROGRESS, COMPLETED, CANCELLED;

    /** Ordens que ainda não terminaram: bloqueiam exclusão de serviço e usuário (RN10). */
    public static final Set<RequestStatus> ACTIVE = Set.copyOf(EnumSet.of(PENDING, ACCEPTED, IN_PROGRESS));

    /** Quem está pedindo a mudança, em relação à ordem. */
    public enum Actor { CLIENT, PROVIDER, ADMIN }

    private record Rule(RequestStatus from, RequestStatus to) {
    }

    private static final Map<Rule, Set<Actor>> TRANSITIONS = Map.of(
            new Rule(PENDING, ACCEPTED), EnumSet.of(Actor.PROVIDER),
            new Rule(PENDING, CANCELLED), EnumSet.of(Actor.CLIENT, Actor.PROVIDER),
            new Rule(ACCEPTED, IN_PROGRESS), EnumSet.of(Actor.PROVIDER),
            new Rule(ACCEPTED, CANCELLED), EnumSet.of(Actor.CLIENT, Actor.PROVIDER),
            new Rule(IN_PROGRESS, COMPLETED), EnumSet.of(Actor.PROVIDER));

    public boolean canTransitionTo(RequestStatus target) {
        return TRANSITIONS.containsKey(new Rule(this, target));
    }

    /** ADMIN pode qualquer transição válida; os demais, só as da tabela. */
    public boolean allowedFor(RequestStatus target, Actor actor) {
        Set<Actor> actors = TRANSITIONS.get(new Rule(this, target));
        return actors != null && (actor == Actor.ADMIN || actors.contains(actor));
    }

    public boolean isActive() {
        return ACTIVE.contains(this);
    }
}
