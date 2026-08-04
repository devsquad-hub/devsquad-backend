package com.devsquad.recruitment.domain;

import com.devsquad.shared.domain.DomainException;
import java.util.UUID;

public final class RecruitmentPosition {

    private final UUID id;
    private final String title;
    private final int capacity;
    private int filled;

    public RecruitmentPosition(UUID id, String title, int capacity, int filled) {
        if (capacity < 1 || filled < 0 || filled > capacity) {
            throw new DomainException("invalid_position_capacity", "Position capacity is invalid");
        }
        this.id = id;
        this.title = title;
        this.capacity = capacity;
        this.filled = filled;
    }

    public void reserve() {
        if (filled >= capacity) {
            throw new DomainException("position_filled", "Position is already filled");
        }
        filled++;
    }

    public int availableSlots() { return capacity - filled; }
    public UUID id() { return id; }
    public String title() { return title; }
    public int capacity() { return capacity; }
    public int filled() { return filled; }
}
