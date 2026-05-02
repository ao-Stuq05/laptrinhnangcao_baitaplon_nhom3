package com.auction.client.controller;

import com.auction.client.SceneManager;
import com.auction.client.network.ServerConnection;
import com.auction.shared.model.Message;
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

    @FXML
    private void handleLogin() {
        String account  = txtAccount.getText().trim();
        String password = txtPassword.getText().trim();

        if (account.isEmpty() || password.isEmpty()) {
            showError("Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        try {
            // 1. Gọi đường ống mạng đã được mở sẵn
            ServerConnection conn = ServerConnection.getInstance();

            // 2. Đóng gói thông tin người dùng gõ vào một đối tượng User
            // (Mượn tạm class Bidder vì User là abstract)
            User loginData = new Bidder(account, "temp@gmail.com", password);

            // 3. Cho User vào phong bì Message, dán nhãn "LOGIN"
            Message requestMsg = new Message("LOGIN", loginData);

            // 4. Gửi phong bì này qua đường ống mạng lên Server
            conn.sendMessage(requestMsg);
            System.out.println(">>> Đã bắn gói tin Đăng nhập qua mạng lên Server!");

        } catch (Exception e) {
            showError("Lỗi kết nối mạng: Không gửi được lên Server!");
            e.printStackTrace();
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