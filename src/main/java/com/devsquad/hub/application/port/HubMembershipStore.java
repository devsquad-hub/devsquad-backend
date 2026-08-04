package com.devsquad.hub.application.port;

import com.devsquad.hub.domain.HubRole;
import java.util.List;
import java.util.UUID;

public interface HubMembershipStore {
  List<MembershipView> findByAccount(UUID accountId);

  List<MemberView> findMembers(UUID hubId);

  void assign(UUID hubId, UUID accountId, HubRole role);

  boolean isActiveAccount(UUID accountId);

  record MembershipView(UUID hubId, String hubName, String hubSlug, HubRole role) {}

  record MemberView(
      UUID accountId, String displayName, String email, String avatarUrl, HubRole role) {}
}
