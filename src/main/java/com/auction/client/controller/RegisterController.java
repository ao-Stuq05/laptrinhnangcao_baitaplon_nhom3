package com.auction.client.controller;

import com.auction.client.SceneManager;
import com.auction.client.network.ServerConnection;
import com.auction.shared.model.Admin;
import com.auction.shared.model.Bidder;
import com.auction.shared.model.Message;
import com.auction.shared.model.Seller;
import com.auction.shared.model.User;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class RegisterController {

    @FXML private TextField     txtName;
    @FXML private TextField     txtEmail;
    @FXML private PasswordField txtPassword;
    @FXML private PasswordField txtConfirmPassword;
    @FXML private ComboBox<String> cmbRole;
    @FXML private Label         lblError;

    @FXML
    public void initialize() {
        cmbRole.getItems().addAll("BIDDER", "SELLER", "ADMIN");
        cmbRole.setValue("BIDDER"); // mặc định chọn BIDDER
    }

    @FXML
    private void handleRegister() {
        String name     = txtName.getText().trim();
        String email    = txtEmail.getText().trim();
        String password = txtPassword.getText().trim();
        String confirm  = txtConfirmPassword.getText().trim();
        String role     = cmbRole.getValue();

        // 1. Kiểm tra dữ liệu đầu vào
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

        if (role == null) {
            showError("Vui lòng chọn vai trò!");
            return;
        }

        // 2. Tạo đối tượng User đúng theo role
        User newUser;
        switch (role) {
            case "SELLER" ->
                    newUser = new Seller(
                            java.util.UUID.randomUUID().toString(),
                            name, email, password,
                            name + "'s Shop", // shopName mặc định
                            5.0
                    );
            case "ADMIN" ->
                    newUser = new Admin(name, email, password);
            default -> // BIDDER
                    newUser = new Bidder(name, email, password);
        }

        newUser.printInfo();

        // 3. Gửi lên Server
        try {
            ServerConnection conn = ServerConnection.getInstance();
            Message requestMsg = new Message("REGISTER", newUser);
            conn.sendMessage(requestMsg);
            System.out.println(">>> Đã gửi yêu cầu ĐĂNG KÝ [" + role + "] lên Server!");

            // Chuyển về màn hình đăng nhập
            SceneManager.switchScene("login.fxml");

        } catch (Exception e) {
            showError("Lỗi kết nối đến Server!");
            e.printStackTrace();
        }
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
