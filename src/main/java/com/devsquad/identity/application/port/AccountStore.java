package com.devsquad.identity.application.port;

import com.devsquad.identity.application.ClerkUser;
import com.devsquad.identity.domain.Account;
import com.devsquad.identity.domain.AccountProfile;
import java.util.Optional;

public interface AccountStore {
  Optional<Account> findByClerkUserId(String clerkUserId);

  Account updateProfile(String clerkUserId, AccountProfile profile);

  void synchronize(ClerkUser user);

  void markDeleted(String clerkUserId);
}
