package com.devsquad.hub.application;

import com.devsquad.hub.application.port.HubCatalog;
import com.devsquad.hub.domain.Hub;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.List;

@ApplicationScoped
public class HubQueryService {

  private final HubCatalog catalog;

  public HubQueryService(HubCatalog catalog) {
    this.catalog = catalog;
  }

  @Transactional
  public List<Hub> findAll() {
    return catalog.findAll();
  }
}
