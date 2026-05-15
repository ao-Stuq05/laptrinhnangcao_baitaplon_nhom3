package com.auction.client;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class SceneManager {

    private static Stage primaryStage;

    // ✅ BỔ SUNG: lưu data muốn truyền sang màn hình tiếp theo
    private static Object pendingData;

    public static void setStage(Stage stage) {
        primaryStage = stage;
    }

    // Chuyển màn hình thông thường (không có data)
    public static void switchScene(String fxmlFile) {
        pendingData = null;
        loadScene(fxmlFile);
    }

    // ✅ BỔ SUNG: Chuyển màn hình kèm data (auction, user, ...)
    public static void switchScene(String fxmlFile, Object data) {
        pendingData = data;
        loadScene(fxmlFile);
    }

    // ✅ BỔ SUNG: Controller gọi hàm này trong initialize() để nhận data
    public static Object getAndClearData() {
        Object data = pendingData;
        pendingData = null;
        return data;
    }

    private static void loadScene(String fxmlFile) {
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