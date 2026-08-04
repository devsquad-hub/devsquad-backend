package com.devsquad.identity.application;

import com.devsquad.identity.application.port.AccountStore;
import com.devsquad.identity.application.port.ClerkEventParser;
import com.devsquad.identity.application.port.ClerkWebhookReceiptStore;
import com.devsquad.identity.application.port.DefaultHubMembership;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class ClerkWebhookService {

  private final ClerkEventParser parser;
  private final ClerkWebhookReceiptStore receipts;
  private final AccountStore accounts;
  private final DefaultHubMembership defaultHubMembership;

  public ClerkWebhookService(
      ClerkEventParser parser,
      ClerkWebhookReceiptStore receipts,
      AccountStore accounts,
      DefaultHubMembership defaultHubMembership) {
    this.parser = parser;
    this.receipts = receipts;
    this.accounts = accounts;
    this.defaultHubMembership = defaultHubMembership;
  }

  @Transactional
  public void process(String messageId, String payload) {
    var event = parser.parse(payload);
    if (!receipts.register(messageId, event.type())) {
      return;
    }

    switch (event.type()) {
      case "user.created", "user.updated" -> synchronize(event.user());
      case "user.deleted" -> accounts.markDeleted(event.deletedUserId());
      default -> {
        /* Unknown events are recorded and intentionally ignored. */
      }
    }
  }

  private void synchronize(ClerkUser user) {
    accounts.synchronize(user);
    defaultHubMembership.ensureFor(user.id());
  }
}
