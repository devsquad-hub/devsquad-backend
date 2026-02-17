package com.spring.boilerplate.domain.squads;

import com.spring.boilerplate.core.entity.AuditableEntity;
import com.spring.boilerplate.domain.project.Project;
import com.spring.boilerplate.domain.user.User;
import com.spring.boilerplate.domain.user.UserProfile;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sq_application")
@Getter @Setter
@AllArgsConstructor @NoArgsConstructor
public class SquadApplication extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_application")
    private UUID id;

    @JoinColumn(name = "id_project")
    @ManyToOne(fetch = FetchType.LAZY)
    private Project project;

    @JoinColumn(name = "id_user")
    @ManyToOne(fetch = FetchType.LAZY)
    private UserProfile user;

    @NotNull
    @Column(name = "id_status")
    private Integer statusId;

    @NotNull
    private String motivation;

    private String roleDesired;
}
