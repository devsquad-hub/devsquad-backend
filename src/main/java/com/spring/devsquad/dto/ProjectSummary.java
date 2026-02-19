package com.spring.devsquad.dto;

import com.spring.devsquad.model.UserProfile;
import com.spring.devsquad.model.ProjectStatus;

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
