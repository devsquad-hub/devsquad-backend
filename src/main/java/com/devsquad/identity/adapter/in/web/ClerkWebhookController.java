package com.devsquad.identity.adapter.in.web;

import com.devsquad.identity.application.ClerkWebhookService;
import com.devsquad.shared.domain.DomainException;
import com.svix.Webhook;
import com.svix.exceptions.EmptyWebhookSecretException;
import com.svix.exceptions.WebhookVerificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/webhooks/clerk")
public class ClerkWebhookController {

    private final ClerkWebhookService service;
    private final String secret;

    public ClerkWebhookController(ClerkWebhookService service, @Value("${app.clerk.webhook-secret:}") String secret) {
        this.service = service;
        this.secret = secret;
    }

    @PostMapping
    public ResponseEntity<Void> receive(@RequestHeader HttpHeaders headers, @RequestBody String payload) {
        if (secret.isBlank()) {
            throw new DomainException("clerk_webhook_not_configured", "Clerk webhook is not configured");
        }
        try {
            Map<String, List<String>> verificationHeaders = new HashMap<>();
            headers.forEach(verificationHeaders::put);
            new Webhook(secret).verify(payload, verificationHeaders);
        } catch (WebhookVerificationException | EmptyWebhookSecretException exception) {
            throw new DomainException("invalid_webhook_signature", "Webhook signature is invalid");
        }
        var messageId = headers.getFirst("svix-id");
        if (messageId == null || messageId.isBlank()) {
            throw new DomainException("invalid_webhook_signature", "Webhook message id is missing");
        }
        service.process(messageId, payload);
        return ResponseEntity.noContent().build();
    }
}
