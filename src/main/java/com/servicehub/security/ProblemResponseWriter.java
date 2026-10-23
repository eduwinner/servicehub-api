package com.servicehub.security;

import com.servicehub.exception.GlobalExceptionHandler;
import com.servicehub.exception.ProblemTypes;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Responde os erros gerados nos filtros do Spring Security (antes do controller) com o mesmo
 * ProblemDetail do GlobalExceptionHandler. Rota inexistente responde 404 e método não suportado 405,
 * mesmo sem token, em vez de um 401 enganoso.
 */
@Component
public class ProblemResponseWriter {

    private final ObjectProvider<RequestMappingHandlerMapping> handlerMapping;
    private final ObjectProvider<JsonMapper> jsonMapper;

    public ProblemResponseWriter(
            @Qualifier("requestMappingHandlerMapping") ObjectProvider<RequestMappingHandlerMapping> handlerMapping,
            ObjectProvider<JsonMapper> jsonMapper) {
        this.handlerMapping = handlerMapping;
        this.jsonMapper = jsonMapper;
    }

    public AuthenticationEntryPoint entryPoint() {
        return (request, response, ex) -> {
            HttpStatus routeStatus = routeStatus(request);
            if (routeStatus == HttpStatus.NOT_FOUND) {
                write(request, response, HttpStatus.NOT_FOUND, ProblemTypes.NOT_FOUND, "Recurso não encontrado",
                        "Rota não encontrada.");
            } else if (routeStatus == HttpStatus.METHOD_NOT_ALLOWED) {
                write(request, response, HttpStatus.METHOD_NOT_ALLOWED, ProblemTypes.METHOD_NOT_ALLOWED,
                        "Método não permitido", "O método HTTP usado não é suportado nesta rota.");
            } else {
                response.setHeader("WWW-Authenticate", "Bearer");
                write(request, response, HttpStatus.UNAUTHORIZED, ProblemTypes.UNAUTHORIZED, "Não autenticado",
                        "Envie um token válido no header Authorization: Bearer <token>.");
            }
        };
    }

    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, ex) -> write(request, response, HttpStatus.FORBIDDEN, ProblemTypes.FORBIDDEN,
                "Acesso negado", "Você não tem permissão para esta operação.");
    }

    /** OK quando há controller para a rota e o método; 404/405 quando não há. */
    private HttpStatus routeStatus(HttpServletRequest request) {
        RequestMappingHandlerMapping mapping = handlerMapping.getIfAvailable();
        if (mapping == null || request.getRequestURI().startsWith("/actuator")) {
            return HttpStatus.OK;
        }
        try {
            return mapping.getHandler(request) != null ? HttpStatus.OK : HttpStatus.NOT_FOUND;
        } catch (HttpRequestMethodNotSupportedException e) {
            return HttpStatus.METHOD_NOT_ALLOWED;
        } catch (Exception e) {
            return HttpStatus.OK; // rota existe, mas outra condição falhou: segue como 401
        }
    }

    private void write(HttpServletRequest request, HttpServletResponse response, HttpStatus status, URI type,
                       String title, String detail) throws IOException {
        ProblemDetail pd = GlobalExceptionHandler.problem(status, type, title, detail, request);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", pd.getType().toString());
        body.put("title", pd.getTitle());
        body.put("status", pd.getStatus());
        body.put("detail", pd.getDetail());
        body.put("instance", pd.getInstance() == null ? null : pd.getInstance().toString());
        if (pd.getProperties() != null) {
            pd.getProperties().forEach((k, v) -> body.put(k, v == null ? null : v.toString()));
        }
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        JsonMapper mapper = jsonMapper.getIfAvailable(JsonMapper::new);
        response.getWriter().write(mapper.writeValueAsString(body));
    }
}
