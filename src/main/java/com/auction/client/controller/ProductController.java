package com.auction.client.controller;

import com.auction.client.SceneManager;
import com.auction.client.network.ServerConnection;
import com.auction.shared.model.*;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.util.Duration;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class ProductController {

    @FXML private ImageView imgProduct;
    @FXML private Label     lblProductName;
    @FXML private Label     lblCategory;
    @FXML private Label     lblStartPrice;
    @FXML private Label     lblStatus;
    @FXML private Label     lblCurrentPrice;
    @FXML private Label     lblHours;
    @FXML private Label     lblMinutes;
    @FXML private Label     lblSeconds;
    @FXML private Label     lblBidError;
    @FXML private TextField txtBidAmount;
    @FXML private VBox      bidHistoryBox;
    @FXML private VBox      paneBalance;
    @FXML private Label     lblBalance;
    @FXML private HBox      paneBalanceInline;
    @FXML private Label     lblBalanceInline;
    @FXML private Button    btnSellProduct;   // chỉ visible với SELLER
    @FXML private Button btnMyAuction;

    private Auction  currentAuction;
    private Timeline countdown;

    // Top-5 bids: Map<bidderId, highestBidAmount> — tính từ bidHistory
    private final LinkedHashMap<String, Double> top5 = new LinkedHashMap<>();

    private static final NumberFormat VND = NumberFormat.getInstance(new Locale("vi", "VN"));

    @FXML
    public void initialize() {
        Object data = SceneManager.getAndClearData();
        if (!(data instanceof Auction auction)) {
            SceneManager.switchScene("UI.fxml");
            return;
        }
        currentAuction = auction;

        loadAuctionData(auction);
        startCountdown(auction);

        // Build top5 từ lịch sử hiện có
        rebuildTop5FromHistory(auction.getBids());
        renderBidHistory();

        setupBalanceDisplay();

        // Callbacks từ server
        ServerConnection conn = ServerConnection.getInstance();
        conn.setBidUpdateCallback(this::onNewBidReceived);
        conn.setAuctionClosedCallback(this::onAuctionClosed);

        // Hiện nút Đăng bán SP và Đấu giá tôi nếu là Seller
        User currentUser = ServerConnection.getInstance().getCurrentUser();
        boolean isSeller = currentUser != null && "SELLER".equals(currentUser.getRole());
        if (btnSellProduct != null) {
            btnSellProduct.setVisible(isSeller);
            btnSellProduct.setManaged(isSeller);
        }
        if (btnMyAuction != null) {
            btnMyAuction.setVisible(isSeller);
            btnMyAuction.setManaged(isSeller);
        }
    }

    // ── Load thông tin phiên ──────────────────────────────────────────────────
    private void loadAuctionData(Auction auction) {
        lblProductName.setText(auction.getItem().getName());
        lblCategory.setText(auction.getItem().getCategory());
        lblStartPrice.setText(fmt(auction.getItem().getBasePrice()) + "đ");
        refreshCurrentPrice(auction.getCurrentPrice());
        refreshStatus(auction.getStatus());

        // Hiển thị ảnh sản phẩm từ Base64
        String base64 = auction.getItem().getImageBase64();
        if (base64 != null && !base64.isEmpty()) {
            try {
                byte[] imageBytes = java.util.Base64.getDecoder().decode(base64);
                java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(imageBytes);
                imgProduct.setImage(new Image(bis));
            } catch (Exception e) {
                System.out.println("[ProductController] Không load được ảnh: " + e.getMessage());
            }
        }
    }

    private void refreshCurrentPrice(double price) {
        lblCurrentPrice.setText(fmt(price) + "đ");
    }

    private void refreshStatus(AuctionStatus st) {
        boolean open = st == AuctionStatus.OPEN || st == AuctionStatus.RUNNING;
        lblStatus.setText(open ? "● Đang mở bán" : "● Đã kết thúc");
        lblStatus.setTextFill(open ? Color.web("#00ff88") : Color.SALMON);
    }

    // ── Số dư ─────────────────────────────────────────────────────────────────
    private void setupBalanceDisplay() {
        User user = ServerConnection.getInstance().getCurrentUser();
        if (user instanceof Bidder bidder) {
            show(paneBalance); show(paneBalanceInline);
            updateBalanceUI(bidder.getBalance(), bidder.getFrozenBalance());
        } else {
            hide(paneBalance); hide(paneBalanceInline);
        }
        // Ẩn nút đăng bán nếu không phải Seller
        boolean isSeller = user != null && "SELLER".equals(user.getRole());
        if (btnSellProduct != null) {
            btnSellProduct.setVisible(isSeller);
            btnSellProduct.setManaged(isSeller);
        }
    }

    private void updateBalanceUI(double balance, double frozen) {
        String balText = fmt(balance) + " đ";
        if (lblBalance       != null) lblBalance.setText(balText);
        if (lblBalanceInline != null) {
            double avail = balance - frozen;
            lblBalanceInline.setText(fmt(avail) + " đ");
            // Cảnh báo nếu có frozen
            if (frozen > 0 && paneBalanceInline != null) {
                lblBalanceInline.setText(
                        fmt(avail) + " đ  (đang giữ " + fmt(frozen) + "đ)");
                lblBalanceInline.setTextFill(Color.web("#ffd700"));
            } else {
                lblBalanceInline.setTextFill(Color.web("#00ff88"));
            }
        }
    }

    // ── Đếm ngược ─────────────────────────────────────────────────────────────
    private void startCountdown(Auction auction) {
        refreshCountdown(auction.getEndTime());
        countdown = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            long s = ChronoUnit.SECONDS.between(LocalDateTime.now(), auction.getEndTime());
            if (s <= 0) {
                countdown.stop();
                lblHours.setText("00"); lblMinutes.setText("00"); lblSeconds.setText("00");
                refreshStatus(AuctionStatus.FINISHED);
                return;
            }
            lblHours.setText(String.format("%02d", s / 3600));
            lblMinutes.setText(String.format("%02d", (s % 3600) / 60));
            lblSeconds.setText(String.format("%02d", s % 60));
        }));
        countdown.setCycleCount(Timeline.INDEFINITE);
        countdown.play();
    }

    private void refreshCountdown(LocalDateTime end) {
        long s = Math.max(0, ChronoUnit.SECONDS.between(LocalDateTime.now(), end));
        lblHours.setText(String.format("%02d", s / 3600));
        lblMinutes.setText(String.format("%02d", (s % 3600) / 60));
        lblSeconds.setText(String.format("%02d", s % 60));
    }

    // ── Top-5 bid history ─────────────────────────────────────────────────────
    /**
     * Xây dựng top5 map từ danh sách bids:
     * Mỗi bidder chỉ giữ lại giá cao nhất của họ.
     */
    private void rebuildTop5FromHistory(List<BidTransaction> bids) {
        top5.clear();
        if (bids == null) return;
        Map<String, Double> maxPerBidder = new LinkedHashMap<>();
        for (BidTransaction tx : bids) {
            String uid = tx.getBidder().getId();
            maxPerBidder.merge(uid, tx.getBidAmount(), Math::max);
            // Lưu tên theo id
            top5.put(uid + "|" + tx.getBidder().getUsername(), maxPerBidder.get(uid));
        }
        // Dùng map uid|name → amount, sắp xếp giảm dần
        top5.clear();
        Map<String, double[]> temp = new LinkedHashMap<>();
        for (BidTransaction tx : bids) {
            String key = tx.getBidder().getId() + "|" + tx.getBidder().getUsername();
            temp.merge(key, new double[]{tx.getBidAmount()},
                    (a, b) -> new double[]{Math.max(a[0], b[0])});
        }
        // Sắp xếp theo giá giảm dần, lấy top 5
        temp.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue()[0], a.getValue()[0]))
                .limit(5)
                .forEach(e -> top5.put(e.getKey(), e.getValue()[0]));
    }

    /**
     * Thêm / cập nhật 1 bid mới vào top5, sắp xếp lại.
     */
    private void addToTop5(BidTransaction tx) {
        String key = tx.getBidder().getId() + "|" + tx.getBidder().getUsername();
        double current = top5.getOrDefault(key, 0.0);
        if (tx.getBidAmount() > current) top5.put(key, tx.getBidAmount());

        // Sắp xếp lại và giới hạn 5
        List<Map.Entry<String, Double>> entries = new ArrayList<>(top5.entrySet());
        entries.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        top5.clear();
        entries.stream().limit(5).forEach(e -> top5.put(e.getKey(), e.getValue()));
    }

    /** Vẽ lại bảng top-5 lên UI */
    private void renderBidHistory() {
        if (bidHistoryBox == null) return;
        bidHistoryBox.getChildren().clear();

        if (top5.isEmpty()) {
            Label empty = new Label("Chưa có ai đặt giá");
            empty.setTextFill(Color.web("rgba(255,255,255,0.45)"));
            bidHistoryBox.getChildren().add(empty);
            return;
        }

        String[] rankIcons = {"🥇", "🥈", "🥉", "4.", "5."};
        int rank = 0;
        for (Map.Entry<String, Double> entry : top5.entrySet()) {
            String username = entry.getKey().split("\\|")[1];
            double amount   = entry.getValue();

            HBox row = new HBox();
            row.setStyle("-fx-border-color: transparent transparent rgba(255,255,255,0.10) transparent;" +
                    "-fx-border-width: 1; -fx-padding: 6 2;");

            Label lblName = new Label(rankIcons[rank] + "  " + username);
            lblName.setTextFill(rank == 0 ? Color.web("#eab308")
                    : rank == 1 ? Color.web("#c0c0c0")
                    : rank == 2 ? Color.web("#cd7f32") : Color.WHITE);
            lblName.setFont(Font.font("System", FontWeight.BOLD, 13));

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label lblAmt = new Label(fmt(amount) + "đ");
            lblAmt.setTextFill(rank == 0 ? Color.web("#e2ff00") : Color.web("#00ff88"));
            lblAmt.setFont(Font.font("System", FontWeight.BOLD, 13));

            row.getChildren().addAll(lblName, spacer, lblAmt);
            bidHistoryBox.getChildren().add(row);
            rank++;
        }
    }

    // ── Callback: nhận bid mới realtime ───────────────────────────────────────
    private void onNewBidReceived(BidTransaction tx) {
        if (currentAuction == null || !tx.getAuctionId().equals(currentAuction.getId())) return;

        // Cập nhật currentPrice trong auction in-memory (KHÔNG gọi placeBid — tránh duplicate)
        currentAuction.setCurrentPriceOnly(tx.getBidAmount());

        Platform.runLater(() -> {
            // Cập nhật giá
            refreshCurrentPrice(tx.getBidAmount());
            refreshStatus(AuctionStatus.RUNNING);

            // Cập nhật top5
            addToTop5(tx);
            renderBidHistory();

            // Ẩn thông báo lỗi
            if (lblBidError != null) lblBidError.setVisible(false);

            // Cập nhật số dư nếu là bid của mình
            User user = ServerConnection.getInstance().getCurrentUser();
            if (user instanceof Bidder bidder && bidder.getId().equals(tx.getBidder().getId())) {
                updateBalanceUI(bidder.getBalance(), bidder.getFrozenBalance());
                txtBidAmount.clear();
            }
        });
    }

    // ── Callback: phiên kết thúc ──────────────────────────────────────────────
    private void onAuctionClosed(Auction closed) {
        if (currentAuction == null || !closed.getId().equals(currentAuction.getId())) return;
        Platform.runLater(() -> {
            if (countdown != null) countdown.stop();
            refreshStatus(AuctionStatus.FINISHED);
            lblHours.setText("00"); lblMinutes.setText("00"); lblSeconds.setText("00");

            // Nếu user là winner → thông báo thắng + cập nhật số dư
            User user = ServerConnection.getInstance().getCurrentUser();
            if (user instanceof Bidder bidder) {
                // Reload balance từ server (server đã update balance)
                updateBalanceUI(bidder.getBalance(), bidder.getFrozenBalance());

                if (closed.getWinner() != null
                        && closed.getWinner().getId().equals(bidder.getId())) {
                    showBidMsg("🏆 Chúc mừng! Bạn đã thắng phiên đấu giá!", true);
                } else if (top5.keySet().stream().anyMatch(k -> k.startsWith(bidder.getId()))) {
                    showBidMsg("Phiên kết thúc. Tiền đặt cọc đã được hoàn trả.", true);
                }
            }
        });
    }

    // ── Đặt giá ───────────────────────────────────────────────────────────────
    @FXML
    private void handleBid() {
        if (currentAuction == null) return;

        User user = ServerConnection.getInstance().getCurrentUser();
        if (!(user instanceof Bidder bidder)) {
            showBidMsg("Chỉ tài khoản Bidder mới được đặt giá!", false);
            return;
        }
        if (currentAuction.getStatus() != AuctionStatus.OPEN
                && currentAuction.getStatus() != AuctionStatus.RUNNING) {
            showBidMsg("Phiên đấu giá đã kết thúc!", false);
            return;
        }

        String input = txtBidAmount.getText().trim();
        if (input.isEmpty()) { showBidMsg("Vui lòng nhập giá đấu!", false); return; }

        double amount;
        try {
            amount = Double.parseDouble(input.replace(",", "").replace(".", ""));
        } catch (NumberFormatException e) {
            showBidMsg("Giá không hợp lệ! Chỉ nhập số.", false);
            return;
        }

        if (amount <= currentAuction.getCurrentPrice()) {
            showBidMsg(String.format("Giá phải cao hơn %sđ!", fmt(currentAuction.getCurrentPrice())), false);
            return;
        }

        // Kiểm tra số dư khả dụng phía client (server sẽ kiểm tra lại)
        double oldFrozen  = bidder.getFrozenForAuction(currentAuction.getId());
        double available  = bidder.getAvailableBalance() + oldFrozen;
        if (available < amount) {
            showBidMsg(String.format("Số dư khả dụng không đủ! (Khả dụng: %sđ)", fmt(available)), false);
            return;
        }

        try {
            HashMap<String, Object> payload = new HashMap<>();
            payload.put("auctionId", currentAuction.getId());
            payload.put("amount", amount);
            ServerConnection.getInstance().sendMessage(new Message("PLACE_BID", payload));
            if (lblBidError != null) lblBidError.setVisible(false);
        } catch (Exception e) {
            showBidMsg("Không thể kết nối server!", false);
        }
    }

    // ── Điều hướng ────────────────────────────────────────────────────────────
    @FXML private void handleGoHome()  { cleanup(); SceneManager.switchScene("UI.fxml"); }
    @FXML private void handleGoBack()  { cleanup(); SceneManager.switchScene("UI.fxml"); }
    @FXML private void handleGoCreateAuction() { cleanup(); SceneManager.switchScene("CreateAuction.fxml"); }
    @FXML private void handleGoProfile()       { cleanup(); SceneManager.switchScene("Profile.fxml"); }
    @FXML private void handleLogout() {
        cleanup();
        try { ServerConnection.getInstance().sendMessage(new Message("LOGOUT", null)); }
        catch (Exception ignored) {}
        SceneManager.switchScene("login.fxml");
    }
    @FXML private void handleGoMyAuction() {
        SceneManager.switchScene("MyAuctions.fxml");
    }
    private void cleanup() {
        if (countdown != null) countdown.stop();
        ServerConnection conn = ServerConnection.getInstance();
        conn.setBidUpdateCallback(null);
        conn.setAuctionClosedCallback(null);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private void showBidMsg(String msg, boolean success) {
        if (lblBidError != null) {
            lblBidError.setText(msg);
            lblBidError.setTextFill(success ? Color.web("#00ff88") : Color.web("#ff6b6b"));
            lblBidError.setVisible(true);
        }
    }
    private static void show(Region node) { if (node != null) { node.setVisible(true);  node.setManaged(true);  } }
    private static void hide(Region node) { if (node != null) { node.setVisible(false); node.setManaged(false); } }
    private String fmt(double v) { return VND.format((long) v); }
}