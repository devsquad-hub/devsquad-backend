package com.devsquad.identity.application.port;

import com.devsquad.identity.application.ClerkEvent;

@FunctionalInterface
public interface ClerkEventParser {
  ClerkEvent parse(String payload);
}
