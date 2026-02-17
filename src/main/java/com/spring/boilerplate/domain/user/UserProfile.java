package com.spring.boilerplate.domain.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "cm_profile")
@AllArgsConstructor @NoArgsConstructor
@Getter @Setter
public class UserProfile {
    @Id
    @Column(name = "id_user")
    private UUID id;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    private String cpf;

    private String bio;
}
