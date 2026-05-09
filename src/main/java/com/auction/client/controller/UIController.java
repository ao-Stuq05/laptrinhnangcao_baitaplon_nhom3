package com.auction.client.controller;

import com.auction.client.SceneManager;
import com.auction.client.network.ServerConnection;
import com.auction.shared.model.Message;
import javafx.fxml.FXML;

public class UIController {

    @FXML
    private void handleLogout() {
        try {
            ServerConnection conn = ServerConnection.getInstance();
            Message msg = new Message("LOGOUT", null);
            conn.sendMessage(msg);
            System.out.println(">>> Đã gửi yêu cầu ĐĂNG XUẤT lên Server!");
        } catch (Exception e) {
            System.out.println("⚠ Không thể gửi lệnh đăng xuất tới Server!");
            e.printStackTrace();
        }
        SceneManager.switchScene("login.fxml");
    }

    // THÊM MỚI: UI.fxml có onAction="#handleEnterRoom" ở 6 nút Vào phòng
    // mà UIController chưa có hàm này → lỗi
    @FXML
    private void handleEnterRoom() {
        SceneManager.switchScene("Product.fxml");
    }

    @FXML
    private void handleGoMyAuction() {
        System.out.println("Chuyển sang màn hình đấu giá tôi");
    }
}
