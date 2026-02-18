package com.spring.devsquad.domain.project;

import com.spring.devsquad.domain.user.UserProfile;

import java.util.UUID;

public interface ProjectSummary {
    UUID getId();
    String getTitle();
    String getDescription();
//    List<String> getStack();
    String getTechStack();
    ProjectStatus getStatus();
    UserProfile getUserAuthor();
//    int getTotalPositions();
    int getCandidatesCount();
}
