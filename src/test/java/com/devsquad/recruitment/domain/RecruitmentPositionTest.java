package com.devsquad.recruitment.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.devsquad.shared.domain.DomainException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RecruitmentPositionTest {

    @Test
    void reservesOnlyAvailableCapacity() {
        var position = new RecruitmentPosition(UUID.randomUUID(), "Backend", 1, 0);

        position.reserve();

        assertThat(position.availableSlots()).isZero();
        assertThatThrownBy(position::reserve)
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("filled");
    }
}
