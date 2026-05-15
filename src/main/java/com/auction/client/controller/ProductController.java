package com.auction.client.controller;

import com.auction.client.SceneManager;
import com.auction.client.network.ServerConnection;
import com.auction.shared.model.Auction;
import com.auction.shared.model.AuctionStatus;
import com.auction.shared.model.BidTransaction;
import com.auction.shared.model.Message;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;

public class ProductController {

    @FXML private ImageView   imgProduct;
    @FXML private Label       lblProductName;
    @FXML private Label       lblCategory;
    @FXML private Label       lblStartPrice;
    @FXML private Label       lblStatus;
    @FXML private Label       lblCurrentPrice;
    @FXML private Label       lblHours;
    @FXML private Label       lblMinutes;
    @FXML private Label       lblSeconds;
    @FXML private Label       lblBidError;
    @FXML private TextField   txtBidAmount;
    @FXML private TableView   tableBidHistory;
    @FXML private TableColumn colBidder;
    @FXML private TableColumn colPrice;
    @FXML private TableColumn colTime;

    // ✅ Lưu auction đang xem
    @FXML private VBox        bidHistoryBox; // fx:id="bidHistoryBox" trong fxml

    private Auction currentAuction;
    private Timeline countdown;

    @FXML
    public void initialize() {
        // ✅ Nhận auction được truyền từ UIController qua SceneManager
        Object data = SceneManager.getAndClearData();
        if (data instanceof Auction auction) {
            currentAuction = auction;
            loadAuctionData(auction);
            startCountdown(auction);
            loadBidHistory(auction.getBids());
        } else {
            // Không có data → quay lại
            SceneManager.switchScene("UI.fxml");
        }

        // Đăng ký callback nhận cập nhật bid realtime từ server
        ServerConnection.getInstance().setBidUpdateCallback(this::onNewBid);
    }

    // Đổ data auction thật vào các Label
    private void loadAuctionData(Auction auction) {
        lblProductName.setText(auction.getItem().getName());
        lblCategory.setText(auction.getItem().getCategory());
        lblStartPrice.setText(String.format("%,.0fđ", auction.getItem().getBasePrice()));
        lblCurrentPrice.setText(String.format("%,.0fđ", auction.getCurrentPrice()));

        boolean isOpen = auction.getStatus() == AuctionStatus.OPEN
                || auction.getStatus() == AuctionStatus.RUNNING;
        lblStatus.setText(isOpen ? "● Đang mở bán" : "● Đã kết thúc");
        lblStatus.setTextFill(isOpen ? Color.web("#00ff88") : Color.SALMON);
    }

    // Đếm ngược thời gian từ endTime thật của auction
    private void startCountdown(Auction auction) {
        updateCountdownLabels(auction.getEndTime());

        countdown = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            long secondsLeft = ChronoUnit.SECONDS.between(
                    LocalDateTime.now(), auction.getEndTime());

            if (secondsLeft <= 0) {
                countdown.stop();
                lblHours.setText("00");
                lblMinutes.setText("00");
                lblSeconds.setText("00");
                lblStatus.setText("● Đã kết thúc");
                lblStatus.setTextFill(Color.SALMON);
                return;
            }
            lblHours.setText(String.format("%02d", secondsLeft / 3600));
            lblMinutes.setText(String.format("%02d", (secondsLeft % 3600) / 60));
            lblSeconds.setText(String.format("%02d", secondsLeft % 60));
        }));
        countdown.setCycleCount(Timeline.INDEFINITE);
        countdown.play();
    }

    private void updateCountdownLabels(LocalDateTime endTime) {
        long s = ChronoUnit.SECONDS.between(LocalDateTime.now(), endTime);
        if (s < 0) s = 0;
        lblHours.setText(String.format("%02d", s / 3600));
        lblMinutes.setText(String.format("%02d", (s % 3600) / 60));
        lblSeconds.setText(String.format("%02d", s % 60));
    }

    // Hiển thị lịch sử đặt giá (top 3)
    private void loadBidHistory(List<BidTransaction> bids) {
        if (bidHistoryBox == null || bids == null) return;
        bidHistoryBox.getChildren().clear();

        int rank = 1;
        for (int i = bids.size() - 1; i >= 0 && rank <= 3; i--, rank++) {
            BidTransaction tx = bids.get(i);
            HBox row = new HBox();
            row.setStyle("-fx-border-color: transparent transparent " +
                    "rgba(255,255,255,0.1) transparent; " +
                    "-fx-border-width: 1; -fx-padding: 5 0;");

            Label lblBidder = new Label(rank + ". " + tx.getBidder().getUsername());
            lblBidder.setTextFill(rank == 1 ? Color.web("#eab308") : Color.WHITE);
            lblBidder.setFont(Font.font("System", FontWeight.BOLD, 13));

            Region spacer = new Region();
            HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

            Label lblAmount = new Label(String.format("%,.0fđ", tx.getBidAmount()));
            lblAmount.setTextFill(Color.web("#00ff88"));
            lblAmount.setFont(Font.font("System", FontWeight.BOLD, 13));

            row.getChildren().addAll(lblBidder, spacer, lblAmount);
            bidHistoryBox.getChildren().add(row);
        }
    }

    // ✅ Callback nhận bid mới realtime từ server (NEW_BID)
    private void onNewBid(BidTransaction tx) {
        if (currentAuction == null) return;
        if (!tx.getAuctionId().equals(currentAuction.getId())) return;

        Platform.runLater(() -> {
            lblCurrentPrice.setText(String.format("%,.0fđ", tx.getBidAmount()));
            loadBidHistory(currentAuction.getBids());
        });
    }

    // ── Đặt giá ───────────────────────────────────────────────────────────────
    @FXML
    private void handleBid() {
        if (currentAuction == null) return;
        String input = txtBidAmount.getText().trim();

        if (input.isEmpty()) { showBidError("Vui lòng nhập giá đấu!"); return; }

        try {
            double amount = Double.parseDouble(input.replace(",", "").replace(".", ""));
            if (amount <= currentAuction.getCurrentPrice()) {
                showBidError(String.format("Giá phải cao hơn %,.0fđ!", currentAuction.getCurrentPrice()));
                return;
            }

            // Gửi PLACE_BID lên server
            HashMap<String, Object> payload = new HashMap<>();
            payload.put("auctionId", currentAuction.getId());
            payload.put("amount",    amount);
            ServerConnection.getInstance().sendMessage(new Message("PLACE_BID", payload));

            lblBidError.setVisible(false);
            txtBidAmount.clear();
            System.out.println(">>> Gửi PLACE_BID: " + amount);

        } catch (NumberFormatException e) {
            showBidError("Giá không hợp lệ! Chỉ nhập số.");
        } catch (Exception e) {
            showBidError("Không thể kết nối server!");
        }
    }

    // ── Điều hướng ─────────────────────────────────────────────────────────────
    @FXML
    private void handleGoBack() {
        stopCountdown();
        ServerConnection.getInstance().setBidUpdateCallback(null);
        SceneManager.switchScene("UI.fxml");
    }

    @FXML
    private void handleLogout() {
        stopCountdown();
        try { ServerConnection.getInstance().sendMessage(new Message("LOGOUT", null)); }
        catch (Exception e) { e.printStackTrace(); }
        SceneManager.switchScene("login.fxml");
    }

    private void showBidError(String msg) {
        if (lblBidError != null) {
            lblBidError.setText(msg);
            lblBidError.setVisible(true);
        }
    }

    private void stopCountdown() {
        if (countdown != null) countdown.stop();
    }
}