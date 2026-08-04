package com.devsquad.shared.web;

import com.devsquad.shared.domain.DomainException;
import java.net.URI;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(DomainException.class)
    ProblemDetail domain(DomainException exception) {
        var status = switch (exception.code()) {
            case "authentication_required" -> HttpStatus.UNAUTHORIZED;
            case "account_not_synchronized" -> HttpStatus.CONFLICT;
            case "invalid_webhook_signature", "clerk_webhook_not_configured" -> HttpStatus.UNAUTHORIZED;
            default -> {
                if (exception.code().endsWith("_not_found")) yield HttpStatus.NOT_FOUND;
                if (exception.code().endsWith("_forbidden") || exception.code().endsWith("_required")) yield HttpStatus.FORBIDDEN;
                if (exception.code().contains("already_") || exception.code().contains("stale_")
                        || exception.code().endsWith("_not_pending") || exception.code().equals("position_full")) {
                    yield HttpStatus.CONFLICT;
                }
                yield HttpStatus.UNPROCESSABLE_CONTENT;
            }
        };
        var problem = ProblemDetail.forStatusAndDetail(status, exception.getMessage());
        problem.setType(URI.create("https://devsquad.app/problems/" + exception.code()));
        problem.setTitle(exception.code());
        problem.setProperty("code", exception.code());
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validation(MethodArgumentNotValidException exception) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed");
        problem.setType(URI.create("https://devsquad.app/problems/invalid-request"));
        problem.setTitle("invalid_request");
        problem.setProperty("code", "invalid_request");
        problem.setProperty("errors", exception.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.entry(error.getField(), error.getDefaultMessage() == null ? "invalid" : error.getDefaultMessage()))
                .toList());
        return problem;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail conflict(DataIntegrityViolationException exception) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "The operation conflicts with existing data");
        problem.setType(URI.create("https://devsquad.app/problems/data-conflict"));
        problem.setTitle("data_conflict");
        problem.setProperty("code", "data_conflict");
        return problem;
    }
}
