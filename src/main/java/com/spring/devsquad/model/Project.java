package com.spring.devsquad.model;

import com.spring.devsquad.shared.model.AuditableEntity;
import com.spring.devsquad.model.UserProfile;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "pj_project")
@Getter @Setter
@AllArgsConstructor
public class Project extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_project")
    private UUID id;

    private String title;

    private String description;

    @Column(name = "tech_stack")
    private String techStack;

    private String difficulty;

    @Enumerated(EnumType.STRING)
    private ProjectStatus status;

    @JoinColumn(name = "created_by")
    @ManyToOne(fetch = FetchType.LAZY)
    private UserProfile userAuthor;

    public Project(){}

    public Project(String title, String description, String techStack, String difficulty, ProjectStatus status, UserProfile userAuthor) {
        this.title = title;
        this.description = description;
        this.techStack = techStack;
        this.difficulty = difficulty;
        this.status = status;
        this.userAuthor = userAuthor;
    }
}
