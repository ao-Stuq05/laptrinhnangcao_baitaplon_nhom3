package com.auction.client.controller;

import com.auction.client.SceneManager;
import com.auction.client.network.ServerConnection; // <-- IMPORT THÊM BƯU TÁ
import com.auction.shared.model.Message;          // <-- IMPORT THÊM PHONG BÌ
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

        // 2. Tạo đối tượng Bidder mới
        Bidder newBidder = new Bidder(name, email, password);
        newBidder.printInfo(); // in ra console để kiểm tra

        // 3. TÍCH HỢP GỬI MẠNG LÊN SERVER
        try {
            // Gọi bưu tá
            ServerConnection conn = ServerConnection.getInstance();

            // Đóng gói thông tin vào Message với nhãn "REGISTER"
            Message requestMsg = new Message("REGISTER", newBidder);

            // Gửi đi!
            conn.sendMessage(requestMsg);
            System.out.println(">>> Đã gửi yêu cầu ĐĂNG KÝ qua mạng lên Server!");

            // Chuyển về màn hình đăng nhập (Tạm thời chuyển luôn, sau này có luồng lắng nghe sẽ đợi Server báo SUCCESS mới chuyển)
            System.out.println("✅ Gửi lệnh đăng kí thành công!");
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