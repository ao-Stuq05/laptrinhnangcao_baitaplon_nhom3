package com.auction.client.network;

import com.auction.shared.model.Message;
import java.io.*;
import java.net.Socket;

public class ServerConnection {
    private static ServerConnection instance;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private boolean isRunning = false; // Biến kiểm soát luồng nghe

    private ServerConnection() {}

    public static ServerConnection getInstance() {
        if (instance == null) {
            instance = new ServerConnection();
        }
        return instance;
    }

    public void connect(String host, int port) throws IOException {
        if (socket == null || socket.isClosed()) {
            this.socket = new Socket(host, port);
            this.out = new ObjectOutputStream(socket.getOutputStream());
            this.in = new ObjectInputStream(socket.getInputStream());
            System.out.println(">>> Đã kết nối đến Server!");

            // BẮT ĐẦU LẮNG NGHE TỰ ĐỘNG
            startListening();
        }
    }

    private void startListening() {
        isRunning = true;
        Thread listenerThread = new Thread(() -> {
            try {
                while (isRunning) {
                    // Đợi và nhận tin nhắn từ Server bất cứ lúc nào
                    Message response = (Message) in.readObject();
                    handleServerResponse(response);
                }
            } catch (Exception e) {
                System.out.println("Lỗi luồng lắng nghe hoặc Server đã ngắt kết nối.");
            }
        });
        listenerThread.setDaemon(true); // Đảm bảo Thread tắt khi app tắt
        listenerThread.start();
    }

    // NƠI XỬ LÝ MỌI PHẢN HỒI TỪ SERVER TRẢ VỀ
    private void handleServerResponse(Message msg) {
        System.out.println("<<< Nhận từ Server: " + msg.getType());

        switch (msg.getType()) {
            case "LOGIN_SUCCESS":
                System.out.println("Đăng nhập thành công rồi!");
                // Ở đây em có thể gọi code để chuyển sang màn hình UI.fxml
                break;
            case "LOGIN_FAILED":
                System.out.println("Sai tài khoản hoặc mật khẩu!");
                break;
            case "NEW_BID":
                System.out.println("Có người vừa trả giá mới: " + msg.getPayload());
                break;
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