package com.devsquad.identity.application.port;

@FunctionalInterface
public interface ClerkWebhookReceiptStore {
    boolean register(String messageId, String eventType);
}
