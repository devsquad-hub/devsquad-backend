package com.devsquad.shared.web;

import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.Map;

@Provider
public class ConstraintViolationExceptionHandler
    implements ExceptionMapper<ConstraintViolationException> {

  @Override
  public Response toResponse(ConstraintViolationException exception) {
    var errors =
        exception.getConstraintViolations().stream()
            .map(
                violation ->
                    Map.of(
                        "field", lastSegment(violation.getPropertyPath().toString()),
                        "message", violation.getMessage()))
            .toList();
    return ApiExceptionHandler.problem(
        400, "invalid_request", "Request validation failed", Map.of("errors", errors));
  }

  private static String lastSegment(String path) {
    var separator = path.lastIndexOf('.');
    return separator < 0 ? path : path.substring(separator + 1);
  }
}
