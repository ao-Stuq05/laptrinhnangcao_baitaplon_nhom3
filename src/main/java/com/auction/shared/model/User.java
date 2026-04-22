package com.auction.shared.model.user;

import com.auction.model.Entity;

/**
 * Abstract base class for all user types: Bidder, Seller, Admin.
 */
public abstract class User extends Entity {

    protected String username;
    protected String email;
    protected String passwordHash;
    protected boolean isActive;

    protected User(String username, String email, String passwordHash) {
        super();
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.isActive = true;
    }

    /** Returns the role label: "BIDDER", "SELLER", "ADMIN" */
    public abstract String getRole();

    /**
     * Simple login check — in production, use BCrypt.
     * Returns true if the provided raw password matches the stored hash.
     */
    public boolean login(String rawPassword) {
        // TODO: replace with BCrypt.checkpw(rawPassword, passwordHash)
        return passwordHash.equals(rawPassword);
    }

    @Override
    public void printInfo() {
        System.out.printf("[%s] username=%s | email=%s | active=%s%n",
                getRole(), username, email, isActive);
    }

    // Getters / Setters
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    public void setEmail(String email) { this.email = email; }
}
