package com.auction.client.network;

import com.auction.shared.model.Message;
import java.io.*;
import java.net.Socket;

public class ServerConnection {
    // Singleton: Đảm bảo toàn bộ ứng dụng chỉ dùng 1 kết nối duy nhất
    private static ServerConnection instance;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    private ServerConnection() {}

    public static ServerConnection getInstance() {
        if (instance == null) {
            instance = new ServerConnection();
        }
        return instance;
    }

    // Hàm kết nối đến máy của Thành viên A
    public void connect(String host, int port) throws IOException {
        if (socket == null || socket.isClosed()) {
            this.socket = new Socket(host, port);

            // QUAN TRỌNG: Phải tạo Output trước Input để tránh bị treo (deadlock)
            this.out = new ObjectOutputStream(socket.getOutputStream());
            this.in = new ObjectInputStream(socket.getInputStream());

            System.out.println(">>> Đã kết nối đến Server thành công!");
        }
    }

    // Gửi Message đi (Dùng Object đã chuẩn bị ở bước 1 & 2)
    public void sendMessage(Message msg) throws IOException {
        if (out != null) {
            out.writeObject(msg);
            out.flush(); // Đẩy dữ liệu đi ngay lập tức
        }
    }

    // Nhận Message về từ Server
    public Message receiveMessage() throws IOException, ClassNotFoundException {
        if (in != null) {
            return (Message) in.readObject();
        }
        return null;
    }

    // Đóng kết nối khi tắt app
    public void close() throws IOException {
        if (socket != null) socket.close();
    }
}