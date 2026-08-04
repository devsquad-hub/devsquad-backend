package com.devsquad.project.domain;

import com.devsquad.shared.domain.DomainException;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

public final class ProjectLifecycle {

    private static final Map<ProjectStatus, EnumSet<ProjectStatus>> TRANSITIONS = transitions();

    private ProjectStatus status;

    public ProjectLifecycle(ProjectStatus status) {
        this.status = status;
    }

    public void transitionTo(ProjectStatus next) {
        if (!TRANSITIONS.get(status).contains(next)) {
            throw new DomainException("invalid_project_transition", "Cannot transition project from " + status + " to " + next);
        }
        status = next;
    }

    public ProjectStatus status() {
        return status;
    }

    private static Map<ProjectStatus, EnumSet<ProjectStatus>> transitions() {
        var transitions = new EnumMap<ProjectStatus, EnumSet<ProjectStatus>>(ProjectStatus.class);
        transitions.put(ProjectStatus.PLANNING, EnumSet.of(ProjectStatus.RECRUITING, ProjectStatus.ARCHIVED));
        transitions.put(ProjectStatus.RECRUITING, EnumSet.of(ProjectStatus.ACTIVE, ProjectStatus.ARCHIVED));
        transitions.put(ProjectStatus.ACTIVE, EnumSet.of(ProjectStatus.COMPLETED, ProjectStatus.ARCHIVED));
        transitions.put(ProjectStatus.COMPLETED, EnumSet.of(ProjectStatus.ARCHIVED));
        transitions.put(ProjectStatus.ARCHIVED, EnumSet.noneOf(ProjectStatus.class));
        return Map.copyOf(transitions);
    }
}
