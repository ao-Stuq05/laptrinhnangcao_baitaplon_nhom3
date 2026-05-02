package com.auction.client.controller;

import com.auction.client.SceneManager;
import com.auction.client.network.ServerConnection; // <-- Gọi bưu tá
import com.auction.shared.model.Message;          // <-- Gọi phong bì
import javafx.fxml.FXML;

public class UIController {

    @FXML
    private void handleLogout() {
        // TÍCH HỢP MẠNG: Báo cho Server biết mình thoát
        try {
            ServerConnection conn = ServerConnection.getInstance();
            Message msg = new Message("LOGOUT", null);
            conn.sendMessage(msg);
            System.out.println(">>> Đã gửi yêu cầu ĐĂNG XUẤT lên Server!");
        } catch (Exception e) {
            System.out.println("⚠ Không thể gửi lệnh đăng xuất tới Server!");
            e.printStackTrace();
        }

        // Chuyển về màn hình đăng nhập (Giữ nguyên code cũ của em)
        SceneManager.switchScene("login.fxml");
    }

    @FXML
    private void handleGoMyAuction() {
        // Sau này thêm màn hình đấu giá của tôi
        System.out.println("Chuyển sang màn hình đấu giá tôi");
    }
}