package com.auction.server.network;

import com.auction.server.db.AuctionDAO;
import com.auction.server.db.ItemDAO;
import com.auction.server.service.AuctionManager;
import com.auction.server.service.UserService;
import com.auction.shared.model.*;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class AuctionServer {
    private int port;
    private final UserService userService = new UserService();

    public AuctionServer(int port) { this.port = port; }

    public void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println(">>> AuctionServer đang lắng nghe tại cổng " + port + "...");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("[Mạng] Client mới: " + clientSocket.getInetAddress());
                new ClientHandler(clientSocket).start();
            }
        } catch (IOException e) {
            System.err.println("Lỗi khởi động Server: " + e.getMessage());
        }
    }

    private class ClientHandler extends Thread {
        private Socket clientSocket;
        private ObjectOutputStream out;
        private ObjectInputStream in;
        private User currentUser;

        private final AuctionDAO auctionDAO = new AuctionDAO();
        private final ItemDAO    itemDAO    = new ItemDAO();

        public ClientHandler(Socket socket) { this.clientSocket = socket; }

        @Override
        public void run() {
            try {
                out = new ObjectOutputStream(clientSocket.getOutputStream());
                in  = new ObjectInputStream(clientSocket.getInputStream());

                while (true) {
                    Message request = (Message) in.readObject();
                    System.out.println("[ClientHandler] Nhận: " + request.getType());

                    switch (request.getType()) {

                        // ── ĐĂNG NHẬP ──────────────────────────────────────────
                        case "LOGIN" -> {
                            User loginData = (User) request.getPayload();
                            try {
                                User loggedIn = userService.login(
                                        loginData.getUsername(), loginData.getPasswordHash());
                                currentUser = loggedIn; // ✅ lưu lại
                                out.writeObject(new Message("LOGIN_SUCCESS", loggedIn));
                                System.out.println("[Server] Login OK: " + loggedIn.getUsername());
                            } catch (IllegalArgumentException e) {
                                out.writeObject(new Message("LOGIN_FAILED", e.getMessage()));
                            }
                            out.flush();
                        }

                        // ── ĐĂNG KÝ ────────────────────────────────────────────
                        case "REGISTER" -> {
                            User newUser = (User) request.getPayload();
                            try {
                                User saved = userService.register(
                                        newUser.getUsername(), newUser.getEmail(),
                                        newUser.getPasswordHash(), newUser.getRole(), null);
                                out.writeObject(new Message("REGISTER_SUCCESS", saved));
                                System.out.println("[Server] Register OK: " + saved.getUsername());
                            } catch (IllegalArgumentException e) {
                                out.writeObject(new Message("REGISTER_FAILED", e.getMessage()));
                            }
                            out.flush();
                        }

                        // ── TẠO PHIÊN ĐẤU GIÁ ─────────────────────────────────
                        case "CREATE_AUCTION" -> {
                            if (currentUser == null || !(currentUser instanceof Seller)) {
                                out.writeObject(new Message("CREATE_AUCTION_FAILED",
                                        "Bạn cần đăng nhập với tài khoản Seller!"));
                                out.flush();
                                break;
                            }
                            Seller seller = (Seller) currentUser;
                            try {
                                @SuppressWarnings("unchecked")
                                HashMap<String, Object> payload = (HashMap<String, Object>) request.getPayload();

                                String name      = (String) payload.get("name");
                                String category  = (String) payload.get("category");
                                String desc      = (String) payload.get("description");
                                double price     = (double) payload.get("startPrice");
                                LocalDate startD = LocalDate.parse((String) payload.get("startDate"));
                                LocalDate endD   = LocalDate.parse((String) payload.get("endDate"));

                                String itemId = UUID.randomUUID().toString();
                                Item item = switch (category) {
                                    case "Điện tử"            -> new Electronics(itemId, name, desc, price, seller, 12);
                                    case "Nghệ thuật"         -> new Art(itemId, name, desc, price, seller, "Unknown", 2024);
                                    case "Xe cộ"              -> new Vehicle(itemId, name, desc, price, seller, "Unknown", 0);
                                    case "Đồng hồ & Trang sức",
                                         "Cổ vật", "Khác"    -> new Electronics(itemId, name, desc, price, seller, 0);
                                    default -> throw new IllegalArgumentException("Danh mục không hợp lệ: " + category);
                                };

                                itemDAO.save(item);

                                String auctionId = "AUC-" + System.currentTimeMillis();
                                Auction auction  = new Auction(auctionId, item, seller,
                                        startD.atStartOfDay(), endD.atStartOfDay());

                                auctionDAO.save(auction);
                                AuctionManager.getInstance().registerAuction(auction);

                                out.writeObject(new Message("CREATE_AUCTION_SUCCESS", auction));
                                System.out.println("[Server] CREATE_AUCTION OK: " + auctionId);

                            } catch (IllegalArgumentException e) {
                                out.writeObject(new Message("CREATE_AUCTION_FAILED", e.getMessage()));
                            } catch (Exception e) {
                                out.writeObject(new Message("CREATE_AUCTION_FAILED", "Lỗi server: " + e.getMessage()));
                                e.printStackTrace();
                            }
                            out.flush();
                        }

                        // ── LẤY DANH SÁCH PHIÊN ĐẤU GIÁ ✅ MỚI ───────────────
                        case "GET_AUCTIONS" -> {
                            try {
                                List<Auction> auctions = auctionDAO.findActive();
                                out.writeObject(new Message("GET_AUCTIONS_SUCCESS", (java.io.Serializable) auctions));
                                System.out.println("[Server] GET_AUCTIONS OK: " + auctions.size() + " phiên");
                            } catch (Exception e) {
                                out.writeObject(new Message("GET_AUCTIONS_FAILED", e.getMessage()));
                                e.printStackTrace();
                            }
                            out.flush();
                        }

                        // ── ĐĂNG XUẤT ──────────────────────────────────────────
                        case "LOGOUT" -> {
                            System.out.println("[Server] " +
                                    (currentUser != null ? currentUser.getUsername() : "?") + " đăng xuất.");
                            currentUser = null;
                        }

                        default -> System.out.println("[Server] Không xử lý: " + request.getType());
                    }
                }

            } catch (EOFException e) {
                System.out.println("[ClientHandler] Client đóng kết nối.");
            } catch (Exception e) {
                System.err.println("[ClientHandler] Lỗi: " + e.getMessage());
            } finally {
                try { if (clientSocket != null) clientSocket.close(); }
                catch (IOException ex) { ex.printStackTrace(); }
            }
        }
    }
}