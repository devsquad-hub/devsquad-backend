package com.devsquad.identity.adapter.in.web;

import com.devsquad.identity.application.ClerkWebhookService;
import com.devsquad.shared.domain.DomainException;
import com.svix.Webhook;
import com.svix.exceptions.EmptyWebhookSecretException;
import com.svix.exceptions.WebhookVerificationException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/api/v1/webhooks/clerk")
public class ClerkWebhookController {

  private final ClerkWebhookService service;
  private final String secret;

  public ClerkWebhookController(
      ClerkWebhookService service,
      @ConfigProperty(name = "app.clerk.webhook-secret") Optional<String> secret) {
    this.service = service;
    this.secret = secret.orElse("");
  }

  @POST
  public Response receive(@Context HttpHeaders headers, String payload) {
    if (secret.isBlank()) {
      throw new DomainException("clerk_webhook_not_configured", "Clerk webhook is not configured");
    }
    try {
      Map<String, List<String>> verificationHeaders = new HashMap<>();
      headers.getRequestHeaders().forEach(verificationHeaders::put);
      new Webhook(secret).verify(payload, verificationHeaders);
    } catch (WebhookVerificationException | EmptyWebhookSecretException exception) {
      throw new DomainException("invalid_webhook_signature", "Webhook signature is invalid");
    }
    var messageId = headers.getHeaderString("svix-id");
    if (messageId == null || messageId.isBlank()) {
      throw new DomainException("invalid_webhook_signature", "Webhook message id is missing");
    }
    service.process(messageId, payload);
    return Response.noContent().build();
  }
}
