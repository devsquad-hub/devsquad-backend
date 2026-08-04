package com.devsquad.hub.domain;

import java.util.UUID;

public record Hub(UUID id, String name, String slug, String description) {}
