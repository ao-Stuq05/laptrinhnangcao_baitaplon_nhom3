package com.auction.client.network;

import javafx.application.Platform;
import com.auction.client.SceneManager;
import com.auction.shared.model.*;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.function.Consumer;

public class ServerConnection {
    private static ServerConnection instance;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream  in;
    private boolean isRunning = false;
    private User currentUser;

    // ── Callbacks ─────────────────────────────────────────────────────────────
    private Consumer<List<Auction>>        auctionListCallback;
    private Consumer<BidTransaction>       bidUpdateCallback;
    private Consumer<Double>               topUpCallback;
    private Consumer<List<Auction>>        myAuctionCallback;
    private Consumer<List<BidTransaction>> myBidCallback;
    private Consumer<User>                 profileUpdateCallback;
    private Consumer<Auction>              auctionClosedCallback;
    /** Callback khi bị người khác vượt giá — nhận auctionId để ProductController refresh balance */
    private Consumer<String>               outbidCallback;

    private ServerConnection() {}
    public static ServerConnection getInstance() {
        if (instance == null) instance = new ServerConnection();
        return instance;
    }

    public User getCurrentUser() { return currentUser; }

    public void setAuctionListCallback   (Consumer<List<Auction>> cb)        { auctionListCallback    = cb; }
    public void setBidUpdateCallback     (Consumer<BidTransaction> cb)        { bidUpdateCallback      = cb; }
    public void setTopUpCallback         (Consumer<Double> cb)                { topUpCallback          = cb; }
    public void setMyAuctionCallback     (Consumer<List<Auction>> cb)         { myAuctionCallback      = cb; }
    public void setMyBidCallback         (Consumer<List<BidTransaction>> cb)  { myBidCallback          = cb; }
    public void setProfileUpdateCallback (Consumer<User> cb)                  { profileUpdateCallback  = cb; }
    public void setAuctionClosedCallback (Consumer<Auction> cb)               { auctionClosedCallback  = cb; }
    public void setOutbidCallback        (Consumer<String> cb)                { outbidCallback         = cb; }

    public void connect(String host, int port) throws IOException {
        if (socket == null || socket.isClosed()) {
            socket = new Socket(host, port);
            out    = new ObjectOutputStream(socket.getOutputStream());
            in     = new ObjectInputStream(socket.getInputStream());
            System.out.println(">>> Đã kết nối Server!");
            startListening();
        }
    }

    private void startListening() {
        isRunning = true;
        Thread t = new Thread(() -> {
            try {
                while (isRunning) {
                    Message msg = (Message) in.readObject();
                    handleServerResponse(msg);
                }
            } catch (Exception e) {
                System.out.println("Mất kết nối Server: " + e.getMessage());
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private void handleServerResponse(Message msg) {
        System.out.println("<<< Server: " + msg.getType());
        switch (msg.getType()) {

            case "LOGIN_SUCCESS" -> {
                currentUser = (User) msg.getPayload();
                Platform.runLater(() -> SceneManager.switchScene("UI.fxml"));
            }
            case "LOGIN_FAILED" ->
                    Platform.runLater(() -> alert("Đăng nhập thất bại",
                            (String) msg.getPayload(), javafx.scene.control.Alert.AlertType.ERROR));

            case "REGISTER_SUCCESS" -> {
                User saved = (User) msg.getPayload();
                Platform.runLater(() -> {
                    alert("Đăng ký thành công",
                            "Tài khoản '" + saved.getUsername() + "' đã được tạo!",
                            javafx.scene.control.Alert.AlertType.INFORMATION);
                    SceneManager.switchScene("login.fxml");
                });
            }
            case "REGISTER_FAILED" ->
                    Platform.runLater(() -> alert("Đăng ký thất bại",
                            (String) msg.getPayload(), javafx.scene.control.Alert.AlertType.ERROR));

            case "CREATE_AUCTION_SUCCESS" -> {
                Auction a = (Auction) msg.getPayload();
                Platform.runLater(() -> {
                    alert("Đăng bán thành công",
                            "\"" + a.getItem().getName() + "\" đã được đăng!",
                            javafx.scene.control.Alert.AlertType.INFORMATION);
                    SceneManager.switchScene("UI.fxml");
                });
            }
            case "CREATE_AUCTION_FAILED" ->
                    Platform.runLater(() -> alert("Đăng bán thất bại",
                            (String) msg.getPayload(), javafx.scene.control.Alert.AlertType.ERROR));

            case "UPDATE_AUCTION_SUCCESS" -> {
                Auction a = (Auction) msg.getPayload();
                Platform.runLater(() -> {
                    alert("Cập nhật thành công",
                            "\"" + a.getItem().getName() + "\" đã được cập nhật!",
                            javafx.scene.control.Alert.AlertType.INFORMATION);
                    SceneManager.switchScene("MyAuctions.fxml");
                });
            }
            case "UPDATE_AUCTION_FAILED" ->
                    Platform.runLater(() -> alert("Cập nhật thất bại",
                            (String) msg.getPayload(), javafx.scene.control.Alert.AlertType.ERROR));

            case "CANCEL_AUCTION_SUCCESS" ->
                    Platform.runLater(() -> alert("Hủy phiên thành công",
                            "Phiên đấu giá đã được hủy.",
                            javafx.scene.control.Alert.AlertType.INFORMATION));

            case "CANCEL_AUCTION_FAILED" ->
                    Platform.runLater(() -> alert("Hủy phiên thất bại",
                            (String) msg.getPayload(), javafx.scene.control.Alert.AlertType.ERROR));

            case "GET_AUCTIONS_SUCCESS" -> {
                @SuppressWarnings("unchecked") List<Auction> list = (List<Auction>) msg.getPayload();
                if (auctionListCallback != null) auctionListCallback.accept(list);
            }

            // ── Bid realtime ─────────────────────────────────────────────────
            case "PLACE_BID_SUCCESS", "NEW_BID" -> {
                BidTransaction tx = (BidTransaction) msg.getPayload();
                // Cập nhật frozen balance phía client khi là bid của mình
                if ("PLACE_BID_SUCCESS".equals(msg.getType())
                        && currentUser instanceof Bidder bidder
                        && bidder.getId().equals(tx.getBidder().getId())) {
                    bidder.freezeForAuction(tx.getAuctionId(), tx.getBidAmount());
                }
                if (bidUpdateCallback != null) bidUpdateCallback.accept(tx);
            }
            case "PLACE_BID_FAILED" ->
                    Platform.runLater(() -> alert("Đặt giá thất bại",
                            (String) msg.getPayload(), javafx.scene.control.Alert.AlertType.ERROR));

            // ── Bị vượt giá: giải phóng frozen balance ───────────────────────
            case "OUTBID_NOTIFY" -> {
                @SuppressWarnings("unchecked")
                java.util.HashMap<String, Object> p =
                        (java.util.HashMap<String, Object>) msg.getPayload();
                String bidderId  = (String) p.get("bidderId");
                double newBalance = (double) p.get("newBalance");
                double newFrozen  = (double) p.get("newFrozen");
                if (currentUser instanceof Bidder bidder
                        && bidder.getId().equals(bidderId)) {
                    bidder.setBalance(newBalance);
                    bidder.setFrozenBalance(newFrozen);
                    // Xóa frozen entry cho phiên này trong map nội bộ
                    String auctionId = (String) p.get("auctionId");
                    bidder.unfreezeForAuction(auctionId);
                    if (bidUpdateCallback != null) {
                        // Tái dùng callback để ProductController cập nhật UI số dư
                        // (tx=null được xử lý an toàn — chỉ cần trigger refresh balance)
                    }
                    Platform.runLater(() -> {
                        if (outbidCallback != null) outbidCallback.accept(auctionId);
                    });
                }
            }

            // ── Nạp tiền ─────────────────────────────────────────────────────
            case "TOP_UP_SUCCESS" -> {
                Double newBal = (Double) msg.getPayload();
                if (currentUser instanceof Bidder b) b.setBalance(newBal);
                if (topUpCallback != null) Platform.runLater(() -> topUpCallback.accept(newBal));
            }
            case "TOP_UP_FAILED" ->
                    Platform.runLater(() -> alert("Nạp tiền thất bại",
                            (String) msg.getPayload(), javafx.scene.control.Alert.AlertType.ERROR));

            // ── Lịch sử ──────────────────────────────────────────────────────
            case "GET_MY_AUCTIONS_SUCCESS" -> {
                @SuppressWarnings("unchecked") List<Auction> list = (List<Auction>) msg.getPayload();
                if (myAuctionCallback != null) myAuctionCallback.accept(list);
            }
            case "GET_MY_BIDS_SUCCESS" -> {
                @SuppressWarnings("unchecked") List<BidTransaction> list = (List<BidTransaction>) msg.getPayload();
                if (myBidCallback != null) myBidCallback.accept(list);
            }

            // ── Hồ sơ ────────────────────────────────────────────────────────
            case "UPDATE_PROFILE_SUCCESS" -> {
                currentUser = (User) msg.getPayload();
                if (profileUpdateCallback != null)
                    Platform.runLater(() -> profileUpdateCallback.accept(currentUser));
            }
            case "UPDATE_PROFILE_FAILED" ->
                    Platform.runLater(() -> alert("Cập nhật thất bại",
                            (String) msg.getPayload(), javafx.scene.control.Alert.AlertType.ERROR));

            // ── Phiên kết thúc (từ AuctionManager broadcast) ─────────────────
            case "AUCTION_CLOSED" -> {
                Auction closed = (Auction) msg.getPayload();

                // Cập nhật lại balance / frozen cho bidder hiện tại
                if (currentUser instanceof Bidder bidder) {
                    // Server đã xử lý: nếu thắng → balance giảm, nếu thua → không đổi
                    // Client: giải phóng frozen cho phiên này
                    bidder.unfreezeForAuction(closed.getId());
                    if (closed.getWinner() != null
                            && closed.getWinner().getId().equals(bidder.getId())) {
                        // Thắng: trừ balance
                        double newBal = bidder.getBalance() - closed.getCurrentPrice();
                        bidder.setBalance(Math.max(0, newBal));
                    }
                }

                if (auctionClosedCallback != null)
                    auctionClosedCallback.accept(closed);
            }

            // ── Thông báo dành riêng: bạn thắng phiên ────────────────────────
            case "AUCTION_WON" -> {
                Auction a = (Auction) msg.getPayload();
                Platform.runLater(() -> {
                    alert("Bạn đã thắng!",
                            "Bạn đã thắng phiên đấu giá: " + a.getItem().getName() +
                                    " với mức giá " + String.format("%.0f đ", a.getCurrentPrice()),
                            javafx.scene.control.Alert.AlertType.INFORMATION);
                    // Callback để cập nhật UI nếu cần
                    if (auctionClosedCallback != null) auctionClosedCallback.accept(a);
                });
            }

            // ── Thông báo dành riêng: seller được thông báo sản phẩm đã bán
            case "AUCTION_SOLD" -> {
                Auction a = (Auction) msg.getPayload();
                Platform.runLater(() -> {
                    alert("Sản phẩm đã bán",
                            "Phiên đấu giá của bạn đã kết thúc. " +
                                    (a.getWinner() != null ? "Người thắng: " + a.getWinner().getUsername() : "Không có người thắng"),
                            javafx.scene.control.Alert.AlertType.INFORMATION);
                    // Nếu seller đang xem trang quản lý, refresh danh sách
                    if (myAuctionCallback != null) {
                        try {
                            // Yêu cầu server lấy lại danh sách my auctions
                            // Client có thể gọi API GET_MY_AUCTIONS khi cần; gửi request
                            sendMessage(new Message("GET_MY_AUCTIONS", null));
                        } catch (Exception ignored) {}
                    }
                });
            }

            default -> System.out.println("[Client] Không xử lý: " + msg.getType());
        }
    }

    public void sendMessage(Message msg) throws IOException {
        if (out != null) { out.writeObject(msg); out.flush(); }
    }

    public void close() throws IOException {
        isRunning = false;
        if (socket != null) socket.close();
    }

    private void alert(String header, String content,
                       javafx.scene.control.Alert.AlertType type) {
        javafx.scene.control.Alert a = new javafx.scene.control.Alert(type, content);
        a.setHeaderText(header);
        a.showAndWait();
    }
}