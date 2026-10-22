package com.servicehub.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import tools.jackson.databind.exc.InvalidFormatException;

import java.net.URI;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Traduz exceções em respostas ProblemDetail (RFC 9457, application/problem+json).
 * Exceções de domínio viram 404/409; qualquer outra vira 500 sem detalhes internos.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, ProblemTypes.NOT_FOUND, "Recurso não encontrado", ex.getMessage(), request);
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ProblemDetail handleEmailExists(EmailAlreadyExistsException ex, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, ProblemTypes.EMAIL_ALREADY_EXISTS, "Conflito", ex.getMessage(), request);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ProblemDetail handleBusinessRule(BusinessRuleException ex, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, ProblemTypes.BUSINESS_RULE, "Regra de negócio violada", ex.getMessage(), request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail pd = problem(HttpStatus.BAD_REQUEST, ProblemTypes.VALIDATION, "Dados inválidos",
                "Um ou mais campos estão inválidos.", request);
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .sorted(Comparator.comparing(FieldError::getField))
                .map(e -> Map.of("field", e.getField(),
                        "message", Objects.requireNonNullElse(e.getDefaultMessage(), "valor inválido")))
                .toList();
        pd.setProperty("errors", errors);
        return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_PROBLEM_JSON).body(pd);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String detail = "Corpo da requisição inválido ou malformado.";
        if (ex.getCause() instanceof InvalidFormatException ife && ife.getTargetType() != null
                && ife.getTargetType().isEnum()) {
            String field = ife.getPath().isEmpty() ? "?" : ife.getPath().getLast().getPropertyName();
            detail = "Valor inválido para '" + field + "'. Valores aceitos: "
                    + Arrays.toString(ife.getTargetType().getEnumConstants());
        }
        ProblemDetail pd = problem(HttpStatus.BAD_REQUEST, ProblemTypes.VALIDATION, "Dados inválidos", detail, request);
        return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_PROBLEM_JSON).body(pd);
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ProblemDetail handleInvalidRequest(InvalidRequestException ex, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, ProblemTypes.VALIDATION, "Dados inválidos", ex.getMessage(), request);
    }

    /** Rede de proteção: propriedade inexistente em sort/consulta derivada é erro do cliente, não 500. */
    @ExceptionHandler(PropertyReferenceException.class)
    public ProblemDetail handlePropertyReference(PropertyReferenceException ex, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, ProblemTypes.VALIDATION, "Dados inválidos",
                "Campo desconhecido: '" + ex.getPropertyName() + "'.", request);
    }

    /** Parâmetro de rota/consulta com tipo errado (ex.: ?category=FOO, ?minPrice=abc). */
    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        String name = ex instanceof MethodArgumentTypeMismatchException m ? m.getName() : ex.getPropertyName();
        Class<?> type = ex.getRequiredType();
        String detail = (type != null && type.isEnum())
                ? "Valor inválido para '" + name + "'. Valores aceitos: " + Arrays.toString(type.getEnumConstants())
                : "Formato inválido para o parâmetro '" + name + "'.";
        ProblemDetail pd = problem(HttpStatus.BAD_REQUEST, ProblemTypes.VALIDATION, "Dados inválidos", detail, request);
        return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_PROBLEM_JSON).body(pd);
    }

    /**
     * Demais erros tratados pelo próprio Spring MVC (404 de rota, 405, 415, 400 genéricos):
     * mesmo contrato (type URN, título em português, timestamp) em vez do padrão "about:blank" em inglês.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
            HttpStatusCode statusCode, WebRequest request) {
        ProblemDetail pd = switch (statusCode.value()) {
            case 400 -> problem(HttpStatus.BAD_REQUEST, ProblemTypes.VALIDATION, "Dados inválidos",
                    "Requisição inválida.", request);
            case 404 -> problem(HttpStatus.NOT_FOUND, ProblemTypes.NOT_FOUND, "Recurso não encontrado",
                    "Rota não encontrada.", request);
            case 405 -> problem(HttpStatus.METHOD_NOT_ALLOWED, ProblemTypes.METHOD_NOT_ALLOWED, "Método não permitido",
                    "O método HTTP usado não é suportado nesta rota.", request);
            case 415 -> problem(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ProblemTypes.UNSUPPORTED_MEDIA_TYPE,
                    "Tipo de conteúdo não suportado", "Envie o corpo como application/json.", request);
            default -> null;
        };
        if (pd == null) {
            return super.handleExceptionInternal(ex, body, headers, statusCode, request);
        }
        HttpHeaders out = new HttpHeaders();
        out.putAll(headers);
        out.setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        return new ResponseEntity<>(pd, out, statusCode);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Erro inesperado", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, ProblemTypes.INTERNAL, "Erro interno",
                "Erro interno. Informe o X-Request-Id ao suporte.", request);
    }

    static ProblemDetail problem(HttpStatus status, URI type, String title, String detail, HttpServletRequest request) {
        return problem(status, type, title, detail, request.getRequestURI());
    }

    private static ProblemDetail problem(HttpStatus status, URI type, String title, String detail, WebRequest request) {
        String uri = request instanceof ServletWebRequest swr ? swr.getRequest().getRequestURI() : "";
        return problem(status, type, title, detail, uri);
    }

    private static ProblemDetail problem(HttpStatus status, URI type, String title, String detail, String uri) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setType(type);
        pd.setTitle(title);
        pd.setInstance(URI.create(uri));
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }
}
