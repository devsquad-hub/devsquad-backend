package com.devsquad.identity.adapter.in.web;

import static com.devsquad.shared.security.JwtSubject.subject;

import com.devsquad.identity.application.AccountService;
import com.devsquad.identity.domain.Account;
import com.devsquad.identity.domain.AccountProfile;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;
import java.util.List;
import org.hibernate.validator.constraints.URL;

@Path("/api/v1/me")
public class AccountController {

  private final AccountService service;

  public AccountController(AccountService service) {
    this.service = service;
  }

  @GET
  public Account get(@Context SecurityContext securityContext) {
    return service.current(subject(securityContext));
  }

  @PATCH
  public Account update(
      @Context SecurityContext securityContext, @Valid UpdateProfileRequest request) {
    return service.update(subject(securityContext), request.toProfile());
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
      return AccountProfile.create(
          displayName, bio, skills, githubUrl, linkedinUrl, portfolioUrl, availabilityHours);
    }
  }
}
