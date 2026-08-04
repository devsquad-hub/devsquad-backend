package com.devsquad.identity.application.port;

import com.devsquad.identity.application.PublicProfileService.PublicProfile;
import java.util.Optional;
import java.util.UUID;

public interface PublicProfileStore {
  Optional<PublicProfile> find(UUID accountId);
}
