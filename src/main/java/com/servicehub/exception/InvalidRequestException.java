package com.servicehub.exception;

/** Requisição sintaticamente válida, mas com parâmetro não aceito (ex.: ordenação por campo não permitido). Responde 400. */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
