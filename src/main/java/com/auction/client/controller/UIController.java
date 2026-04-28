package com.auction.client.controller;

import com.auction.client.SceneManager;
import javafx.fxml.FXML;

public class UIController {

    @FXML
    private void handleLogout() {
        SceneManager.switchScene("login.fxml");
    }

    @FXML
    private void handleGoMyAuction() {
        // Sau này thêm màn hình đấu giá của tôi
        System.out.println("Chuyển sang màn hình đấu giá tôi");
    }
}