package com.spring.boilerplate.api.handler;

import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.stream.Collectors;

/**
 * Tratador global de exceções — retorna ProblemDetail (RFC 7807) com traceId.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final Tracer tracer;

    public GlobalExceptionHandler(Tracer tracer) {
        this.tracer = tracer;
    }

    // --- Erros de validação (400) ---
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, errors);
        problem.setTitle("Validation Failed");
        problem.setType(URI.create("about:blank"));
        injectTraceId(problem);

        log.warn("Validation error: {}", errors);
        return problem;
    }

    // --- Captura genérica (500) ---
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        problem.setTitle("Internal Server Error");
        problem.setType(URI.create("about:blank"));
        injectTraceId(problem);

        log.error("Unhandled exception", ex);
        return problem;
    }

    /**
     * Injeta o traceId atual nas propriedades do ProblemDetail
     * para que clientes possam referenciá-lo em chamadas de suporte.
     */
    private void injectTraceId(ProblemDetail problem) {
        var span = tracer.currentSpan();
        if (span != null) {
            String traceId = span.context().traceId();
            problem.setProperty("traceId", traceId);
        }
    }
}
