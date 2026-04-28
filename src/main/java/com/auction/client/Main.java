package com.auction.client;

import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        // Đăng ký Stage vào SceneManager
        SceneManager.setStage(stage);

        stage.setTitle("Auction System");
        stage.setResizable(false);

        // Mở màn hình Login đầu tiên
        SceneManager.switchScene("login.fxml");
    }

    public static void main(String[] args) {
        launch();
    }
}