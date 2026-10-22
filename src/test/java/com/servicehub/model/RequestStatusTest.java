package com.servicehub.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/** RN08: matriz completa da máquina de estados (5 x 5 = 25 combinações). */
class RequestStatusTest {

    @ParameterizedTest(name = "{0} -> {1}: transição válida={2}, prestador={3}, cliente={4}")
    @DisplayName("RN08: transições válidas e quem pode executá-las")
    @CsvSource({
            "PENDING,     PENDING,     false, false, false",
            "PENDING,     ACCEPTED,    true,  true,  false",
            "PENDING,     IN_PROGRESS, false, false, false",
            "PENDING,     COMPLETED,   false, false, false",
            "PENDING,     CANCELLED,   true,  true,  true",
            "ACCEPTED,    PENDING,     false, false, false",
            "ACCEPTED,    ACCEPTED,    false, false, false",
            "ACCEPTED,    IN_PROGRESS, true,  true,  false",
            "ACCEPTED,    COMPLETED,   false, false, false",
            "ACCEPTED,    CANCELLED,   true,  true,  true",
            "IN_PROGRESS, PENDING,     false, false, false",
            "IN_PROGRESS, ACCEPTED,    false, false, false",
            "IN_PROGRESS, IN_PROGRESS, false, false, false",
            "IN_PROGRESS, COMPLETED,   true,  true,  false",
            "IN_PROGRESS, CANCELLED,   false, false, false",
            "COMPLETED,   PENDING,     false, false, false",
            "COMPLETED,   ACCEPTED,    false, false, false",
            "COMPLETED,   IN_PROGRESS, false, false, false",
            "COMPLETED,   COMPLETED,   false, false, false",
            "COMPLETED,   CANCELLED,   false, false, false",
            "CANCELLED,   PENDING,     false, false, false",
            "CANCELLED,   ACCEPTED,    false, false, false",
            "CANCELLED,   IN_PROGRESS, false, false, false",
            "CANCELLED,   COMPLETED,   false, false, false",
            "CANCELLED,   CANCELLED,   false, false, false"
    })
    void deveRespeitarMatrizDeTransicoes(RequestStatus from, RequestStatus to, boolean valid,
                                         boolean provider, boolean client) {
        assertThat(from.canTransitionTo(to)).isEqualTo(valid);
        assertThat(from.allowedFor(to, RequestStatus.Actor.PROVIDER)).isEqualTo(provider);
        assertThat(from.allowedFor(to, RequestStatus.Actor.CLIENT)).isEqualTo(client);
        assertThat(from.allowedFor(to, RequestStatus.Actor.ADMIN)).isEqualTo(valid);
    }

    @ParameterizedTest
    @DisplayName("ordens ativas são as que ainda não terminaram (RN10)")
    @CsvSource({"PENDING, true", "ACCEPTED, true", "IN_PROGRESS, true", "COMPLETED, false", "CANCELLED, false"})
    void deveIdentificarOrdensAtivas(RequestStatus status, boolean active) {
        assertThat(status.isActive()).isEqualTo(active);
        assertThat(RequestStatus.ACTIVE.contains(status)).isEqualTo(active);
    }
}
