package com.auction.client;

import com.auction.client.network.ServerConnection; // Import lớp em vừa tạo
import javafx.application.Application;
import javafx.stage.Stage;
import java.io.IOException; // Import để bắt lỗi mạng

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        // 1. Cố gắng kết nối đến Server ngay khi mở app
        try {
            // Lưu ý: Cổng 1234 phải giống hệt cổng Thành viên A cài đặt ở ServerSocket
            ServerConnection.getInstance().connect("localhost", 1234);
        } catch (IOException e) {
            System.err.println("CẢNH BÁO: Không thể kết nối! Hãy nhắc Thành viên A bật Server lên trước.");
            // Ở tuần sau (khi làm mịn GUI), em có thể thay dòng in này bằng một hộp thoại cảnh báo (Alert) của JavaFX.
        }

        // 2. Phần code khởi tạo giao diện của em giữ nguyên
        SceneManager.setStage(stage);
        stage.setTitle("Auction System");
        stage.setResizable(false);

        // Mở màn hình Login đầu tiên
        SceneManager.switchScene("login.fxml");
    }

    // 3. Hàm stop() tự động chạy khi người dùng bấm dấu [X] tắt cửa sổ
    @Override
    public void stop() throws Exception {
        ServerConnection.getInstance().close(); // Đóng "đường ống" sạch sẽ
        System.out.println("Đã ngắt kết nối an toàn.");
        super.stop();
    }

    public static void main(String[] args) {
        launch();
    }
}