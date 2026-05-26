package com.auction.server.network;

import com.auction.server.db.AuctionDAO;
import com.auction.server.db.BidTransactionDAO;
import com.auction.server.db.ItemDAO;
import com.auction.server.db.UserDAO;
import com.auction.server.service.AuctionManager;
import com.auction.server.service.UserService;
import com.auction.shared.model.*;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class AuctionServer {
    private int port;
    private final UserService userService = new UserService();

    // Giữ danh sách tất cả ClientHandler để broadcast bid mới realtime
    private static final List<ClientHandler> connectedClients = new CopyOnWriteArrayList<>();

    public AuctionServer(int port) { this.port = port; }

    public void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println(">>> AuctionServer đang lắng nghe tại cổng " + port + "...");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("[Mạng] Client mới: " + clientSocket.getInetAddress());
                ClientHandler handler = new ClientHandler(clientSocket);
                connectedClients.add(handler);
                handler.start();
            }
        } catch (IOException e) {
            System.err.println("Lỗi khởi động Server: " + e.getMessage());
        }
    }

    /** Broadcast 1 message tới tất cả client đang kết nối (trừ sender nếu cần) */
    private static void broadcast(Message msg, ClientHandler sender) {
        for (ClientHandler c : connectedClients) {
            if (c == sender) continue;
            try {
                c.sendMessage(msg);
            } catch (IOException e) {
                System.err.println("[Broadcast] Lỗi gửi tới client: " + e.getMessage());
            }
        }
    }

    private class ClientHandler extends Thread {
        private Socket clientSocket;
        private ObjectOutputStream out;
        private ObjectInputStream in;
        private User currentUser;

        private final AuctionDAO        auctionDAO        = new AuctionDAO();
        private final ItemDAO           itemDAO           = new ItemDAO();
        private final UserDAO           userDAO           = new UserDAO();
        private final BidTransactionDAO bidTransactionDAO = new BidTransactionDAO();

        public ClientHandler(Socket socket) { this.clientSocket = socket; }

        public synchronized void sendMessage(Message msg) throws IOException {
            if (out != null) { out.writeObject(msg); out.flush(); }
        }

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
                                currentUser = loggedIn;
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

                                // ── Gán Base64 ảnh vào item nếu có ─────────────
                                String imageBase64 = (String) payload.get("imageBase64");
                                if (imageBase64 != null && !imageBase64.isEmpty()) {
                                    item.setImageBase64(imageBase64);
                                    System.out.println("[Server] Đã nhận ảnh Base64 cho item: " + itemId);
                                }

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

                        // ── LẤY DANH SÁCH PHIÊN ĐẤU GIÁ ──────────────────────
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

                        // ── ĐẶT GIÁ ───────────────────────────────────────────
                        case "PLACE_BID" -> {
                            if (!(currentUser instanceof Bidder)) {
                                out.writeObject(new Message("PLACE_BID_FAILED",
                                        "Chỉ tài khoản Bidder mới được đặt giá!"));
                                out.flush();
                                break;
                            }
                            try {
                                @SuppressWarnings("unchecked")
                                HashMap<String, Object> p = (HashMap<String, Object>) request.getPayload();
                                String auctionId = (String) p.get("auctionId");
                                double amount    = (double) p.get("amount");

                                Bidder bidder = (Bidder) currentUser;

                                // Lấy phiên từ AuctionManager (in-memory) trước, fallback sang DB
                                Auction auction = AuctionManager.getInstance().getAuction(auctionId);
                                if (auction == null) {
                                    // Load từ DB nếu chưa có trong memory
                                    auction = auctionDAO.findById(auctionId)
                                            .orElseThrow(() -> new IllegalArgumentException(
                                                    "Không tìm thấy phiên đấu giá: " + auctionId));
                                    AuctionManager.getInstance().registerAuction(auction);
                                }

                                // Kiểm tra số dư
                                if (bidder.getBalance() < amount) {
                                    out.writeObject(new Message("PLACE_BID_FAILED",
                                            "Số dư không đủ! Số dư hiện tại: "
                                                    + String.format("%,.0f đ", bidder.getBalance())));
                                    out.flush();
                                    break;
                                }

                                // Kiểm tra giá hợp lệ
                                if (amount <= auction.getCurrentPrice()) {
                                    out.writeObject(new Message("PLACE_BID_FAILED",
                                            String.format("Giá phải cao hơn %,.0f đ!", auction.getCurrentPrice())));
                                    out.flush();
                                    break;
                                }

                                // Thực hiện đặt giá (cập nhật in-memory)
                                auction.placeBid(bidder, amount);

                                // Tạo BidTransaction và lưu vào DB
                                BidTransaction tx = new BidTransaction(bidder, amount, auctionId);
                                bidTransactionDAO.save(tx);

                                // Cập nhật auction trong DB (current_price, status)
                                auctionDAO.update(auction);

                                // Trừ số dư bidder
                                double newBalance = bidder.getBalance() - amount;
                                userDAO.updateBalance(bidder.getId(), newBalance);
                                bidder.setBalance(newBalance);

                                // Gửi kết quả về người đặt
                                out.writeObject(new Message("PLACE_BID_SUCCESS", tx));
                                out.flush();

                                // Broadcast NEW_BID tới tất cả client khác đang xem
                                broadcast(new Message("NEW_BID", tx), this);

                                System.out.printf("[Server] PLACE_BID OK: %s đặt %.0f vào %s%n",
                                        bidder.getUsername(), amount, auctionId);

                            } catch (IllegalArgumentException e) {
                                out.writeObject(new Message("PLACE_BID_FAILED", e.getMessage()));
                                out.flush();
                            } catch (Exception e) {
                                out.writeObject(new Message("PLACE_BID_FAILED", "Lỗi server: " + e.getMessage()));
                                e.printStackTrace();
                                out.flush();
                            }
                        }

                        // ── NẠP TIỀN ─────────────────────────────────────────
                        case "TOP_UP" -> {
                            if (!(currentUser instanceof Bidder)) {
                                out.writeObject(new Message("TOP_UP_FAILED",
                                        "Chỉ tài khoản Bidder mới nạp tiền được!"));
                                out.flush();
                                break;
                            }
                            try {
                                @SuppressWarnings("unchecked")
                                HashMap<String, Object> p = (HashMap<String, Object>) request.getPayload();
                                double amount = (double) p.get("amount");

                                Bidder bidder = (Bidder) currentUser;
                                double newBal = bidder.getBalance() + amount;
                                userDAO.updateBalance(bidder.getId(), newBal);
                                bidder.setBalance(newBal);

                                out.writeObject(new Message("TOP_UP_SUCCESS", newBal));
                                System.out.println("[Server] TOP_UP OK: " + bidder.getUsername()
                                        + " | +" + amount + " | Số dư mới: " + newBal);
                            } catch (Exception e) {
                                out.writeObject(new Message("TOP_UP_FAILED", "Lỗi server: " + e.getMessage()));
                                e.printStackTrace();
                            }
                            out.flush();
                        }

                        // ── LẤY PHIÊN ĐẤU GIÁ CỦA SELLER ───────────────────
                        case "GET_MY_AUCTIONS" -> {
                            try {
                                List<Auction> list;
                                if (currentUser instanceof Seller seller) {
                                    list = auctionDAO.findBySeller(seller.getId());
                                } else {
                                    list = new ArrayList<>();
                                }
                                out.writeObject(new Message("GET_MY_AUCTIONS_SUCCESS",
                                        (java.io.Serializable) list));
                                System.out.println("[Server] GET_MY_AUCTIONS OK: " + list.size() + " phiên");
                            } catch (Exception e) {
                                out.writeObject(new Message("GET_MY_AUCTIONS_SUCCESS",
                                        new ArrayList<>()));
                                e.printStackTrace();
                            }
                            out.flush();
                        }

                        // ── LẤY LỊCH SỬ BID CỦA BIDDER ─────────────────────
                        case "GET_MY_BIDS" -> {
                            try {
                                List<BidTransaction> list;
                                if (currentUser instanceof Bidder bidder) {
                                    list = bidTransactionDAO.findByBidderWithItem(bidder.getId(), auctionDAO);
                                } else {
                                    list = new ArrayList<>();
                                }
                                out.writeObject(new Message("GET_MY_BIDS_SUCCESS",
                                        (java.io.Serializable) list));
                                System.out.println("[Server] GET_MY_BIDS OK: " + list.size() + " giao dịch");
                            } catch (Exception e) {
                                out.writeObject(new Message("GET_MY_BIDS_SUCCESS",
                                        new ArrayList<>()));
                                e.printStackTrace();
                            }
                            out.flush();
                        }

                        // ── CẬP NHẬT THÔNG TIN CÁ NHÂN ──────────────────────
                        case "UPDATE_PROFILE" -> {
                            if (currentUser == null) {
                                out.writeObject(new Message("UPDATE_PROFILE_FAILED", "Chưa đăng nhập!"));
                                out.flush();
                                break;
                            }
                            try {
                                @SuppressWarnings("unchecked")
                                HashMap<String, Object> p = (HashMap<String, Object>) request.getPayload();
                                String newEmail = (String) p.get("email");
                                String newPass  = (String) p.get("password"); // null nếu không đổi

                                userDAO.updateProfile(currentUser.getId(), newEmail, newPass);
                                currentUser.setEmail(newEmail);

                                out.writeObject(new Message("UPDATE_PROFILE_SUCCESS", currentUser));
                                System.out.println("[Server] UPDATE_PROFILE OK: " + currentUser.getUsername());
                            } catch (Exception e) {
                                out.writeObject(new Message("UPDATE_PROFILE_FAILED", "Lỗi: " + e.getMessage()));
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
                connectedClients.remove(this);
                try { if (clientSocket != null) clientSocket.close(); }
                catch (IOException ex) { ex.printStackTrace(); }
            }
        }
    }
}