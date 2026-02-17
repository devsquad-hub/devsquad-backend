package com.spring.boilerplate.domain.project;

import com.spring.boilerplate.domain.user.User;
import com.spring.boilerplate.domain.user.UserProfile;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProjectComplement {
    @Id
    @Column(name = "id_project")
    private UUID projectId;

    @JoinColumn(name = "id_user")
    @ManyToOne
    private UserProfile userProfile;
}
