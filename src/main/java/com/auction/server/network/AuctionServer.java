package com.auction.server.network;

import com.auction.server.service.UserService;
import com.auction.shared.model.Message;
import com.auction.shared.model.User;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class AuctionServer {
    private int port;

    // Khởi tạo UserService một lần duy nhất dùng chung cho toàn server
    private final UserService userService = new UserService();

    public AuctionServer(int port) {
        this.port = port;
    }

    public void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println(">>> AuctionServer đang lắng nghe kết nối tại cổng " + port + "...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("\n[Mạng] Có Client mới kết nối từ IP: " + clientSocket.getInetAddress());

                ClientHandler handler = new ClientHandler(clientSocket);
                handler.start();
            }

        } catch (IOException e) {
            System.err.println("Lỗi khởi động Server: " + e.getMessage());
        }
    }

    private class ClientHandler extends Thread {
        private Socket clientSocket;
        private ObjectOutputStream out;
        private ObjectInputStream in;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try {
                out = new ObjectOutputStream(clientSocket.getOutputStream());
                in  = new ObjectInputStream(clientSocket.getInputStream());

                while (true) {
                    Message request = (Message) in.readObject();
                    System.out.println("[ClientHandler] Nhận được yêu cầu: " + request.getType());

                    switch (request.getType()) {

                        // ── ĐĂNG NHẬP ──────────────────────────────────────────
                        case "LOGIN" -> {
                            User loginData = (User) request.getPayload();
                            try {
                                // Gọi UserService → UserDAO → MySQL
                                User loggedIn = userService.login(
                                        loginData.getUsername(),
                                        loginData.getPasswordHash() // client gửi raw password ở đây
                                );
                                out.writeObject(new Message("LOGIN_SUCCESS", loggedIn));
                                System.out.println("[Server] Đăng nhập OK: " + loggedIn.getUsername());
                            } catch (IllegalArgumentException e) {
                                out.writeObject(new Message("LOGIN_FAILED", e.getMessage()));
                                System.out.println("[Server] Đăng nhập THẤT BẠI: " + e.getMessage());
                            }
                            out.flush();
                        }

                        // ── ĐĂNG KÝ ────────────────────────────────────────────
                        case "REGISTER" -> {
                            User newUser = (User) request.getPayload();
                            try {
                                // Gọi UserService → UserDAO → INSERT vào MySQL
                                User saved = userService.register(
                                        newUser.getUsername(),
                                        newUser.getEmail(),
                                        newUser.getPasswordHash(), // client gửi raw password
                                        newUser.getRole(),
                                        null // shopName sẽ tự sinh nếu null
                                );
                                out.writeObject(new Message("REGISTER_SUCCESS", saved));
                                System.out.println("[Server] Đăng ký OK: " + saved.getUsername());
                            } catch (IllegalArgumentException e) {
                                out.writeObject(new Message("REGISTER_FAILED", e.getMessage()));
                                System.out.println("[Server] Đăng ký THẤT BẠI: " + e.getMessage());
                            }
                            out.flush();
                        }

                        default -> {
                            System.out.println("[Server] Loại tin nhắn không xử lý: " + request.getType());
                        }
                    }
                }

            } catch (EOFException e) {
                System.out.println("[ClientHandler] Client đã đóng kết nối.");
            } catch (Exception e) {
                System.err.println("[ClientHandler] Lỗi: " + e.getMessage());
            } finally {
                try {
                    if (clientSocket != null) clientSocket.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
}