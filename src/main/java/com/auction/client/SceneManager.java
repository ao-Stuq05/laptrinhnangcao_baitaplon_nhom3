package com.auction.client;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class SceneManager {

    private static Stage primaryStage;

    // Gọi 1 lần trong Main.java để đăng ký Stage
    public static void setStage(Stage stage) {
        primaryStage = stage;
    }

    // Chuyển màn hình — truyền tên file FXML
    public static void switchScene(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    SceneManager.class.getResource(
                            "/com/auction/client/view/" + fxmlFile
                    )
            );
            Parent root = loader.load();
            Scene scene = new Scene(root);
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("❌ Không load được: " + fxmlFile);
        }
    }

    // Dùng khi cần lấy Controller sau khi load (truyền data)
    public static FXMLLoader getLoader(String fxmlFile) {
        return new FXMLLoader(
                SceneManager.class.getResource(
                        "/com/auction/client/view/" + fxmlFile
                )
        );
    }

    public static Stage getStage() {
        return primaryStage;
    }
}