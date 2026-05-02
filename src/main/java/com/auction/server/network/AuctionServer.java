package com.auction.server.network;

import com.auction.shared.model.Message;
import com.auction.shared.model.User;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class AuctionServer {
    private int port;

    public AuctionServer(int port) {
        this.port = port;
    }

    public void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println(">>> AuctionServer đang lắng nghe kết nối tại cổng " + port + "...");

            while (true) {
                // Lễ tân đứng đợi khách
                Socket clientSocket = serverSocket.accept();
                System.out.println("\n[Mạng] Có Client mới kết nối từ IP: " + clientSocket.getInetAddress());

                // Gọi nhân viên phục vụ (ClientHandler) ra chăm sóc khách này
                ClientHandler handler = new ClientHandler(clientSocket);
                handler.start();
            }

        } catch (IOException e) {
            System.err.println("Lỗi khởi động Server: " + e.getMessage());
        }
    }

    // =========================================================================
    // INNER CLASS: Đưa ClientHandler vào thẳng đây để khỏi phải tạo file mới
    // =========================================================================
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
                in = new ObjectInputStream(clientSocket.getInputStream());

                while (true) {
                    Message request = (Message) in.readObject();
                    System.out.println("[ClientHandler] Nhận được yêu cầu: " + request.getType());

                    if ("LOGIN".equals(request.getType())) {
                        User loginData = (User) request.getPayload();

                        // Demo test cứng
                        if ("admin".equals(loginData.getUsername())) {
                            out.writeObject(new Message("SUCCESS", "Đăng nhập thành công!"));
                        } else {
                            out.writeObject(new Message("ERROR", "Sai tài khoản!"));
                        }
                        out.flush();
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