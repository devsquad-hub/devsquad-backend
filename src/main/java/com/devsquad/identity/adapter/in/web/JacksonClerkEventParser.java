package com.devsquad.identity.adapter.in.web;

import com.devsquad.identity.application.ClerkEvent;
import com.devsquad.identity.application.ClerkUser;
import com.devsquad.identity.application.port.ClerkEventParser;
import com.devsquad.shared.domain.DomainException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;

@ApplicationScoped
final class JacksonClerkEventParser implements ClerkEventParser {

  private final ObjectMapper mapper;

  JacksonClerkEventParser(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public ClerkEvent parse(String payload) {
    var event = read(payload);
    var type = requiredText(event, "type");
    var data = event.path("data");
    return switch (type) {
      case "user.created", "user.updated" -> new ClerkEvent(type, toUser(data), null);
      case "user.deleted" -> new ClerkEvent(type, null, requiredText(data, "id"));
      default -> new ClerkEvent(type, null, null);
    };
  }

  private JsonNode read(String payload) {
    try {
      return mapper.readTree(payload);
    } catch (Exception exception) {
      throw new DomainException("invalid_clerk_payload", "Clerk webhook payload is invalid");
    }
  }

  private static ClerkUser toUser(JsonNode data) {
    var emails = new ArrayList<ClerkUser.Email>();
    for (var email : data.path("email_addresses")) {
      emails.add(
          new ClerkUser.Email(requiredText(email, "id"), requiredText(email, "email_address")));
    }
    return new ClerkUser(
        requiredText(data, "id"), nullableText(data, "primary_email_address_id"),
        nullableText(data, "first_name"), nullableText(data, "last_name"),
        nullableText(data, "image_url"), emails);
  }

  private static String requiredText(JsonNode node, String field) {
    var value = nullableText(node, field);
    if (value == null) {
      throw new DomainException("invalid_clerk_payload", "Missing Clerk field: " + field);
    }
    return value;
  }

  private static String nullableText(JsonNode node, String field) {
    var value = node.path(field);
    return value.isMissingNode() || value.isNull() || value.asText().isBlank()
        ? null
        : value.asText();
  }
}
