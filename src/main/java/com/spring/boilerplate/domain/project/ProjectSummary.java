package com.spring.boilerplate.domain.project;

import com.spring.boilerplate.domain.user.UserProfile;

import java.util.List;
import java.util.UUID;

public interface ProjectSummary {
    UUID getId();
    String getTitle();
    String getDescription();
//    List<String> getStack();
    String getTechStack();
    ProjectStatus getStatus();
    UserProfile getUserAuthor();
    int getTotalPositions();
    int getCandidatesCount();
}
