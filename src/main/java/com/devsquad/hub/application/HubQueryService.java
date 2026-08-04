package com.devsquad.hub.application;

import com.devsquad.hub.application.port.HubCatalog;
import com.devsquad.hub.domain.Hub;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HubQueryService {

    private final HubCatalog catalog;

    public HubQueryService(HubCatalog catalog) {
        this.catalog = catalog;
    }

    @Transactional(readOnly = true)
    public List<Hub> findAll() {
        return catalog.findAll();
    }
}
