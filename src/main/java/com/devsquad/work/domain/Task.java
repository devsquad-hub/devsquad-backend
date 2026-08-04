package com.devsquad.work.domain;

import com.devsquad.shared.domain.DomainException;
import java.util.UUID;

public final class Task {

  private final UUID id;
  private final UUID projectId;
  private final long sequence;
  private final String title;
  private UUID columnId;
  private int version;

  private Task(UUID id, UUID projectId, long sequence, String title, UUID columnId) {
    this.id = id;
    this.projectId = projectId;
    this.sequence = sequence;
    this.title = title;
    this.columnId = columnId;
  }

  public static Task create(UUID id, UUID projectId, long sequence, String title, UUID columnId) {
    return new Task(id, projectId, sequence, title, columnId);
  }

  public void moveTo(UUID nextColumnId, int expectedVersion) {
    if (version != expectedVersion) {
      throw new DomainException("stale_task_version", "Task version is stale");
    }
    columnId = nextColumnId;
    version++;
  }

  public UUID id() {
    return id;
  }

  public UUID projectId() {
    return projectId;
  }

  public long sequence() {
    return sequence;
  }

  public String title() {
    return title;
  }

  public UUID columnId() {
    return columnId;
  }

  public int version() {
    return version;
  }
}
