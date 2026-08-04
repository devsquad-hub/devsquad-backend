package com.devsquad.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.devsquad.shared.domain.DomainException;
import java.util.List;
import org.junit.jupiter.api.Test;

class AccountProfileTest {

    @Test
    void normalizesEditableProfileData() {
        var profile = AccountProfile.create(
                "  Ada Lovelace  ", "  Backend engineer  ", List.of(" Java ", "PostgreSQL", "java"),
                "https://github.com/ada", null, null, 8);

        assertThat(profile.displayName()).isEqualTo("Ada Lovelace");
        assertThat(profile.skills()).containsExactly("Java", "PostgreSQL");
        assertThat(profile.availabilityHours()).isEqualTo(8);
    }

    @Test
    void rejectsInvalidAvailability() {
        assertThatThrownBy(() -> AccountProfile.create("Ada", null, List.of(), null, null, null, -1))
                .isInstanceOf(DomainException.class)
                .extracting("code")
                .isEqualTo("invalid_availability_hours");
    }
}
