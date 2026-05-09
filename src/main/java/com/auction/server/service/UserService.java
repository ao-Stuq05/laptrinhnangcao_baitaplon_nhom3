package com.auction.server.service;

import com.auction.server.db.UserDAO;
import com.auction.shared.model.*;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public class UserService {

    private final UserDAO userDAO = new UserDAO();

    // ── ĐĂNG KÝ ──────────────────────────────────────────────

    /**
     * Đăng ký tài khoản mới.
     * @return User vừa tạo
     * @throws IllegalArgumentException nếu username đã tồn tại
     */
    public User register(String username, String email,
                         String password, String role,
                         String shopName) throws SQLException {

        // 1. Validate đầu vào
        if (username == null || username.isBlank())
            throw new IllegalArgumentException("Username không được rỗng.");
        if (password == null || password.length() < 6)
            throw new IllegalArgumentException("Mật khẩu phải từ 6 ký tự.");
        if (email == null || !email.contains("@"))
            throw new IllegalArgumentException("Email không hợp lệ.");

        // 2. Kiểm tra username trùng
        if (userDAO.existsByUsername(username))
            throw new IllegalArgumentException("Username '" + username + "' đã tồn tại.");

        // 3. Hash password trước khi tạo user
        String hashedPassword = PasswordUtil.hash(password);

        // 4. Tạo user theo role
        User user = switch (role.toUpperCase()) {
            case "BIDDER" -> new Bidder(username, email, hashedPassword, 0.0);
            case "SELLER" -> new Seller(UUID.randomUUID().toString(), username, email, hashedPassword, shopName != null ? shopName : username + "_shop", 0.0);
            case "ADMIN"  -> new Admin(username, email, hashedPassword);
            default -> throw new IllegalArgumentException("Role không hợp lệ: " + role);
        };

        // 5. Lưu vào DB
        userDAO.save(user);
        System.out.println("[UserService] Đăng ký thành công: " + username);
        return user;
    }

    // ── ĐĂNG NHẬP ────────────────────────────────────────────

    /**
     * Đăng nhập.
     * @return User nếu đúng thông tin
     * @throws IllegalArgumentException nếu sai username hoặc password
     */
    public User login(String username, String password) throws SQLException {

        // 1. Tìm user theo username
        Optional<User> found = userDAO.findByUsername(username);
        if (found.isEmpty())
            throw new IllegalArgumentException("Tài khoản không tồn tại.");

        User user = found.get();

        // 2. Kiểm tra tài khoản có bị khóa không
        if (!user.isActive())
            throw new IllegalArgumentException("Tài khoản đã bị khóa.");

        // 3. Kiểm tra mật khẩu
        // Dùng method login() đã có trong User class
        if (!user.login(password))
            throw new IllegalArgumentException("Sai mật khẩu.");

        System.out.println("[UserService] Đăng nhập thành công: "
            + username + " (" + user.getRole() + ")");
        return user;
    }
}