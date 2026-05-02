package com.auction.shared.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UserLoginTest {

    private User testUser;

    @BeforeEach
    void setUp() {
        // Anh lấy y hệt dữ liệu em đang hardcode trong LoginController
        testUser = new Bidder("admin", "admin@gmail.com", "123");
    }

    @Test
    void testLoginSuccess() {
        // Kịch bản 1: Mật khẩu đúng là "123" -> Hàm login() phải trả về true
        boolean result = testUser.login("123");
        assertTrue(result, "Hàm login phải trả về true khi nhập đúng mật khẩu");
    }

    @Test
    void testLoginWrongPassword() {
        // Kịch bản 2: Nhập sai mật khẩu -> Hàm login() phải trả về false
        boolean result = testUser.login("mat_khau_bay_ba");
        assertFalse(result, "Hàm login phải trả về false khi nhập sai mật khẩu");
    }

    @Test
    void testCheckUsername() {
        // Kịch bản 3: Kiểm tra xem hàm getUsername có hoạt động đúng không
        // (vì trong Controller em có dùng nó để so sánh tài khoản)
        String username = testUser.getUsername();
        assertEquals("admin", username, "Username phải lấy ra chính xác là 'admin'");
    }
}