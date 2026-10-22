package com.servicehub.security;

import com.servicehub.exception.ProblemTypes;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Responde 401/403 gerados nos filtros do Spring Security (antes do controller) no mesmo
 * formato ProblemDetail do GlobalExceptionHandler.
 */
@Component
public class ProblemResponseWriter {

    public AuthenticationEntryPoint entryPoint() {
        return (request, response, ex) -> write(request, response, HttpStatus.UNAUTHORIZED,
                ProblemTypes.UNAUTHORIZED.toString(), "Não autenticado",
                "Envie um token válido no header Authorization: Bearer <token>.");
    }

    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, ex) -> write(request, response, HttpStatus.FORBIDDEN,
                ProblemTypes.FORBIDDEN.toString(), "Acesso negado",
                "Você não tem permissão para esta operação.");
    }

    private static void write(HttpServletRequest request, HttpServletResponse response, HttpStatus status,
                              String type, String title, String detail) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        if (status == HttpStatus.UNAUTHORIZED) {
            response.setHeader("WWW-Authenticate", "Bearer");
        }
        String json = "{\"type\":\"" + type + "\",\"title\":\"" + title + "\",\"status\":" + status.value()
                + ",\"detail\":\"" + detail + "\",\"instance\":\"" + escape(request.getRequestURI())
                + "\",\"timestamp\":\"" + Instant.now() + "\"}";
        response.getWriter().write(json);
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
