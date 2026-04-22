package com.auction.shared.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Abstract base class for all domain entities in the auction system.
 */
public abstract class Entity {

    protected String id;
    protected LocalDateTime createdAt;
    protected LocalDateTime updatedAt;

    protected Entity() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    protected Entity(String id) {
        this.id = id;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public abstract void printInfo();

    public String getId() { return id; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    protected void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Entity)) return false;
        Entity entity = (Entity) o;
        return Objects.equals(id, entity.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{id='" + id + "', createdAt=" + createdAt + "}";
    }
}
