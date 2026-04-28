package com.auction.client.controller;

import com.auction.client.SceneManager;
import com.auction.shared.model.Bidder;
import com.auction.shared.model.User;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML private TextField     txtAccount;
    @FXML private PasswordField txtPassword;
    @FXML private Label         lblError;

    // Tạm thời hardcode 1 user để test
    // Sau này thay bằng gọi server / database
    private final User fakeUser = new Bidder("admin", "admin@gmail.com", "123");

    @FXML
    private void handleLogin() {
        String account  = txtAccount.getText().trim();
        String password = txtPassword.getText().trim();

        if (account.isEmpty() || password.isEmpty()) {
            showError("Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        // Kiểm tra username khớp
        if (!fakeUser.getUsername().equals(account)) {
            showError("Tài khoản không tồn tại!");
            return;
        }

        // Dùng đúng method login() của class User bạn đã viết
        if (fakeUser.login(password)) {
            System.out.println("✅ Đăng nhập thành công: " + fakeUser.getUsername());
            SceneManager.switchScene("UI.fxml");
        } else {
            showError("Sai mật khẩu!");
        }
    }

    @FXML
    private void handleGoRegister() {
        SceneManager.switchScene("Register.fxml");
    }

    private void showError(String message) {
        if (lblError != null) {
            lblError.setText(message);
            lblError.setVisible(true);
        }
        System.out.println("⚠ " + message);
    }
}