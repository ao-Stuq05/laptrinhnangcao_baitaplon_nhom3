package com.auction.shared.model;
import java.io.Serializable;
/**
 * Abstract base class for all user types: Bidder, Seller, Admin.
 */
public abstract class User extends Entity implements Serializable {

    private static final long serialVersionUID = 1L;

    protected String username;
    protected String email;
    protected String passwordHash;
    protected boolean isActive;

    protected User(String id,String username, String email, String passwordHash) {
        super(id);
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.isActive = true;
    }

    /** Returns the role label: "BIDDER", "SELLER", "ADMIN" */
    public abstract String getRole();

    /**
     * Xác thực password sử dụng BCrypt.
     * So sánh rawPassword với passwordHash đã lưu trong database.
     * 
     * @param rawPassword Password người dùng nhập (chưa hash)
     * @return true nếu password khớp, false nếu không khớp
     */
    public boolean login(String rawPassword) {
        return PasswordUtil.verify(rawPassword, passwordHash);
    }

    @Override
    public void printInfo() {
        System.out.printf("[%s] username=%s | email=%s | active=%s%n",
                getRole(), username, email, isActive);
    }

    // Getters / Setters
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    public void setEmail(String email) { this.email = email; }

}
