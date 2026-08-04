package com.devsquad.identity.application;

import com.devsquad.identity.application.port.AccountStore;
import com.devsquad.identity.domain.Account;
import com.devsquad.identity.domain.AccountProfile;
import com.devsquad.shared.domain.DomainException;
import org.springframework.stereotype.Service;

@Service
public class AccountService {

    private final AccountStore accounts;

    public AccountService(AccountStore accounts) {
        this.accounts = accounts;
    }

    public Account current(String clerkUserId) {
        return accounts.findByClerkUserId(requireSubject(clerkUserId))
                .orElseThrow(() -> new DomainException("account_not_synchronized", "Account has not been synchronized yet"));
    }

    public Account update(String clerkUserId, AccountProfile profile) {
        current(clerkUserId);
        return accounts.updateProfile(clerkUserId, profile);
    }

    private static String requireSubject(String subject) {
        if (subject == null || subject.isBlank()) {
            throw new DomainException("authentication_required", "Authentication is required");
        }
        return subject;
    }
}
