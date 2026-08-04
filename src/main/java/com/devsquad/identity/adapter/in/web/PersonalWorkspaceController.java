package com.devsquad.identity.adapter.in.web;

import static com.devsquad.shared.security.JwtSubject.subject;

import com.devsquad.identity.application.PersonalWorkspaceService;
import com.devsquad.identity.application.port.PersonalWorkspaceStore.ApplicationSummary;
import com.devsquad.identity.application.port.PersonalWorkspaceStore.InvitationSummary;
import com.devsquad.project.application.port.ProjectStore.ProjectView;
import com.devsquad.shared.web.PageResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
public class PersonalWorkspaceController {
    private final PersonalWorkspaceService service;

    public PersonalWorkspaceController(PersonalWorkspaceService service) {
        this.service = service;
    }

    @GetMapping("/projects")
    public PageResponse<ProjectView> projects(@AuthenticationPrincipal Jwt jwt) {
        return PageResponse.singlePage(service.projects(subject(jwt)));
    }

    @GetMapping("/applications")
    public PageResponse<ApplicationSummary> applications(@AuthenticationPrincipal Jwt jwt) {
        return PageResponse.singlePage(service.applications(subject(jwt)));
    }

    @GetMapping("/invitations")
    public PageResponse<InvitationSummary> invitations(@AuthenticationPrincipal Jwt jwt) {
        return PageResponse.singlePage(service.invitations(subject(jwt)));
    }
}
