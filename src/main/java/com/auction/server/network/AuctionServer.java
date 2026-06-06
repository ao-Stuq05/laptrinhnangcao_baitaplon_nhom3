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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class AuctionServer {
    private int port;
    private final UserService userService = new UserService();

    private static final List<ClientHandler> connectedClients = new CopyOnWriteArrayList<>();

    public AuctionServer(int port) { this.port = port; }

    public void startServer() {
        // Đăng ký callback xử lý khi phiên đấu giá kết thúc (DB + tài chính + broadcast)
        AuctionManager.getInstance().setOnAuctionClosedCallback(this::handleAuctionClosed);

        // Pre-load tất cả phiên đang active + bid history vào in-memory
        preloadActiveAuctions();

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

    /**
     * Khi server khởi động: load tất cả phiên OPEN/RUNNING từ DB vào AuctionManager,
     * kèm toàn bộ bid history để các client vào sau vẫn thấy lịch sử đầy đủ.
     */
    private void preloadActiveAuctions() {
        AuctionDAO auctionDAO       = new AuctionDAO();
        BidTransactionDAO bidDAO    = new BidTransactionDAO();
        try {
            List<Auction> actives = auctionDAO.findActive();
            for (Auction auction : actives) {
                List<BidTransaction> bids = bidDAO.findByAuction(auction.getId());
                for (BidTransaction tx : bids) {
                    auction.injectBid(tx);
                }
                AuctionManager.getInstance().registerAuction(auction);
            }
            System.out.println("[Server] Pre-load " + actives.size()
                    + " phiên active vào bộ nhớ.");
        } catch (Exception e) {
            System.err.println("[Server] Lỗi pre-load auctions: " + e.getMessage());
            e.printStackTrace();
        }
    }


    /* 1. Lưu trạng thái + winner vào DB
     * 2. Xử lý tài chính: charge winner, unfreeze losers
     * 3. Broadcast AUCTION_CLOSED tới tất cả clients
     */
    private void handleAuctionClosed(Auction auction) {
        UserDAO userDAO = new UserDAO();
        AuctionDAO auctionDAO = new AuctionDAO();

        try {
            // 1. Cập nhật DB auction (status=FINISHED, winner_id)
            auctionDAO.update(auction);
            System.out.printf("[Server] Phiên %s kết thúc. Winner: %s%n",
                    auction.getId(),
                    auction.getWinner() != null ? auction.getWinner().getUsername() : "không có");

            // 2. Xử lý tài chính cho từng bidder trong lịch sử bid
            //    Thu thập danh sách bidder duy nhất từ lịch sử
            java.util.Map<String, Bidder> bidders = new java.util.LinkedHashMap<>();
            for (BidTransaction tx : auction.getBids()) {
                bidders.put(tx.getBidder().getId(), tx.getBidder());
            }

            Bidder winner = auction.getWinner();
            for (Bidder bidder : bidders.values()) {
                if (winner != null && bidder.getId().equals(winner.getId())) {
                    // Winner: trừ thật balance = winAmount, giải phóng frozen
                    bidder.chargeWin(auction.getId());
                    userDAO.updateBalanceAndFrozen(bidder.getId(),
                            bidder.getBalance(), bidder.getFrozenBalance());
                    System.out.printf("[Server] Charge winner %s: -%.0f đ%n",
                            bidder.getUsername(), auction.getCurrentPrice());
                } else {
                    // Người thua: giải phóng frozen, không trừ balance
                    bidder.unfreezeForAuction(auction.getId());
                    userDAO.updateFrozenBalance(bidder.getId(), bidder.getFrozenBalance());
                    System.out.printf("[Server] Unfreeze loser %s%n", bidder.getUsername());
                }
            }

        } catch (Exception e) {
            System.err.println("[Server] Lỗi xử lý kết thúc phiên: " + e.getMessage());
            e.printStackTrace();
        }

        // 3. Broadcast AUCTION_CLOSED tới tất cả client
        broadcast(new Message("AUCTION_CLOSED", auction), null);
        System.out.println("[Server] Đã broadcast AUCTION_CLOSED: " + auction.getId());

        // 4. Gửi thông báo riêng cho Seller và Winner (nếu có)
        try {
            // Seller notification
            User seller = auction.getSeller();
            if (seller != null) {
                for (ClientHandler c : connectedClients) {
                    User cu = c.getCurrentUser();
                    if (cu != null && cu instanceof Seller && cu.getId().equals(seller.getId())) {
                        try {
                            c.sendMessage(new Message("AUCTION_SOLD", auction));
                        } catch (IOException e) {
                            System.err.println("[Notify] Không gửi được AUCTION_SOLD tới seller: " + e.getMessage());
                        }
                        break;
                    }
                }
            }

            // Winner notification
            Bidder winner = auction.getWinner();
            if (winner != null) {
                for (ClientHandler c : connectedClients) {
                    User cu = c.getCurrentUser();
                    if (cu != null && cu instanceof Bidder && cu.getId().equals(winner.getId())) {
                        try {
                            c.sendMessage(new Message("AUCTION_WON", auction));
                        } catch (IOException e) {
                            System.err.println("[Notify] Không gửi được AUCTION_WON tới winner: " + e.getMessage());
                        }
                        break;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[Notify] Lỗi khi gửi thông báo riêng: " + e.getMessage());
        }
    }

    /** Broadcast 1 message tới tất cả client đang kết nối (sender=null → gửi tất cả) */
    private static void broadcast(Message msg, ClientHandler sender) {
        for (ClientHandler c : connectedClients) {
            if (sender != null && c == sender) continue;
            try {
                c.sendMessage(msg);
            } catch (IOException e) {
                System.err.println("[Broadcast] Lỗi gửi tới client: " + e.getMessage());
            }
        }
    }

    /** Broadcast chỉ tới các client đang đăng nhập với role Admin */
    private static void broadcastToAdmins(Message msg) {
        for (ClientHandler c : connectedClients) {
            if (c.currentUser instanceof Admin) {
                try {
                    c.sendMessage(msg);
                } catch (IOException e) {
                    System.err.println("[BroadcastAdmin] Loi gui toi admin: " + e.getMessage());
                }
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

        public User getCurrentUser() { return currentUser; }

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
                                // [FIX] Nhận durationSeconds từ client, tính endTime theo đồng hồ server
                                long durationSeconds = ((Number) payload.get("durationSeconds")).longValue();
                                java.time.LocalDateTime startD = java.time.LocalDateTime.now();
                                java.time.LocalDateTime endD   = startD.plusSeconds(durationSeconds);

                                String itemId = UUID.randomUUID().toString();
                                // [FIX] Chuẩn hoá category sang enum string để DB nhất quán
                                String categoryEnum = switch (category) {
                                    case "Nghệ thuật", "Nghệ thuật & Cổ vật" -> "ART";
                                    case "Xe cộ", "Xe cội" -> "VEHICLE";
                                    default -> "ELECTRONICS";
                                };
                                Item item = switch (categoryEnum) {
                                    case "ART"     -> new Art(itemId, name, desc, price, seller, "Unknown", 2024);
                                    case "VEHICLE" -> new Vehicle(itemId, name, desc, price, seller, "Unknown", 0);
                                    default        -> new Electronics(itemId, name, desc, price, seller, 12);
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
                                        startD, endD);
                                // Tự động duyệt ngay khi seller tạo phiên, không cần admin duyệt
                                auction.setStatus(com.auction.shared.model.AuctionStatus.OPEN);

                                auctionDAO.save(auction);
                                // Register vào AuctionManager ngay để đếm giờ và hiển thị cho buyer
                                AuctionManager.getInstance().registerAuction(auction);

                                out.writeObject(new Message("CREATE_AUCTION_SUCCESS", auction));
                                System.out.println("[Server] CREATE_AUCTION OK (auto-approved): " + auctionId);
                                // Broadcast tới tất cả client để cập nhật danh sách phiên
                                broadcast(new Message("APPROVE_AUCTION_SUCCESS", auctionId), this);

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
                                // Inject bid history vào mỗi auction:
                                // Ưu tiên in-memory (có bids realtime), fallback load từ DB
                                for (Auction auction : auctions) {
                                    Auction inMem = AuctionManager.getInstance()
                                            .getAuction(auction.getId());
                                    if (inMem != null && inMem.getBids() != null
                                            && !inMem.getBids().isEmpty()) {
                                        // Dùng bids từ in-memory (đầy đủ, realtime)
                                        for (BidTransaction tx : inMem.getBids()) {
                                            auction.injectBid(tx);
                                        }
                                        // Đồng bộ leading bidder
                                        auction.setCurrentPriceOnly(inMem.getCurrentPrice());
                                    } else {
                                        // Load từ DB nếu chưa có in-memory
                                        List<BidTransaction> bids =
                                                bidTransactionDAO.findByAuction(auction.getId());
                                        for (BidTransaction tx : bids) {
                                            auction.injectBid(tx);
                                        }
                                    }
                                }
                                out.writeObject(new Message("GET_AUCTIONS_SUCCESS",
                                        (java.io.Serializable) auctions));
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
                            Message bidResult = null;
                            Message outbidMsg = null;
                            Message newBidMsg = null;
                            try {
                                @SuppressWarnings("unchecked")
                                HashMap<String, Object> p = (HashMap<String, Object>) request.getPayload();
                                String auctionId = (String) p.get("auctionId");
                                double amount    = (double) p.get("amount");

                                Bidder bidder = (Bidder) currentUser;

                                // Lấy phiên (load DB nếu chưa có trong memory)
                                Auction auction = AuctionManager.getInstance().getAuction(auctionId);
                                if (auction == null) {
                                    auction = auctionDAO.findById(auctionId)
                                            .orElseThrow(() -> new IllegalArgumentException(
                                                    "Không tìm thấy phiên đấu giá: " + auctionId));
                                    List<BidTransaction> existingBids =
                                            bidTransactionDAO.findByAuction(auctionId);
                                    for (BidTransaction tx : existingBids) auction.injectBid(tx);
                                    AuctionManager.getInstance().registerAuction(auction);
                                }

                                // ── CRITICAL SECTION ──────────────────────────
                                // Đồng bộ trên object auction để tránh race condition:
                                // Hai thread không thể vào cùng lúc → loại bỏ lost update
                                // và duplicate BidTransaction khi nhiều bidder đặt gần đồng thời.
                                BidTransaction tx;
                                Bidder previousLeader;
                                synchronized (auction) {
                                    // Kiểm tra lại trạng thái bên trong lock
                                    if (auction.getStatus() != com.auction.shared.model.AuctionStatus.OPEN
                                            && auction.getStatus() != com.auction.shared.model.AuctionStatus.RUNNING) {
                                        bidResult = new Message("PLACE_BID_FAILED", "Phiên đấu giá đã đóng!");
                                        break;
                                    }

                                    // Kiểm tra giá hợp lệ (bên trong lock — đảm bảo currentPrice không đổi giữa chừng)
                                    if (amount <= auction.getCurrentPrice()) {
                                        bidResult = new Message("PLACE_BID_FAILED",
                                                String.format("Giá phải cao hơn %,.0f đ!", auction.getCurrentPrice()));
                                        break;
                                    }

                                    // Kiểm tra số dư khả dụng
                                    double oldFrozen   = bidder.getFrozenForAuction(auctionId);
                                    double extraNeeded = amount - oldFrozen;
                                    if (bidder.getAvailableBalance() < extraNeeded) {
                                        bidResult = new Message("PLACE_BID_FAILED",
                                                String.format("Số dư không đủ! Cần thêm %,.0f đ (khả dụng: %,.0f đ)",
                                                        extraNeeded, bidder.getAvailableBalance()));
                                        break;
                                    }

                                    previousLeader = auction.getLeadingBidder();

                                    // Freeze tiền bidder mới
                                    bidder.freezeForAuction(auctionId, amount);
                                    userDAO.updateFrozenBalance(bidder.getId(), bidder.getFrozenBalance());

                                    // Unfreeze bidder bị vượt (nếu khác người)
                                    if (previousLeader != null
                                            && !previousLeader.getId().equals(bidder.getId())) {
                                        previousLeader.unfreezeForAuction(auctionId);
                                        userDAO.updateFrozenBalance(previousLeader.getId(),
                                                previousLeader.getFrozenBalance());
                                        System.out.printf("[Server] Unfreeze outbid: %s%n",
                                                previousLeader.getUsername());
                                    }

                                    // Cập nhật in-memory auction
                                    auction.placeBid(bidder, amount);

                                    // Lưu BidTransaction + cập nhật auction trong DB
                                    tx = new BidTransaction(bidder, amount, auctionId);
                                    bidTransactionDAO.save(tx);
                                    auctionDAO.update(auction);

                                    // ── Anti-sniping: nếu bid trong 60 giây cuối → gia hạn thêm 60 giây
                                    long secsLeft = java.time.temporal.ChronoUnit.SECONDS.between(
                                            java.time.LocalDateTime.now(), auction.getEndTime());
                                    if (secsLeft <= 60) {
                                        AuctionManager.getInstance().extendAuction(auctionId, 60);
                                        auctionDAO.updateEndTime(auctionId, auction.getEndTime());
                                        System.out.printf("[Anti-snipe] Phiên %s gia hạn thêm 60s (còn %ds)%n",
                                                auctionId, secsLeft);
                                        // Thông báo gia hạn tới tất cả client
                                        HashMap<String, Object> extPayload = new HashMap<>();
                                        extPayload.put("auctionId", auctionId);
                                        extPayload.put("newEndTime", auction.getEndTime().toString());
                                        broadcast(new Message("AUCTION_EXTENDED", extPayload), null);
                                    }
                                }
                                // ── END CRITICAL SECTION ──────────────────────

                                bidResult = new Message("PLACE_BID_SUCCESS", tx);

                                // Broadcast NEW_BID tới tất cả client
                                newBidMsg = new Message("NEW_BID", tx);

                                // Broadcast OUTBID_NOTIFY cho người bị vượt
                                if (previousLeader != null
                                        && !previousLeader.getId().equals(bidder.getId())) {
                                    HashMap<String, Object> outbidPayload = new HashMap<>();
                                    outbidPayload.put("auctionId", auctionId);
                                    outbidPayload.put("bidderId", previousLeader.getId());
                                    outbidPayload.put("newBalance", previousLeader.getBalance());
                                    outbidPayload.put("newFrozen", previousLeader.getFrozenBalance());
                                    outbidMsg = new Message("OUTBID_NOTIFY", outbidPayload);
                                }

                                System.out.printf("[Server] PLACE_BID OK: %s đặt %.0f vào %s%n",
                                        bidder.getUsername(), amount, auctionId);

                            } catch (IllegalArgumentException e) {
                                bidResult = new Message("PLACE_BID_FAILED", e.getMessage());
                            } catch (Exception e) {
                                bidResult = new Message("PLACE_BID_FAILED", "Lỗi server: " + e.getMessage());
                                e.printStackTrace();
                            }
                            // Gửi kết quả ra ngoài synchronized block để không giữ lock khi I/O
                            if (bidResult != null) { out.writeObject(bidResult); out.flush(); }
                            if (newBidMsg  != null) broadcast(newBidMsg, this);
                            if (outbidMsg  != null) broadcast(outbidMsg, null);
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
                                    // Inject bids vào mỗi auction (giống GET_AUCTIONS)
                                    for (Auction auction : list) {
                                        Auction inMem = AuctionManager.getInstance()
                                                .getAuction(auction.getId());
                                        if (inMem != null && inMem.getBids() != null
                                                && !inMem.getBids().isEmpty()) {
                                            for (BidTransaction tx : inMem.getBids()) {
                                                auction.injectBid(tx);
                                            }
                                            auction.setCurrentPriceOnly(inMem.getCurrentPrice());
                                        } else {
                                            List<BidTransaction> bids =
                                                    bidTransactionDAO.findByAuction(auction.getId());
                                            for (BidTransaction tx : bids) {
                                                auction.injectBid(tx);
                                            }
                                        }
                                    }
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

                        // ── HỦY PHIÊN ĐẤU GIÁ ───────────────────────────────
                        case "CANCEL_AUCTION" -> {
                            try {
                                String auctionId = (String) request.getPayload();
                                Auction auction = AuctionManager.getInstance().getAuction(auctionId);
                                if (auction == null) {
                                    auction = auctionDAO.findById(auctionId).orElse(null);
                                }
                                if (auction == null) {
                                    out.writeObject(new Message("CANCEL_AUCTION_FAILED",
                                            "Không tìm thấy phiên: " + auctionId));
                                    out.flush();
                                    break;
                                }
                                // Chỉ cho hủy nếu chưa có ai đặt giá
                                if (auction.getLeadingBidder() != null) {
                                    out.writeObject(new Message("CANCEL_AUCTION_FAILED",
                                            "Không thể hủy phiên đã có người đặt giá!"));
                                    out.flush();
                                    break;
                                }
                                // Chỉ seller sở hữu phiên mới được hủy
                                if (currentUser == null ||
                                        !auction.getSeller().getId().equals(currentUser.getId())) {
                                    out.writeObject(new Message("CANCEL_AUCTION_FAILED",
                                            "Bạn không có quyền hủy phiên này!"));
                                    out.flush();
                                    break;
                                }
                                auction.setStatus(AuctionStatus.CANCELLED);
                                auctionDAO.update(auction);
                                // Hủy lịch đóng tự động trong AuctionManager
                                AuctionManager.getInstance().closeAuction(auctionId);

                                out.writeObject(new Message("CANCEL_AUCTION_SUCCESS", auctionId));
                                System.out.println("[Server] CANCEL_AUCTION OK: " + auctionId);
                                // Broadcast để các client khác cập nhật UI
                                broadcast(new Message("AUCTION_CLOSED", auction), this);
                            } catch (Exception e) {
                                out.writeObject(new Message("CANCEL_AUCTION_FAILED",
                                        "Lỗi server: " + e.getMessage()));
                                e.printStackTrace();
                            }
                            out.flush();
                        }

                        // ── CẬP NHẬT PHIÊN ĐẤU GIÁ ──────────────────────────
                        case "UPDATE_AUCTION" -> {
                            try {
                                @SuppressWarnings("unchecked")
                                HashMap<String, Object> payload =
                                        (HashMap<String, Object>) request.getPayload();
                                String auctionId = (String) payload.get("auctionId");

                                Auction auction = AuctionManager.getInstance().getAuction(auctionId);
                                if (auction == null) {
                                    auction = auctionDAO.findById(auctionId).orElse(null);
                                }
                                if (auction == null) {
                                    out.writeObject(new Message("UPDATE_AUCTION_FAILED",
                                            "Không tìm thấy phiên: " + auctionId));
                                    out.flush();
                                    break;
                                }
                                // Chỉ seller sở hữu phiên mới được sửa
                                if (currentUser == null ||
                                        !auction.getSeller().getId().equals(currentUser.getId())) {
                                    out.writeObject(new Message("UPDATE_AUCTION_FAILED",
                                            "Bạn không có quyền sửa phiên này!"));
                                    out.flush();
                                    break;
                                }
                                // Chỉ cho sửa nếu chưa có ai đặt giá
                                if (auction.getLeadingBidder() != null) {
                                    out.writeObject(new Message("UPDATE_AUCTION_FAILED",
                                            "Không thể sửa phiên đã có người đặt giá!"));
                                    out.flush();
                                    break;
                                }

                                // Cập nhật thông tin item
                                String name     = (String) payload.get("name");
                                String desc     = (String) payload.get("description");
                                double price    = (double) payload.get("startPrice");
                                String category = (String) payload.get("category");
                                java.time.LocalDateTime newEnd = java.time.LocalDateTime.parse(
                                        (String) payload.get("endDateTime"));

                                // Nếu category thay đổi → tạo lại Item đúng subclass
                                Item oldItem = auction.getItem();
                                String newCategoryKey = switch (category != null ? category : "") {
                                    case "Xe cộ"              -> "VEHICLE";
                                    case "Nghệ thuật"         -> "ART";
                                    case "Điện tử"            -> "ELECTRONICS";
                                    default                   -> "ELECTRONICS";
                                };
                                Item updatedItem;
                                if (!newCategoryKey.equalsIgnoreCase(oldItem.getCategory())) {
                                    // Tạo lại Item mới cùng id, đúng subclass
                                    Seller itemSeller = oldItem.getSeller();
                                    updatedItem = switch (newCategoryKey) {
                                        case "VEHICLE"     -> new Vehicle(oldItem.getId(), name, desc, price, itemSeller, "Unknown", 0);
                                        case "ART"         -> new Art(oldItem.getId(), name, desc, price, itemSeller, "Unknown", 2024);
                                        default            -> new Electronics(oldItem.getId(), name, desc, price, itemSeller, 12);
                                    };
                                    updatedItem.setImageBase64(oldItem.getImageBase64());
                                    auction.setItem(updatedItem);
                                } else {
                                    oldItem.setName(name);
                                    oldItem.setDescription(desc);
                                    oldItem.setBasePrice(price);
                                    updatedItem = oldItem;
                                }

                                auction.setCurrentPriceOnly(price);
                                auction.setEndTime(newEnd);

                                // Cập nhật ảnh nếu có ảnh mới
                                String imageBase64 = (String) payload.get("imageBase64");
                                if (imageBase64 != null && !imageBase64.isEmpty()) {
                                    updatedItem.setImageBase64(imageBase64);
                                }

                                // Lưu DB
                                itemDAO.update(updatedItem);
                                auctionDAO.update(auction);

                                // Cập nhật lịch đóng tự động (endTime mới)
                                AuctionManager.getInstance().extendAuction(auctionId, 0);

                                out.writeObject(new Message("UPDATE_AUCTION_SUCCESS", auction));
                                System.out.println("[Server] UPDATE_AUCTION OK: " + auctionId);
                            } catch (Exception e) {
                                out.writeObject(new Message("UPDATE_AUCTION_FAILED",
                                        "Lỗi server: " + e.getMessage()));
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

                        // ── ADMIN: LẤY DANH SÁCH NGƯỜI DÙNG ─────────────────
                        case "GET_ALL_USERS" -> {
                            if (currentUser == null || !(currentUser instanceof Admin)) {
                                out.writeObject(new Message("GET_ALL_USERS_SUCCESS", new ArrayList<>()));
                                out.flush();
                                break;
                            }
                            try {
                                List<User> allUsers = userDAO.findAll();
                                out.writeObject(new Message("GET_ALL_USERS_SUCCESS",
                                        (java.io.Serializable) allUsers));
                                System.out.println("[Server] GET_ALL_USERS OK: " + allUsers.size() + " users");
                            } catch (Exception e) {
                                out.writeObject(new Message("GET_ALL_USERS_SUCCESS", new ArrayList<>()));
                                e.printStackTrace();
                            }
                            out.flush();
                        }

                        // ── ADMIN: LẤY DANH SÁCH PHIÊN ──────────────────────
                        case "GET_ALL_AUCTIONS" -> {
                            if (currentUser == null || !(currentUser instanceof Admin)) {
                                out.writeObject(new Message("GET_ALL_AUCTIONS_SUCCESS", new ArrayList<>()));
                                out.flush();
                                break;
                            }
                            try {
                                List<Auction> allAuctions = auctionDAO.findAll();
                                // Inject bids từ DB và in-memory
                                for (Auction auction : allAuctions) {
                                    Auction inMem = AuctionManager.getInstance()
                                            .getAuction(auction.getId());
                                    if (inMem != null && inMem.getBids() != null
                                            && !inMem.getBids().isEmpty()) {
                                        for (BidTransaction tx : inMem.getBids()) {
                                            auction.injectBid(tx);
                                        }
                                        auction.setCurrentPriceOnly(inMem.getCurrentPrice());
                                    } else {
                                        List<BidTransaction> bids =
                                                bidTransactionDAO.findByAuction(auction.getId());
                                        for (BidTransaction tx : bids) {
                                            auction.injectBid(tx);
                                        }
                                    }
                                }
                                out.writeObject(new Message("GET_ALL_AUCTIONS_SUCCESS",
                                        (java.io.Serializable) allAuctions));
                                System.out.println("[Server] GET_ALL_AUCTIONS OK: " + allAuctions.size() + " phiên");
                            } catch (Exception e) {
                                out.writeObject(new Message("GET_ALL_AUCTIONS_SUCCESS", new ArrayList<>()));
                                e.printStackTrace();
                            }
                            out.flush();
                        }

                        // ── ADMIN: DỰA PHIÊN ─────────────────────────────────
                        case "APPROVE_AUCTION" -> {
                            if (currentUser == null || !(currentUser instanceof Admin)) {
                                out.writeObject(new Message("APPROVE_AUCTION_SUCCESS", null));
                                out.flush();
                                break;
                            }
                            try {
                                String auctionId = (String) request.getPayload();
                                Auction auction = AuctionManager.getInstance().getAuction(auctionId);
                                if (auction == null) {
                                    auction = auctionDAO.findById(auctionId).orElse(null);
                                }
                                if (auction == null) {
                                    out.writeObject(new Message("APPROVE_AUCTION_SUCCESS", null));
                                    out.flush();
                                    break;
                                }

                                auction.setStatus(AuctionStatus.OPEN);
                                auctionDAO.update(auction);
                                AuctionManager.getInstance().registerAuction(auction);

                                out.writeObject(new Message("APPROVE_AUCTION_SUCCESS", auctionId));
                                System.out.println("[Server] APPROVE_AUCTION OK: " + auctionId);

                                // Broadcast để seller và clients khác cập nhật UI
                                broadcast(new Message("APPROVE_AUCTION_SUCCESS", auctionId), this);
                            } catch (Exception e) {
                                out.writeObject(new Message("APPROVE_AUCTION_SUCCESS", null));
                                e.printStackTrace();
                            }
                            out.flush();
                        }

                        // ── ADMIN: TỪ CHốI PHIÊN ─────────────────────────────
                        case "REJECT_AUCTION" -> {
                            if (currentUser == null || !(currentUser instanceof Admin)) {
                                out.writeObject(new Message("REJECT_AUCTION_SUCCESS", null));
                                out.flush();
                                break;
                            }
                            try {
                                String auctionId = (String) request.getPayload();
                                Auction auction = AuctionManager.getInstance().getAuction(auctionId);
                                if (auction == null) {
                                    auction = auctionDAO.findById(auctionId).orElse(null);
                                }
                                if (auction == null) {
                                    out.writeObject(new Message("REJECT_AUCTION_SUCCESS", null));
                                    out.flush();
                                    break;
                                }


                                auction.setStatus(AuctionStatus.CANCELLED);
                                auctionDAO.update(auction);

                                out.writeObject(new Message("REJECT_AUCTION_SUCCESS", auctionId));
                                System.out.println("[Server] REJECT_AUCTION OK: " + auctionId);

                                // Broadcast để seller cập nhật UI
                                broadcast(new Message("REJECT_AUCTION_SUCCESS", auctionId), this);
                            } catch (Exception e) {
                                out.writeObject(new Message("REJECT_AUCTION_SUCCESS", null));
                                e.printStackTrace();
                            }
                            out.flush();
                        }

                        // ── ADMIN: KHOÁ/MỞ KHOÁ NGƯỜI DÙNG ──────────────────
                        case "BAN_USER" -> {
                            if (currentUser == null || !(currentUser instanceof Admin)) {
                                out.writeObject(new Message("BAN_USER_SUCCESS", null));
                                out.flush();
                                break;
                            }
                            try {
                                String userId = (String) request.getPayload();
                                User user = userDAO.findById(userId).orElse(null);
                                if (user == null) {
                                    out.writeObject(new Message("BAN_USER_SUCCESS", null));
                                    out.flush();
                                    break;
                                }

                                // Toggle active status
                                boolean newStatus = !user.isActive();
                                userDAO.updateActive(userId, newStatus);
                                user.setActive(newStatus);

                                out.writeObject(new Message("BAN_USER_SUCCESS", userId));
                                System.out.println("[Server] BAN_USER OK: " + user.getUsername() +
                                        " | Active: " + newStatus);

                                // Broadcast để admin khác cập nhật UI
                                broadcast(new Message("BAN_USER_SUCCESS", userId), this);
                            } catch (Exception e) {
                                out.writeObject(new Message("BAN_USER_SUCCESS", null));
                                e.printStackTrace();
                            }
                            out.flush();
                        }

                        // ── ADMIN: ĐÓNG PHIÊN ĐANG HOẠT ĐỘNG ────────────────
                        case "ADMIN_CLOSE_AUCTION" -> {
                            if (currentUser == null || !(currentUser instanceof Admin)) {
                                out.writeObject(new Message("ADMIN_CLOSE_AUCTION_SUCCESS", null));
                                out.flush();
                                break;
                            }
                            try {
                                String auctionId = (String) request.getPayload();
                                Auction auction = AuctionManager.getInstance().getAuction(auctionId);
                                if (auction == null) {
                                    auction = auctionDAO.findById(auctionId).orElse(null);
                                }
                                if (auction == null) {
                                    out.writeObject(new Message("ADMIN_CLOSE_AUCTION_SUCCESS", null));
                                    out.flush();
                                    break;
                                }

                                // Chỉ đóng phiên đang OPEN hoặc RUNNING
                                if (auction.getStatus() != AuctionStatus.OPEN
                                        && auction.getStatus() != AuctionStatus.RUNNING) {
                                    out.writeObject(new Message("ADMIN_CLOSE_AUCTION_SUCCESS", null));
                                    out.flush();
                                    break;
                                }

                                // Đặt trạng thái FINISHED và lưu DB
                                auction.setStatus(AuctionStatus.FINISHED);
                                auctionDAO.update(auction);
                                // Huỷ lịch đóng tự động
                                AuctionManager.getInstance().closeAuction(auctionId);

                                out.writeObject(new Message("ADMIN_CLOSE_AUCTION_SUCCESS", auctionId));
                                System.out.println("[Server] ADMIN_CLOSE_AUCTION OK: " + auctionId);

                                // Broadcast để tất cả clients biết phiên đã đóng
                                broadcast(new Message("AUCTION_CLOSED", auction), this);
                                broadcast(new Message("ADMIN_CLOSE_AUCTION_SUCCESS", auctionId), this);
                            } catch (Exception e) {
                                out.writeObject(new Message("ADMIN_CLOSE_AUCTION_SUCCESS", null));
                                e.printStackTrace();
                            }
                            out.flush();
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