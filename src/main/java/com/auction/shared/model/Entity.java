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
    /// constructor khoi tao khi Entity chua co trong he thong
    protected Entity() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    /// constructor khoi tao khi Entity da co trong he thong
    protected Entity(String id) {
        this.id = id;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public  void printInfo(){
        
    }

    public String getId() { return id; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    protected void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        // Vừa kiểm tra, vừa ép kiểu o thành biến entity luôn nếu đúng
        if (!(o instanceof Entity entity)) return false;
        return Objects.equals(id, entity.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{id='" + id + "', createdAt=" + createdAt + "}";
    }
}
