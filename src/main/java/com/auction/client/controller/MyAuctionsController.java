package com.auction.client.controller;

import com.auction.client.SceneManager;
import com.auction.client.network.ServerConnection;
import com.auction.shared.model.Auction;
import com.auction.shared.model.AuctionStatus;
import com.auction.shared.model.Message;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MyAuctionsController {

    // ── FXML bindings ──────────────────────────────────────────────────────────
    @FXML private GridPane         gridMyAuctions;
    @FXML private HBox             hboxPageButtons;
    @FXML private Button           btnPrev;
    @FXML private Button           btnNext;
    @FXML private ComboBox<String> cbStatusFilter;
    @FXML private Label            lblTotalCount;
    @FXML private Label            lblActiveCount;
    @FXML private Label            lblEndedCount;

    // ── State ──────────────────────────────────────────────────────────────────
    private static final int ITEMS_PER_PAGE = 6;
    private List<Auction>    allAuctions    = new ArrayList<>();
    private List<Auction>    filtered       = new ArrayList<>();
    private int              currentPage    = 1;

    // ── initialize ─────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        cbStatusFilter.getItems().addAll("Tất cả", "Đang chạy", "Đã kết thúc", "Đã hủy");
        cbStatusFilter.setValue("Tất cả");
        cbStatusFilter.setOnAction(e -> applyFilter());

        // Đăng ký callback nhận data từ server
        ServerConnection.getInstance().setMyAuctionCallback(this::onAuctionsLoaded);

        // Gửi yêu cầu lấy danh sách phiên của Seller hiện tại
        try {
            ServerConnection.getInstance().sendMessage(new Message("GET_MY_AUCTIONS", null));
        } catch (Exception e) {
            System.out.println("⚠ Không thể gửi GET_MY_AUCTIONS: " + e.getMessage());
        }
    }

    // ── Nhận data từ server ────────────────────────────────────────────────────
    public void onAuctionsLoaded(List<Auction> auctions) {
        Platform.runLater(() -> {
            this.allAuctions = (auctions != null) ? auctions : new ArrayList<>();
            updateStats();
            applyFilter();
        });
    }

    // ── Cập nhật 3 ô thống kê ─────────────────────────────────────────────────
    private void updateStats() {
        long active = allAuctions.stream()
                .filter(a -> a.getStatus() == AuctionStatus.OPEN
                          || a.getStatus() == AuctionStatus.RUNNING)
                .count();
        long ended = allAuctions.stream()
                .filter(a -> a.getStatus() == AuctionStatus.FINISHED
                          || a.getStatus() == AuctionStatus.CANCELLED)
                .count();

        lblTotalCount.setText(String.valueOf(allAuctions.size()));
        lblActiveCount.setText(String.valueOf(active));
        lblEndedCount.setText(String.valueOf(ended));
    }

    // ── Lọc theo ComboBox ──────────────────────────────────────────────────────
    private void applyFilter() {
        String selected = cbStatusFilter.getValue();
        filtered = switch (selected) {
            case "Đang chạy"   -> allAuctions.stream()
                    .filter(a -> a.getStatus() == AuctionStatus.OPEN
                              || a.getStatus() == AuctionStatus.RUNNING)
                    .collect(Collectors.toList());
            case "Đã kết thúc" -> allAuctions.stream()
                    .filter(a -> a.getStatus() == AuctionStatus.FINISHED)
                    .collect(Collectors.toList());
            case "Đã hủy"      -> allAuctions.stream()
                    .filter(a -> a.getStatus() == AuctionStatus.CANCELLED)
                    .collect(Collectors.toList());
            default            -> new ArrayList<>(allAuctions);
        };
        currentPage = 1;
        renderPage(currentPage);
        renderPageButtons();
    }

    // ── Render card ────────────────────────────────────────────────────────────
    private void renderPage(int page) {
        gridMyAuctions.getChildren().clear();

        if (filtered.isEmpty()) {
            Label empty = new Label("Chưa có phiên đấu giá nào.");
            empty.setTextFill(Color.WHITE);
            gridMyAuctions.add(empty, 0, 0);
            return;
        }

        int from = (page - 1) * ITEMS_PER_PAGE;
        int to   = Math.min(from + ITEMS_PER_PAGE, filtered.size());

        int col = 0, row = 0;
        for (int i = from; i < to; i++) {
            VBox card = buildCard(filtered.get(i));
            gridMyAuctions.add(card, col, row);
            col++;
            if (col == 3) { col = 0; row++; }
        }
    }

    private VBox buildCard(Auction auction) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: rgba(255,255,255,0.10);" +
                "-fx-border-color: rgba(255,255,255,0.18);" +
                "-fx-background-radius: 8; -fx-border-radius: 8;" +
                "-fx-border-width: 1; -fx-padding: 12;");

        // Tên sản phẩm
        Label lblName = new Label(auction.getItem().getName());
        lblName.setTextFill(Color.WHITE);
        lblName.setFont(Font.font("System", FontWeight.BOLD, 13));
        lblName.setWrapText(true);

        // Trạng thái
        Label lblStatus = buildStatusLabel(auction.getStatus());

        // Giá hiện tại
        Label lblPrice = new Label(String.format("Giá HT: %,.0fđ", auction.getCurrentPrice()));
        lblPrice.setTextFill(Color.WHITE);

        // Người đang dẫn đầu
        String leaderText = auction.getLeadingBidder() != null
                ? "Dẫn đầu: " + auction.getLeadingBidder().getUsername()
                : "Chưa có ai đặt giá";
        Label lblLeader = new Label(leaderText);
        lblLeader.setTextFill(Color.LIGHTYELLOW);
        lblLeader.setFont(Font.font(11));

        // Thời gian còn lại
        long minutesLeft = ChronoUnit.MINUTES.between(LocalDateTime.now(), auction.getEndTime());
        String timeText = minutesLeft > 0
                ? String.format("Còn: %d giờ %d phút", minutesLeft / 60, minutesLeft % 60)
                : "Đã kết thúc";
        Label lblTime = new Label(timeText);
        lblTime.setTextFill(minutesLeft > 0 ? Color.LIGHTGREEN : Color.SALMON);

        // 2 nút: Chỉnh sửa & Xóa
        HBox btnRow = new HBox(8);
        Button btnEdit   = new Button("✏ Chỉnh sửa");
        Button btnDelete = new Button("🗑 Xóa");

        btnEdit.setMaxWidth(Double.MAX_VALUE);
        btnDelete.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnEdit,   Priority.ALWAYS);
        HBox.setHgrow(btnDelete, Priority.ALWAYS);

        btnEdit.setStyle("-fx-background-color: #0d3d6e; -fx-text-fill: white;" +
                "-fx-font-size: 11px; -fx-border-color: rgba(255,255,255,0.25);" +
                "-fx-border-width: 1; -fx-border-radius: 5; -fx-background-radius: 5; -fx-cursor: hand;");
        btnDelete.setStyle("-fx-background-color: rgba(180,30,30,0.55); -fx-text-fill: white;" +
                "-fx-font-size: 11px; -fx-border-color: rgba(255,255,255,0.20);" +
                "-fx-border-width: 1; -fx-border-radius: 5; -fx-background-radius: 5; -fx-cursor: hand;");

        // Khóa nút nếu phiên đã kết thúc/hủy
        boolean isActive = auction.getStatus() == AuctionStatus.OPEN
                        || auction.getStatus() == AuctionStatus.RUNNING;
        btnEdit.setDisable(!isActive);
        btnDelete.setDisable(!isActive);

        btnEdit.setOnAction(e -> handleEdit(auction));
        btnDelete.setOnAction(e -> handleDelete(auction));

        btnRow.getChildren().addAll(btnEdit, btnDelete);
        card.getChildren().addAll(lblName, lblStatus, lblPrice, lblLeader, lblTime, btnRow);
        return card;
    }

    // Badge trạng thái màu sắc khác nhau
    private Label buildStatusLabel(AuctionStatus status) {
        Label lbl = new Label();
        switch (status) {
            case OPEN     -> { lbl.setText("● Đang mở");    lbl.setTextFill(Color.LIGHTGREEN); }
            case RUNNING  -> { lbl.setText("● Đang chạy");  lbl.setTextFill(Color.web("#00ccff")); }
            case FINISHED -> { lbl.setText("● Đã kết thúc"); lbl.setTextFill(Color.SALMON); }
            case CANCELLED-> { lbl.setText("● Đã hủy");     lbl.setTextFill(Color.GRAY); }
            default       -> { lbl.setText("● Không rõ");   lbl.setTextFill(Color.WHITE); }
        }
        lbl.setFont(Font.font("System", FontWeight.BOLD, 11));
        return lbl;
    }

    // ── Xử lý Chỉnh sửa & Xóa ────────────────────────────────────────────────
    private void handleEdit(Auction auction) {
        // Chuyển sang màn hình chỉnh sửa, truyền auction theo
        SceneManager.switchScene("ProductSeller.fxml", auction);
    }

    private void handleDelete(Auction auction) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Bạn chắc chắn muốn xóa phiên \"" + auction.getItem().getName() + "\"?",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Xác nhận xóa");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    ServerConnection.getInstance()
                            .sendMessage(new Message("DELETE_AUCTION", auction.getId()));
                    allAuctions.remove(auction);
                    updateStats();
                    applyFilter();
                } catch (Exception e) {
                    System.out.println("⚠ Không thể xóa phiên: " + e.getMessage());
                }
            }
        });
    }

    // ── Pagination ─────────────────────────────────────────────────────────────
    private void renderPageButtons() {
        hboxPageButtons.getChildren().clear();
        int totalPages = (int) Math.ceil((double) filtered.size() / ITEMS_PER_PAGE);

        boolean show = totalPages > 1;
        btnPrev.setVisible(show); btnPrev.setManaged(show);
        btnNext.setVisible(show); btnNext.setManaged(show);
        hboxPageButtons.setVisible(show); hboxPageButtons.setManaged(show);

        for (int i = 1; i <= totalPages; i++) {
            final int pageNum = i;
            Button btn = new Button(String.valueOf(i));
            btn.setStyle((i == currentPage)
                    ? "-fx-background-color: #0d3d6e; -fx-text-fill: white; -fx-border-color: rgba(255,255,255,0.25); -fx-border-width: 1; -fx-border-radius: 4; -fx-background-radius: 4; -fx-cursor: hand; -fx-min-width: 32; -fx-min-height: 32;"
                    : "-fx-background-color: rgba(255,255,255,0.10); -fx-text-fill: white; -fx-border-color: rgba(255,255,255,0.20); -fx-border-width: 1; -fx-border-radius: 4; -fx-background-radius: 4; -fx-cursor: hand; -fx-min-width: 32; -fx-min-height: 32;");
            btn.setOnAction(e -> goToPage(pageNum));
            hboxPageButtons.getChildren().add(btn);
        }
    }

    private void goToPage(int page) {
        int totalPages = (int) Math.ceil((double) filtered.size() / ITEMS_PER_PAGE);
        if (page < 1 || page > totalPages) return;
        currentPage = page;
        renderPage(currentPage);
        renderPageButtons();
    }

    @FXML private void handlePrev() { goToPage(currentPage - 1); }
    @FXML private void handleNext() { goToPage(currentPage + 1); }

    // ── Điều hướng ─────────────────────────────────────────────────────────────
    @FXML private void handleGoHome()          { SceneManager.switchScene("UI.fxml"); }
    @FXML private void handleGoProfile()       { SceneManager.switchScene("Profile.fxml"); }
    @FXML private void handleGoProductSeller() { SceneManager.switchScene("ProductSeller.fxml"); }

    @FXML
    private void handleLogout() {
        ServerConnection conn = ServerConnection.getInstance();
        conn.setMyAuctionCallback(null);
        try {
            conn.sendMessage(new Message("LOGOUT", null));
        } catch (Exception e) {
            e.printStackTrace();
        }
        SceneManager.switchScene("login.fxml");
    }
}
