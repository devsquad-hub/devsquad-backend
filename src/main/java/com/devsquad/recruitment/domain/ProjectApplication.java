package com.devsquad.recruitment.domain;

import com.devsquad.shared.domain.DomainException;
import java.util.UUID;

public final class ProjectApplication {

  private final UUID id;
  private final UUID positionId;
  private final UUID applicantId;
  private ApplicationStatus status;
  private UUID reviewerId;
  private String decisionNote;

  private ProjectApplication(UUID id, UUID positionId, UUID applicantId) {
    this.id = id;
    this.positionId = positionId;
    this.applicantId = applicantId;
    this.status = ApplicationStatus.SUBMITTED;
  }

  public static ProjectApplication submitted(UUID id, UUID positionId, UUID applicantId) {
    return new ProjectApplication(id, positionId, applicantId);
  }

  public void accept(UUID reviewerId, String note) {
    decide(ApplicationStatus.ACCEPTED, reviewerId, note);
  }

  public void reject(UUID reviewerId, String note) {
    decide(ApplicationStatus.REJECTED, reviewerId, note);
  }

  private void decide(ApplicationStatus decision, UUID reviewerId, String note) {
    if (status != ApplicationStatus.SUBMITTED) {
      throw new DomainException("application_already_decided", "Application is already decided");
    }
    this.status = decision;
    this.reviewerId = reviewerId;
    this.decisionNote = note;
  }

  public UUID id() {
    return id;
  }

  public UUID positionId() {
    return positionId;
  }

  public UUID applicantId() {
    return applicantId;
  }

  public ApplicationStatus status() {
    return status;
  }

  public UUID reviewerId() {
    return reviewerId;
  }

  public String decisionNote() {
    return decisionNote;
  }
}
