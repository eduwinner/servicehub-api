package com.servicehub.exception;

import java.net.URI;

/** Identificadores estáveis dos tipos de erro da API (campo "type" do ProblemDetail, RFC 9457). */
public final class ProblemTypes {

    public static final URI NOT_FOUND = URI.create("urn:servicehub:problem:not-found");
    public static final URI EMAIL_ALREADY_EXISTS = URI.create("urn:servicehub:problem:email-already-exists");
    public static final URI BUSINESS_RULE = URI.create("urn:servicehub:problem:business-rule");
    public static final URI VALIDATION = URI.create("urn:servicehub:problem:validation-error");
    public static final URI UNAUTHORIZED = URI.create("urn:servicehub:problem:unauthorized");
    public static final URI FORBIDDEN = URI.create("urn:servicehub:problem:forbidden");
    public static final URI METHOD_NOT_ALLOWED = URI.create("urn:servicehub:problem:method-not-allowed");
    public static final URI UNSUPPORTED_MEDIA_TYPE = URI.create("urn:servicehub:problem:unsupported-media-type");
    public static final URI INTERNAL = URI.create("urn:servicehub:problem:internal-error");

    private ProblemTypes() {
    }
}
