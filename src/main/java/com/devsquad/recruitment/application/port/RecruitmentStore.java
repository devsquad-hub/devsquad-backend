package com.devsquad.recruitment.application.port;

import com.devsquad.recruitment.application.RecruitmentService.ApplicationView;
import com.devsquad.recruitment.application.RecruitmentService.PositionCommand;
import com.devsquad.recruitment.application.RecruitmentService.PositionView;
import com.devsquad.recruitment.application.RecruitmentService.RoundCommand;
import java.util.List;
import java.util.UUID;

public interface RecruitmentStore {
    UUID createRound(UUID projectId, RoundCommand command);
    UUID projectForRound(UUID roundId);
    UUID createPosition(UUID roundId, PositionCommand command);
    void openRound(UUID roundId, UUID projectId);
    List<PositionView> publicPositions(UUID projectId);
    PositionContext positionContext(UUID positionId);
    UUID apply(UUID positionId, UUID projectId, UUID applicantId, Object answers);
    ApplicationContext applicationContext(UUID applicationId);
    void decide(UUID applicationId, ApplicationContext context, UUID reviewerId, boolean accept, String note);
    List<ApplicationView> applications(UUID projectId);
    UUID invite(UUID projectId, UUID accountId, UUID positionId, String functionalRole, UUID inviterId);
    void respondToInvitation(UUID invitationId, UUID accountId, boolean accept);

    record PositionContext(UUID projectId, UUID hubId) {}
    record ApplicationContext(UUID positionId, UUID applicantId, UUID projectId) {}
}
