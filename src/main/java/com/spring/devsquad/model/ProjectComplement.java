package com.spring.devsquad.model;

import com.spring.devsquad.model.UserProfile;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "pj_project_compl")
public class ProjectComplement {
    @Id
    @Column(name = "id_project")
    private UUID projectId;

    @JoinColumn(name = "id_user")
    @ManyToOne
    private UserProfile userProfile;
}
