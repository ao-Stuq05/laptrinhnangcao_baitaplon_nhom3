package com.auction.client.controller;

import com.auction.client.SceneManager;
import com.auction.shared.model.Bidder;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class RegisterController {

    @FXML private TextField     txtName;
    @FXML private TextField     txtEmail;
    @FXML private PasswordField txtPassword;
    @FXML private PasswordField txtConfirmPassword;
    @FXML private Label         lblError;

    @FXML
    private void handleRegister() {
        String name     = txtName.getText().trim();
        String email    = txtEmail.getText().trim();
        String password = txtPassword.getText().trim();
        String confirm  = txtConfirmPassword.getText().trim();

        if (name.isEmpty() || email.isEmpty() ||
                password.isEmpty() || confirm.isEmpty()) {
            showError("Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        if (!password.equals(confirm)) {
            showError("Mật khẩu không khớp!");
            return;
        }

        if (!email.contains("@")) {
            showError("Email không hợp lệ!");
            return;
        }

        // Tạo Bidder mới — username = txtName, passwordHash = password tạm thời
        // Sau này gửi object này lên server để lưu DB
        Bidder newBidder = new Bidder(name, email, password);
        newBidder.printInfo(); // in ra console để kiểm tra

        System.out.println("✅ Đăng kí thành công!");
        SceneManager.switchScene("login.fxml");
    }

    @FXML
    private void handleGoLogin() {
        SceneManager.switchScene("login.fxml");
    }

    private void showError(String message) {
        if (lblError != null) {
            lblError.setText(message);
            lblError.setVisible(true);
        }
        System.out.println("⚠ " + message);
    }
}