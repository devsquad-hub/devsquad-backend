package com.devsquad.identity.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.devsquad.identity.application.port.AccountStore;
import com.devsquad.identity.application.port.ClerkEventParser;
import com.devsquad.identity.application.port.ClerkWebhookReceiptStore;
import com.devsquad.identity.application.port.DefaultHubMembership;
import com.devsquad.identity.domain.Account;
import com.devsquad.identity.domain.AccountProfile;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ClerkWebhookServiceTest {

  @Test
  void synchronizesUserAndEnsuresDefaultHubMembership() {
    var user = user();
    var accounts = new RecordingAccountStore();
    var memberships = new RecordingMembership();
    ClerkEventParser parser = ignored -> new ClerkEvent("user.created", user, null);
    ClerkWebhookReceiptStore receipts = (messageId, eventType) -> true;
    var service = new ClerkWebhookService(parser, receipts, accounts, memberships);

    service.process("message-1", "payload");

    assertThat(accounts.syncedUser).isEqualTo(user);
    assertThat(memberships.clerkUserId).isEqualTo(user.id());
  }

  @Test
  void ignoresAnEventWhoseReceiptAlreadyExists() {
    var accounts = new RecordingAccountStore();
    var memberships = new RecordingMembership();
    ClerkEventParser parser = ignored -> new ClerkEvent("user.updated", user(), null);
    ClerkWebhookReceiptStore receipts = (messageId, eventType) -> false;
    var service = new ClerkWebhookService(parser, receipts, accounts, memberships);

    service.process("message-1", "payload");

    assertThat(accounts.syncedUser).isNull();
    assertThat(memberships.clerkUserId).isNull();
  }

  private static ClerkUser user() {
    return new ClerkUser(
        "user_123",
        "email_1",
        "Ada",
        "Lovelace",
        null,
        List.of(new ClerkUser.Email("email_1", "ada@example.com")));
  }

  private static final class RecordingMembership implements DefaultHubMembership {
    private String clerkUserId;

    @Override
    public void ensureFor(String clerkUserId) {
      this.clerkUserId = clerkUserId;
    }
  }

  private static final class RecordingAccountStore implements AccountStore {
    private ClerkUser syncedUser;

    @Override
    public Optional<Account> findByClerkUserId(String clerkUserId) {
      return Optional.empty();
    }

    @Override
    public Account updateProfile(String clerkUserId, AccountProfile profile) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void synchronize(ClerkUser user) {
      syncedUser = user;
    }

    @Override
    public void markDeleted(String clerkUserId) {}
  }
}
