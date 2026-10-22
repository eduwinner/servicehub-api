package com.servicehub.exception;

import java.net.URI;

/** Identificadores estáveis dos tipos de erro da API (campo "type" do ProblemDetail, RFC 9457). */
public final class ProblemTypes {

    public static final URI NOT_FOUND = URI.create("urn:servicehub:problem:not-found");
    public static final URI EMAIL_ALREADY_EXISTS = URI.create("urn:servicehub:problem:email-already-exists");
    public static final URI BUSINESS_RULE = URI.create("urn:servicehub:problem:business-rule");
    public static final URI VALIDATION = URI.create("urn:servicehub:problem:validation-error");
    public static final URI INTERNAL = URI.create("urn:servicehub:problem:internal-error");

    private ProblemTypes() {
    }
}
