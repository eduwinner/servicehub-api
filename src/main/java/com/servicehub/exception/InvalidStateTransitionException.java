package com.servicehub.exception;

import com.servicehub.model.RequestStatus;

/** Transição fora da máquina de estados da ordem (RN08). Responde 409. */
public class InvalidStateTransitionException extends RuntimeException {

    public InvalidStateTransitionException(RequestStatus from, RequestStatus to) {
        super("Não é possível mudar de " + from + " para " + to + ".");
    }
}
