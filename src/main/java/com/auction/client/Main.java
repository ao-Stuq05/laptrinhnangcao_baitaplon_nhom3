package com.auction.client;

import com.auction.client.network.ServerConnection;
import javafx.application.Application;
import javafx.scene.control.Alert;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;
import java.io.IOException;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        // 1. Hỏi IP máy chủ (hỗ trợ chạy qua mạng LAN)
        TextInputDialog dialog = new TextInputDialog("localhost");
        dialog.setTitle("Kết nối Server");
        dialog.setHeaderText("Nhập địa chỉ IP của máy đang chạy Server.\n(Nếu chạy cùng máy, giữ nguyên 'localhost')");
        dialog.setContentText("IP Server:");

        // Bắt buộc người dùng nhập, không cho bỏ qua
        String host = dialog.showAndWait().orElse("localhost").trim();
        if (host.isEmpty()) host = "localhost";

        // 2. Cố gắng kết nối đến Server
        try {
            ServerConnection.getInstance().connect(host, 1234);
        } catch (IOException e) {
            // Hiển thị cảnh báo rõ ràng thay vì chỉ in ra console
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Không thể kết nối");
            alert.setHeaderText("Kết nối tới Server thất bại!");
            alert.setContentText(
                "Không thể kết nối tới: " + host + ":1234\n\n" +
                "Hãy kiểm tra:\n" +
                "  • Máy Server đã bật chưa?\n" +
                "  • IP nhập có đúng không? (dùng lệnh 'ipconfig' để kiểm tra)\n" +
                "  • Firewall có đang chặn cổng 1234 không?"
            );
            alert.showAndWait();
        }

        // 3. Khởi tạo giao diện
        SceneManager.setStage(stage);
        stage.setTitle("Auction System");
        stage.setResizable(false);

        // Mở màn hình Login đầu tiên
        SceneManager.switchScene("login.fxml");
    }

    // 4. Hàm stop() tự động chạy khi người dùng bấm dấu [X] tắt cửa sổ
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
