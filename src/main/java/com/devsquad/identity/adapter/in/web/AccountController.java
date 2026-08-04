package com.devsquad.identity.adapter.in.web;

import static com.devsquad.shared.security.JwtSubject.subject;

import com.devsquad.identity.application.AccountService;
import com.devsquad.identity.domain.Account;
import com.devsquad.identity.domain.AccountProfile;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.hibernate.validator.constraints.URL;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
public class AccountController {

    private final AccountService service;

    public AccountController(AccountService service) {
        this.service = service;
    }

    @GetMapping
    public Account get(@AuthenticationPrincipal Jwt jwt) {
        return service.current(subject(jwt));
    }

    @PatchMapping
    public Account update(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UpdateProfileRequest request) {
        return service.update(subject(jwt), request.toProfile());
    }

    public record UpdateProfileRequest(
            @NotBlank @Size(max = 160) String displayName,
            @Size(max = 4_000) String bio,
            @Size(max = 30) List<@NotBlank @Size(max = 80) String> skills,
            @URL String githubUrl,
            @URL String linkedinUrl,
            @URL String portfolioUrl,
            @Max(168) Integer availabilityHours) {
        AccountProfile toProfile() {
            return AccountProfile.create(displayName, bio, skills, githubUrl, linkedinUrl, portfolioUrl, availabilityHours);
        }
    }
}
