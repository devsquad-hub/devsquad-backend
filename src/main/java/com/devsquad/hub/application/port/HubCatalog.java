package com.devsquad.hub.application.port;

import com.devsquad.hub.domain.Hub;
import java.util.List;
import java.util.Optional;

public interface HubCatalog {
  List<Hub> findAll();

  Optional<Hub> findBySlug(String slug);
}
