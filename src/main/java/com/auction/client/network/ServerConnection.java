package com.auction.client.network;

import javafx.application.Platform;
import com.auction.client.SceneManager;
import com.auction.shared.model.Auction;
import com.auction.shared.model.BidTransaction;
import com.auction.shared.model.Message;
import com.auction.shared.model.User;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.function.Consumer;

public class ServerConnection {
    private static ServerConnection instance;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private boolean isRunning = false;

    private User currentUser;

    // Callbacks
    private Consumer<List<Auction>>        auctionListCallback;
    private Consumer<BidTransaction>       bidUpdateCallback;
    private Consumer<Double>               topUpCallback;
    private Consumer<List<Auction>>        myAuctionCallback;
    private Consumer<List<BidTransaction>> myBidCallback;

    private ServerConnection() {}

    public static ServerConnection getInstance() {
        if (instance == null) instance = new ServerConnection();
        return instance;
    }

    public User getCurrentUser() { return currentUser; }

    public void setAuctionListCallback(Consumer<List<Auction>> cb)         { this.auctionListCallback = cb; }
    public void setBidUpdateCallback(Consumer<BidTransaction> cb)          { this.bidUpdateCallback   = cb; }
    public void setTopUpCallback(Consumer<Double> cb)                      { this.topUpCallback       = cb; }
    public void setMyAuctionCallback(Consumer<List<Auction>> cb)           { this.myAuctionCallback   = cb; }
    public void setMyBidCallback(Consumer<List<BidTransaction>> cb)        { this.myBidCallback       = cb; }

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
        Thread t = new Thread(() -> {
            try {
                while (isRunning) {
                    Message response = (Message) in.readObject();
                    handleServerResponse(response);
                }
            } catch (Exception e) {
                System.out.println("Lỗi luồng lắng nghe hoặc Server đã ngắt kết nối.");
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private void handleServerResponse(Message msg) {
        System.out.println("<<< Nhận từ Server: " + msg.getType());

        switch (msg.getType()) {

            case "LOGIN_SUCCESS" -> {
                currentUser = (User) msg.getPayload();
                Platform.runLater(() -> SceneManager.switchScene("UI.fxml"));
            }

            case "LOGIN_FAILED" -> {
                String reason = (String) msg.getPayload();
                Platform.runLater(() -> showAlert("Đăng nhập thất bại",
                        reason != null ? reason : "Sai tài khoản hoặc mật khẩu!",
                        javafx.scene.control.Alert.AlertType.ERROR));
            }

            case "REGISTER_SUCCESS" -> {
                User saved = (User) msg.getPayload();
                Platform.runLater(() -> {
                    showAlert("Đăng ký thành công",
                            "Tài khoản '" + saved.getUsername() + "' đã được tạo!",
                            javafx.scene.control.Alert.AlertType.INFORMATION);
                    SceneManager.switchScene("login.fxml");
                });
            }

            case "REGISTER_FAILED" -> {
                String reason = (String) msg.getPayload();
                Platform.runLater(() -> showAlert("Đăng ký thất bại",
                        reason != null ? reason : "Vui lòng thử lại!",
                        javafx.scene.control.Alert.AlertType.ERROR));
            }

            case "CREATE_AUCTION_SUCCESS" -> {
                Auction a = (Auction) msg.getPayload();
                Platform.runLater(() -> {
                    showAlert("Đăng bán thành công",
                            "Sản phẩm \"" + a.getItem().getName() + "\" đã được đăng!",
                            javafx.scene.control.Alert.AlertType.INFORMATION);
                    SceneManager.switchScene("UI.fxml");
                });
            }

            case "CREATE_AUCTION_FAILED" -> {
                String reason = (String) msg.getPayload();
                Platform.runLater(() -> showAlert("Đăng bán thất bại",
                        reason != null ? reason : "Vui lòng thử lại!",
                        javafx.scene.control.Alert.AlertType.ERROR));
            }

            case "GET_AUCTIONS_SUCCESS" -> {
                @SuppressWarnings("unchecked")
                List<Auction> auctions = (List<Auction>) msg.getPayload();
                if (auctionListCallback != null) auctionListCallback.accept(auctions);
            }

            case "GET_AUCTIONS_FAILED" ->
                    System.out.println("Lấy danh sách thất bại: " + msg.getPayload());

            // Nhận bid mới realtime
            case "NEW_BID" -> {
                BidTransaction tx = (BidTransaction) msg.getPayload();
                if (bidUpdateCallback != null) bidUpdateCallback.accept(tx);
            }

            // Kết quả đặt giá của chính mình
            case "PLACE_BID_SUCCESS" -> {
                BidTransaction tx = (BidTransaction) msg.getPayload();
                System.out.println("Đặt giá thành công: " + tx.getBidAmount());
                if (bidUpdateCallback != null) bidUpdateCallback.accept(tx);
            }

            case "PLACE_BID_FAILED" -> {
                String reason = (String) msg.getPayload();
                Platform.runLater(() -> showAlert("Đặt giá thất bại",
                        reason != null ? reason : "Vui lòng thử lại!",
                        javafx.scene.control.Alert.AlertType.ERROR));
            }

            // ── MỚI: NẠP TIỀN ────────────────────────────────────────────
            case "TOP_UP_SUCCESS" -> {
                Double newBalance = (Double) msg.getPayload();
                if (topUpCallback != null)
                    Platform.runLater(() -> topUpCallback.accept(newBalance));
            }

            case "TOP_UP_FAILED" -> {
                String reason = (String) msg.getPayload();
                Platform.runLater(() -> showAlert("Nạp tiền thất bại",
                        reason != null ? reason : "Vui lòng thử lại!",
                        javafx.scene.control.Alert.AlertType.ERROR));
            }

            // ── MỚI: LỊCH SỬ ĐẤU GIÁ CỦA TÔI ───────────────────────────
            case "GET_MY_AUCTIONS_SUCCESS" -> {
                @SuppressWarnings("unchecked")
                List<Auction> auctions = (List<Auction>) msg.getPayload();
                if (myAuctionCallback != null) myAuctionCallback.accept(auctions);
            }

            case "GET_MY_BIDS_SUCCESS" -> {
                @SuppressWarnings("unchecked")
                List<BidTransaction> bids = (List<BidTransaction>) msg.getPayload();
                if (myBidCallback != null) myBidCallback.accept(bids);
            }

            // ── MỚI: CẬP NHẬT THÔNG TIN CÁ NHÂN ────────────────────────
            case "UPDATE_PROFILE_SUCCESS" -> {
                User updated = (User) msg.getPayload();
                currentUser = updated;
                Platform.runLater(() -> showAlert("Cập nhật thành công",
                        "Thông tin cá nhân đã được lưu!",
                        javafx.scene.control.Alert.AlertType.INFORMATION));
            }

            case "UPDATE_PROFILE_FAILED" -> {
                String reason = (String) msg.getPayload();
                Platform.runLater(() -> showAlert("Cập nhật thất bại",
                        reason != null ? reason : "Vui lòng thử lại!",
                        javafx.scene.control.Alert.AlertType.ERROR));
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

    private void showAlert(String header, String content,
                           javafx.scene.control.Alert.AlertType type) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(type, content);
        alert.setHeaderText(header);
        alert.showAndWait();
    }
}