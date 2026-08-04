package com.devsquad.identity.application;

public record ClerkEvent(String type, ClerkUser user, String deletedUserId) {}
