package com.auction.shared.model;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtil {

    /**
     * Hash password sử dụng BCrypt.
     * 
     * @param rawPassword Password chưa được hash
     * @return Password đã được hash (bao gồm salt)
     */
    public static String hash(String rawPassword) {
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt());
    }

    /**
     * Xác thực password so với hash đã lưu.
     * 
     * @param rawPassword Password người dùng nhập
     * @param hashedPassword Password đã hash trong database
     *                       hoặc plain text khi hệ thống chưa chuyển sang BCrypt.
     * @return true nếu password khớp, false nếu không khớp
     */
    public static boolean verify(String rawPassword, String hashedPassword) {
        if (hashedPassword == null || rawPassword == null) {
            return false;
        }

        // Hỗ trợ cả BCrypt hash và dữ liệu mật khẩu chưa mã hoá hiện có.
        if (hashedPassword.startsWith("$2a$")
                || hashedPassword.startsWith("$2b$")
                || hashedPassword.startsWith("$2y$")) {
            return BCrypt.checkpw(rawPassword, hashedPassword);
        }

        return rawPassword.equals(hashedPassword);
    }
}