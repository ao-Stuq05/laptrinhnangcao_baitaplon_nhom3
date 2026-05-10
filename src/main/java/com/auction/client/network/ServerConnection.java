package com.auction.client.network;

import javafx.application.Platform;
import com.auction.client.SceneManager;
import com.auction.shared.model.Message;
import com.auction.shared.model.User;

import java.io.*;
import java.net.Socket;

public class ServerConnection {
    private static ServerConnection instance;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private boolean isRunning = false;

    // Lưu user đang đăng nhập để dùng ở các màn hình khác
    private User currentUser;

    private ServerConnection() {}

    public static ServerConnection getInstance() {
        if (instance == null) {
            instance = new ServerConnection();
        }
        return instance;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void connect(String host, int port) throws IOException {
        if (socket == null || socket.isClosed()) {
            this.socket = new Socket(host, port);
            this.out = new ObjectOutputStream(socket.getOutputStream());
            this.in  = new ObjectInputStream(socket.getInputStream());
            System.out.println(">>> Đã kết nối đến Server!");
            startListening();
        }
    }

    private void startListening() {
        isRunning = true;
        Thread listenerThread = new Thread(() -> {
            try {
                while (isRunning) {
                    Message response = (Message) in.readObject();
                    handleServerResponse(response);
                }
            } catch (Exception e) {
                System.out.println("Lỗi luồng lắng nghe hoặc Server đã ngắt kết nối.");
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    private void handleServerResponse(Message msg) {
        System.out.println("<<< Nhận từ Server: " + msg.getType());

        switch (msg.getType()) {

            case "LOGIN_SUCCESS" -> {
                currentUser = (User) msg.getPayload(); // Lưu user đang đăng nhập
                System.out.println("Đăng nhập thành công: " + currentUser.getUsername()
                        + " [" + currentUser.getRole() + "]");
                Platform.runLater(() -> SceneManager.switchScene("UI.fxml"));
            }

            case "LOGIN_FAILED" -> {
                String reason = (String) msg.getPayload();
                System.out.println("Đăng nhập thất bại: " + reason);
                Platform.runLater(() -> {
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                            javafx.scene.control.Alert.AlertType.ERROR,
                            reason != null ? reason : "Sai tài khoản hoặc mật khẩu!"
                    );
                    alert.setHeaderText("Đăng nhập thất bại");
                    alert.showAndWait();
                });
            }

            case "REGISTER_SUCCESS" -> {
                User saved = (User) msg.getPayload();
                System.out.println("Đăng ký thành công: " + saved.getUsername());
                Platform.runLater(() -> {
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                            javafx.scene.control.Alert.AlertType.INFORMATION,
                            "Tài khoản '" + saved.getUsername() + "' đã được tạo thành công!"
                    );
                    alert.setHeaderText("Đăng ký thành công");
                    alert.showAndWait();
                    SceneManager.switchScene("login.fxml"); // Chuyển về màn hình đăng nhập
                });
            }

            case "REGISTER_FAILED" -> {
                String reason = (String) msg.getPayload();
                System.out.println("Đăng ký thất bại: " + reason);
                Platform.runLater(() -> {
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                            javafx.scene.control.Alert.AlertType.ERROR,
                            reason != null ? reason : "Đăng ký thất bại, vui lòng thử lại!"
                    );
                    alert.setHeaderText("Đăng ký thất bại");
                    alert.showAndWait();
                });
            }

            case "NEW_BID" -> {
                System.out.println("Có người vừa trả giá mới: " + msg.getPayload());
            }

            default -> System.out.println("[Client] Không xử lý loại tin: " + msg.getType());
        }
    }

    public void sendMessage(Message msg) throws IOException {
        if (out != null) {
            out.writeObject(msg);
            out.flush();
        }
    }

    public void close() throws IOException {
        isRunning = false;
        if (socket != null) socket.close();
    }
}