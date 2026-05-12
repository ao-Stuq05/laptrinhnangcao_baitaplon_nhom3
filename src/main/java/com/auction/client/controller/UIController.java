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

    @FXML
    private void handleEnterRoom() {
        SceneManager.switchScene("Product.fxml");
    }

    @FXML
    private void handleGoMyAuction() {
        System.out.println("Chuyển sang màn hình đấu giá tôi");
    }

    // MỚI: chuyển sang màn hình đăng bán sản phẩm
    @FXML
    private void handleGoProductSeller() {
        SceneManager.switchScene("ProductSeller.fxml");
    }
}
