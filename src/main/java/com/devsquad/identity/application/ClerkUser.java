package com.devsquad.identity.application;

import java.util.List;

public record ClerkUser(
    String id,
    String primaryEmailAddressId,
    String firstName,
    String lastName,
    String imageUrl,
    List<Email> emailAddresses) {

  public record Email(String id, String emailAddress) {}

  public String primaryEmail() {
    if (emailAddresses == null) {
      return null;
    }
    return emailAddresses.stream()
        .filter(email -> email.id().equals(primaryEmailAddressId))
        .map(Email::emailAddress)
        .findFirst()
        .orElseGet(() -> emailAddresses.stream().findFirst().map(Email::emailAddress).orElse(null));
  }

  public String displayName() {
    var fullName = String.join(" ", nullToEmpty(firstName), nullToEmpty(lastName)).trim();
    if (!fullName.isBlank()) {
      return fullName;
    }
    var email = primaryEmail();
    return email == null ? "DevSquad member" : email.substring(0, email.indexOf('@'));
  }

  private static String nullToEmpty(String value) {
    return value == null ? "" : value;
  }
}
