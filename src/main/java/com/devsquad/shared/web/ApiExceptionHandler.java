package com.devsquad.shared.web;

import com.devsquad.shared.domain.DomainException;
import com.devsquad.shared.persistence.JdbcClient.SqlException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.LinkedHashMap;
import java.util.Map;

@Provider
public class ApiExceptionHandler implements ExceptionMapper<Throwable> {

  private static final MediaType PROBLEM_JSON = MediaType.valueOf("application/problem+json");

  @Override
  public Response toResponse(Throwable exception) {
    if (exception instanceof WebApplicationException webException) {
      return webException.getResponse();
    }
    if (exception instanceof DomainException domain) return domain(domain);
    if (exception instanceof SqlException sql
        && sql.sqlState() != null
        && sql.sqlState().startsWith("23")) {
      return problem(409, "data_conflict", "The operation conflicts with existing data", Map.of());
    }
    return problem(500, "internal_error", "An unexpected error occurred", Map.of());
  }

  private static Response domain(DomainException exception) {
    var code = exception.code();
    var status =
        switch (code) {
          case "authentication_required" -> 401;
          case "account_not_synchronized" -> 409;
          case "invalid_webhook_signature", "clerk_webhook_not_configured" -> 401;
          default -> {
            if (code.endsWith("_not_found")) yield 404;
            if (code.endsWith("_forbidden") || code.endsWith("_required")) yield 403;
            if (code.contains("already_")
                || code.contains("stale_")
                || code.endsWith("_not_pending")
                || code.equals("position_full")) yield 409;
            yield 422;
          }
        };
    return problem(status, code, exception.getMessage(), Map.of());
  }

  static Response problem(
      int status, String code, String detail, Map<String, ?> properties) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("type", "https://devsquad.app/problems/" + code.replace('_', '-'));
    body.put("title", code);
    body.put("status", status);
    body.put("detail", detail);
    body.put("code", code);
    body.putAll(properties);
    return Response.status(status).type(PROBLEM_JSON).entity(body).build();
  }
}
