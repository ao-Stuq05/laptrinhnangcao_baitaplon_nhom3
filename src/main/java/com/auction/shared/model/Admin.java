package com.auction.shared.model;

import java.util.UUID;

public class Admin extends User {

    private static final long serialVersionUID = 1L;

    private String adminLevel; // "SUPER" | "NORMAL"

    public Admin(String username, String email, String passwordHash) {
        super(UUID.randomUUID().toString(), username, email, passwordHash);
        this.adminLevel = "NORMAL";
    }

    public Admin(String id, String username, String email,
                 String passwordHash, String adminLevel) {
        super(id, username, email, passwordHash);
        this.adminLevel = adminLevel;
    }

    @Override
    public String getRole() { return "ADMIN"; }

    public String getAdminLevel()             { return adminLevel; }
    public void setAdminLevel(String level)   { this.adminLevel = level; }
}
