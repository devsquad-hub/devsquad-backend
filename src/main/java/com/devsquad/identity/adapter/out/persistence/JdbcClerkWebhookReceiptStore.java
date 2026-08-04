package com.devsquad.identity.adapter.out.persistence;

import com.devsquad.identity.application.port.ClerkWebhookReceiptStore;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcClerkWebhookReceiptStore implements ClerkWebhookReceiptStore {

    private final JdbcClient jdbc;

    public JdbcClerkWebhookReceiptStore(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean register(String messageId, String eventType) {
        return jdbc.sql("""
                        insert into clerk_webhook_receipts (message_id, event_type)
                        values (:messageId, :eventType) on conflict do nothing
                        """)
                .param("messageId", messageId)
                .param("eventType", eventType)
                .update() == 1;
    }
}
