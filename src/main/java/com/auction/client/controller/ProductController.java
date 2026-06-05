package com.auction.client.controller;

import com.auction.client.SceneManager;
import com.auction.client.network.ServerConnection;
import com.auction.shared.model.*;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
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
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class ProductController {

    // ── FXML fields ──────────────────────────────────────────────────────────
    @FXML private ImageView imgProduct;
    @FXML private Label     lblProductName;
    @FXML private Label     lblCategory;
    @FXML private Label     lblStartPrice;
    @FXML private Label     lblDescription;
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
    @FXML private Button    btnSellProduct;
    @FXML private Button    btnMyAuction;

    // ── Chart fields ─────────────────────────────────────────────────────────
    @FXML private LineChart<String, Number> bidChart;
    @FXML private CategoryAxis             chartXAxis;
    @FXML private NumberAxis               chartYAxis;

    /** Series duy nhất hiển thị đường giá theo thời gian */
    private XYChart.Series<String, Number> chartSeries;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final int MAX_CHART_POINTS = 20;

    // ── State ─────────────────────────────────────────────────────────────────
    private Auction  currentAuction;
    private Timeline countdown;

    /** Top-5: key = "userId|username", value = giá cao nhất */
    private final LinkedHashMap<String, Double> top5 = new LinkedHashMap<>();

    private static final NumberFormat VND = NumberFormat.getInstance(new Locale("vi", "VN"));

    // ── initialize ────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        Object data = SceneManager.getAndClearData();
        if (!(data instanceof Auction auction)) {
            SceneManager.switchScene("UI.fxml");
            return;
        }
        currentAuction = auction;

        initChart();
        loadAuctionData(auction);
        startCountdown(auction);

        seedChartFromHistory(auction.getBids());

        rebuildTop5FromHistory(auction.getBids());
        renderBidHistory();

        setupBalanceDisplay();

        ServerConnection conn = ServerConnection.getInstance();
        conn.setBidUpdateCallback(this::onNewBidReceived);
        conn.setAuctionClosedCallback(this::onAuctionClosed);
        conn.setOutbidCallback(this::onOutbid);
        conn.setAuctionExtendedCallback(this::onAuctionExtended);

        User currentUser = conn.getCurrentUser();
        boolean isSeller = currentUser != null && "SELLER".equals(currentUser.getRole());
        setVisible(btnSellProduct, isSeller);
        setVisible(btnMyAuction, isSeller);
    }

    // ── Chart ─────────────────────────────────────────────────────────────────

    private void initChart() {
        if (bidChart == null) return;

        // Style trục
        chartXAxis.setTickLabelFill(Color.web("rgba(255,255,255,0.55)"));
        chartYAxis.setTickLabelFill(Color.web("rgba(255,255,255,0.55)"));
        chartYAxis.setTickLabelFormatter(new NumberAxis.DefaultFormatter(chartYAxis) {
            @Override
            public String toString(Number v) {
                double val = v.doubleValue();
                if (val >= 1_000_000) return String.format("%.1fM", val / 1_000_000);
                if (val >= 1_000)     return String.format("%.0fk", val / 1_000);
                return String.valueOf((long) val);
            }
        });

        chartXAxis.setAnimated(false);
        chartYAxis.setAnimated(false);

        bidChart.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-plot-background-color: rgba(255,255,255,0.04);" +
                        "-fx-horizontal-grid-lines-visible: true;" +
                        "-fx-vertical-grid-lines-visible: false;" +
                        "-fx-horizontal-zero-line-visible: false;"
        );

        chartSeries = new XYChart.Series<>();
        chartSeries.setName("Giá đấu");
        bidChart.getData().add(chartSeries);

        bidChart.applyCss();
        styleSeries();
    }

    /** Tô màu đường vàng #e2ff00 và các điểm tròn */
    private void styleSeries() {
        Platform.runLater(() -> {
            if (bidChart == null) return;
            var line = bidChart.lookup(".chart-series-line");
            if (line != null) {
                line.setStyle("-fx-stroke: #000000; -fx-stroke-width: 2px;");
            }
            bidChart.lookupAll(".chart-line-symbol").forEach(node ->
                    node.setStyle(
                            "-fx-background-color: #000000, #ffffff;" +
                                    "-fx-background-radius: 5;" +
                                    "-fx-padding: 4;"
                    )
            );
        });
    }

    /**
     * Seed chart từ lịch sử bid có sẵn khi load trang.
     * Chỉ lấy tối đa MAX_CHART_POINTS điểm gần nhất.
     */
    private void seedChartFromHistory(List<BidTransaction> bids) {
        if (chartSeries == null) return;
        if (bids == null || bids.isEmpty()) {
            double basePrice = currentAuction.getItem() != null
                    ? currentAuction.getItem().getBasePrice() : 0;
            addChartPoint(basePrice, "Khởi điểm");
            return;
        }
        int start = Math.max(0, bids.size() - MAX_CHART_POINTS);
        for (int i = start; i < bids.size(); i++) {
            BidTransaction tx = bids.get(i);
            String label = tx.getTimestamp() != null
                    ? tx.getTimestamp().format(TIME_FMT)
                    : String.valueOf(i + 1);
            chartSeries.getData().add(new XYChart.Data<>(label, tx.getBidAmount()));
        }
        styleSeries();
    }

    /**
     * Thêm một điểm mới vào chart.
     * Gọi trong Platform.runLater hoặc từ seedChartFromHistory.
     */
    private void addChartPoint(double price, String timeLabel) {
        if (chartSeries == null) return;
        chartSeries.getData().add(new XYChart.Data<>(timeLabel, price));
        if (chartSeries.getData().size() > MAX_CHART_POINTS) {
            chartSeries.getData().remove(0);
        }
        styleSeries();
    }

    // ── Load dữ liệu ──────────────────────────────────────────────────────────
    private void loadAuctionData(Auction auction) {
        lblProductName.setText(auction.getItem().getName());
        lblCategory.setText(auction.getItem().getCategory());
        lblStartPrice.setText(fmt(auction.getItem().getBasePrice()) + "đ");

        String desc = auction.getItem().getDescription();
        if (lblDescription != null) {
            lblDescription.setText((desc != null && !desc.isBlank()) ? desc : "—");
        }

        refreshCurrentPrice(auction.getCurrentPrice());
        refreshStatus(auction.getStatus());

        String base64 = auction.getItem().getImageBase64();
        if (base64 != null && !base64.isEmpty()) {
            try {
                byte[] bytes = java.util.Base64.getDecoder().decode(base64);
                imgProduct.setImage(new Image(new java.io.ByteArrayInputStream(bytes)));
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
            show(paneBalance);
            show(paneBalanceInline);
            updateBalanceUI(bidder.getBalance(), bidder.getFrozenBalance());
        } else {
            hide(paneBalance);
            hide(paneBalanceInline);
        }
    }

    private void updateBalanceUI(double balance, double frozen) {
        if (lblBalance != null) lblBalance.setText(fmt(balance) + " đ");
        if (lblBalanceInline == null) return;
        double avail = balance - frozen;
        if (frozen > 0) {
            lblBalanceInline.setText(fmt(avail) + " đ  (đang giữ " + fmt(frozen) + "đ)");
            lblBalanceInline.setTextFill(Color.web("#ffd700"));
        } else {
            lblBalanceInline.setText(fmt(avail) + " đ");
            lblBalanceInline.setTextFill(Color.web("#00ff88"));
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

    // ── Top-5 ─────────────────────────────────────────────────────────────────
    private void rebuildTop5FromHistory(List<BidTransaction> bids) {
        top5.clear();
        if (bids == null) return;
        Map<String, double[]> temp = new LinkedHashMap<>();
        for (BidTransaction tx : bids) {
            String key = tx.getBidder().getId() + "|" + tx.getBidder().getUsername();
            temp.merge(key, new double[]{tx.getBidAmount()},
                    (a, b) -> new double[]{Math.max(a[0], b[0])});
        }
        temp.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue()[0], a.getValue()[0]))
                .limit(5)
                .forEach(e -> top5.put(e.getKey(), e.getValue()[0]));
    }

    private void addToTop5(BidTransaction tx) {
        String key = tx.getBidder().getId() + "|" + tx.getBidder().getUsername();
        top5.merge(key, tx.getBidAmount(), Math::max);
        List<Map.Entry<String, Double>> entries = new ArrayList<>(top5.entrySet());
        entries.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        top5.clear();
        entries.stream().limit(5).forEach(e -> top5.put(e.getKey(), e.getValue()));
    }

    private void renderBidHistory() {
        if (bidHistoryBox == null) return;
        bidHistoryBox.getChildren().clear();

        if (top5.isEmpty()) {
            Label empty = new Label("Chưa có ai đặt giá");
            empty.setTextFill(Color.web("rgba(255,255,255,0.45)"));
            empty.setFont(Font.font("System", 12));
            bidHistoryBox.getChildren().add(empty);
            return;
        }

        String[] rankIcons = {"🥇", "🥈", "🥉", "4.", "5."};
        String[] rankBg    = {
                "rgba(234,179,8,0.10)", "rgba(192,192,192,0.08)",
                "rgba(205,127,50,0.08)", "rgba(255,255,255,0.04)", "rgba(255,255,255,0.04)"
        };

        int rank = 0;
        for (Map.Entry<String, Double> entry : top5.entrySet()) {
            String username = entry.getKey().split("\\|")[1];
            double amount   = entry.getValue();

            HBox row = new HBox();
            row.setStyle(String.format(
                    "-fx-background-color: %s; -fx-background-radius: 5; " +
                            "-fx-border-color: rgba(255,255,255,0.07); -fx-border-radius: 5; " +
                            "-fx-border-width: 1; -fx-padding: 5 7; -fx-spacing: 6;",
                    rankBg[rank]));

            Label lblRank = new Label(rankIcons[rank]);
            lblRank.setFont(Font.font("System", 12));
            lblRank.setPrefWidth(24);

            Color nameColor = rank == 0 ? Color.web("#eab308")
                    : rank == 1 ? Color.web("#c0c0c0")
                    : rank == 2 ? Color.web("#cd7f32") : Color.WHITE;
            Label lblName = new Label(username);
            lblName.setTextFill(nameColor);
            lblName.setFont(Font.font("System", FontWeight.BOLD, 11));
            HBox.setHgrow(lblName, Priority.ALWAYS);

            Label lblAmt = new Label(fmt(amount) + "đ");
            lblAmt.setTextFill(rank == 0 ? Color.web("#e2ff00") : Color.web("#00ff88"));
            lblAmt.setFont(Font.font("System", FontWeight.BOLD, 11));

            row.getChildren().addAll(lblRank, lblName, lblAmt);
            bidHistoryBox.getChildren().add(row);
            rank++;
        }

        int totalBids = currentAuction != null && currentAuction.getBids() != null
                ? currentAuction.getBids().size() : top5.size();
        Label lblTotal = new Label("Tổng cộng " + totalBids + " lượt đặt giá");
        lblTotal.setTextFill(Color.web("rgba(255,255,255,0.38)"));
        lblTotal.setFont(Font.font("System", 10));
        lblTotal.setStyle("-fx-padding: 4 2 0 2;");
        bidHistoryBox.getChildren().add(lblTotal);
    }

    // ── Callbacks realtime ────────────────────────────────────────────────────
    private void onNewBidReceived(BidTransaction tx) {
        if (currentAuction == null || !tx.getAuctionId().equals(currentAuction.getId())) return;

        currentAuction.setCurrentPriceOnly(tx.getBidAmount());
        currentAuction.injectBid(tx);

        Platform.runLater(() -> {
            refreshCurrentPrice(tx.getBidAmount());
            refreshStatus(AuctionStatus.RUNNING);

            // ── Thêm điểm vào chart ──
            String timeLabel = tx.getTimestamp() != null
                    ? tx.getTimestamp().format(TIME_FMT)
                    : LocalDateTime.now().format(TIME_FMT);
            addChartPoint(tx.getBidAmount(), timeLabel);

            addToTop5(tx);
            renderBidHistory();

            if (lblBidError != null) lblBidError.setVisible(false);

            User user = ServerConnection.getInstance().getCurrentUser();
            if (user instanceof Bidder bidder && bidder.getId().equals(tx.getBidder().getId())) {
                updateBalanceUI(bidder.getBalance(), bidder.getFrozenBalance());
                txtBidAmount.clear();
            }
        });
    }

    private void onOutbid(String auctionId) {
        if (currentAuction == null || !currentAuction.getId().equals(auctionId)) return;
        User user = ServerConnection.getInstance().getCurrentUser();
        if (user instanceof Bidder bidder) {
            Platform.runLater(() -> {
                updateBalanceUI(bidder.getBalance(), bidder.getFrozenBalance());
                showBidMsg("Bạn vừa bị vượt giá! Tiền đặt cọc đã được hoàn trả.", true);
            });
        }
    }

    /**
     * Anti-sniping: Server gia hạn phiên vì có bid trong 60 giây cuối.
     * Cập nhật lại endTime của auction hiện tại và khởi động lại countdown.
     */
    private void onAuctionExtended(String auctionId, String newEndTimeStr) {
        if (currentAuction == null || !currentAuction.getId().equals(auctionId)) return;
        java.time.LocalDateTime newEnd = java.time.LocalDateTime.parse(newEndTimeStr);
        currentAuction.setEndTime(newEnd);
        Platform.runLater(() -> {
            if (countdown != null) countdown.stop();
            startCountdown(currentAuction);
            showBidMsg("⏱ Phiên được gia hạn thêm 60 giây do có bid mới!", false);
        });
    }

    private void onAuctionClosed(Auction closed) {
        if (currentAuction == null || !closed.getId().equals(currentAuction.getId())) return;
        Platform.runLater(() -> {
            if (countdown != null) countdown.stop();
            refreshStatus(AuctionStatus.FINISHED);
            lblHours.setText("00"); lblMinutes.setText("00"); lblSeconds.setText("00");

            User user = ServerConnection.getInstance().getCurrentUser();
            if (user instanceof Bidder bidder) {
                updateBalanceUI(bidder.getBalance(), bidder.getFrozenBalance());
                if (closed.getWinner() != null && closed.getWinner().getId().equals(bidder.getId())) {
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

        double oldFrozen = bidder.getFrozenForAuction(currentAuction.getId());
        double available = bidder.getAvailableBalance() + oldFrozen;
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
    @FXML private void handleGoHome()          { cleanup(); SceneManager.switchScene("UI.fxml"); }
    @FXML private void handleGoBack()          { cleanup(); SceneManager.switchScene("UI.fxml"); }
    @FXML private void handleGoCreateAuction() { cleanup(); SceneManager.switchScene("CreateAuction.fxml"); }
    @FXML private void handleGoProfile()       { cleanup(); SceneManager.switchScene("Profile.fxml"); }
    @FXML private void handleGoMyAuction()     { SceneManager.switchScene("MyAuctions.fxml"); }

    @FXML private void handleLogout() {
        cleanup();
        try { ServerConnection.getInstance().sendMessage(new Message("LOGOUT", null)); }
        catch (Exception ignored) {}
        SceneManager.switchScene("login.fxml");
    }

    private void cleanup() {
        if (countdown != null) countdown.stop();
        ServerConnection conn = ServerConnection.getInstance();
        conn.setBidUpdateCallback(null);
        conn.setAuctionClosedCallback(null);
        conn.setOutbidCallback(null);
        conn.setAuctionExtendedCallback(null);
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
    private static void setVisible(Button btn, boolean v) {
        if (btn != null) { btn.setVisible(v); btn.setManaged(v); }
    }
    private String fmt(double v) { return VND.format((long) v); }
}