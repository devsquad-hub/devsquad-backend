package com.spring.boilerplate.domain.squads;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "sq_status")
@AllArgsConstructor @NoArgsConstructor
@Getter @Setter
public class SquadApplicationStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_status")
    private UUID id;

    @NotNull
    private String code;

    private String description;
}
