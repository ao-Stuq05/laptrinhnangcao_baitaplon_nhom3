package com.auction.client.controller;

import com.auction.client.SceneManager;
import com.auction.client.network.ServerConnection;
import com.auction.shared.model.Auction;
import com.auction.shared.model.AuctionStatus;
import com.auction.shared.model.BidTransaction;
import com.auction.shared.model.Message;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    private static final int ITEMS_PER_PAGE = 3;   // 3 card / trang, 1 hàng ngang
    private List<Auction>    allAuctions    = new ArrayList<>();
    private List<Auction>    filtered       = new ArrayList<>();
    private int              currentPage    = 1;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    // ── initialize ─────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        cbStatusFilter.getItems().addAll("Tất cả", "Chờ duyệt", "Đang chạy", "Đã kết thúc", "Đã hủy");
        cbStatusFilter.setValue("Tất cả");
        cbStatusFilter.setOnAction(e -> applyFilter());

        ServerConnection.getInstance().setMyAuctionCallback(this::onAuctionsLoaded);

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
            case "Chờ duyệt"   -> allAuctions.stream()
                    .filter(a -> a.getStatus() == AuctionStatus.PENDING_APPROVAL)
                    .collect(Collectors.toList());
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

    // ── Render page ────────────────────────────────────────────────────────────
    private void renderPage(int page) {
        gridMyAuctions.getChildren().clear();
        gridMyAuctions.getRowConstraints().clear();

        if (filtered.isEmpty()) {
            Label empty = new Label("Chưa có phiên đấu giá nào.");
            empty.setTextFill(Color.WHITE);
            gridMyAuctions.add(empty, 0, 0);
            return;
        }

        int from = (page - 1) * ITEMS_PER_PAGE;
        int to   = Math.min(from + ITEMS_PER_PAGE, filtered.size());

        // Thêm RowConstraints động cho mỗi hàng
        int rowCount = (int) Math.ceil((double)(to - from) / 3);
        for (int r = 0; r < rowCount; r++) {
            RowConstraints rc = new RowConstraints();
            rc.setVgrow(Priority.ALWAYS);
            rc.setFillHeight(true);
            gridMyAuctions.getRowConstraints().add(rc);
        }

        int col = 0, row = 0;
        for (int i = from; i < to; i++) {
            VBox card = buildCard(filtered.get(i));
            GridPane.setFillWidth(card, true);
            GridPane.setFillHeight(card, true);
            GridPane.setHgrow(card, Priority.ALWAYS);
            GridPane.setVgrow(card, Priority.ALWAYS);
            gridMyAuctions.add(card, col, row);
            col++;
            if (col == 3) { col = 0; row++; }
        }
    }

    // ── Build card (ĐÃ PHÓNG TO THEO CÁCH 2) ───────────────────────────────────
    private VBox buildCard(Auction auction) {
        // Tăng spacing nội bộ từ 8 -> 14 giúp các thành phần thoáng hơn
        VBox card = new VBox(14);
        // Nâng tầm chiều cao tối thiểu từ 320 -> 460 để card to và dài hơn hẳn
        card.setMinHeight(460);
        card.setMaxHeight(Double.MAX_VALUE);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setStyle(
                "-fx-background-color: rgba(255,255,255,0.08);" +
                        "-fx-border-color: rgba(255,255,255,0.16);" +
                        "-fx-background-radius: 12; -fx-border-radius: 12;" + // Bo tròn góc mượt hơn (8 -> 12)
                        "-fx-border-width: 1; -fx-padding: 20;"); // Tăng padding viền trong từ 12 -> 20 giúp ruột rộng rãi

        // ── Tên sản phẩm (Phóng to font chữ) ──────────────────
        Label lblName = new Label(auction.getItem().getName());
        lblName.setTextFill(Color.WHITE);
        lblName.setFont(Font.font("System", FontWeight.BOLD, 17)); // Tăng size từ 13 -> 17
        lblName.setWrapText(true);

        // ── Danh mục badge (Làm tag to hơn) ───────────────────
        String rawCat   = auction.getItem().getCategory();
        String category = (rawCat != null && !rawCat.isBlank()) ? rawCat : "Khác";

        Label lblCategory = new Label("🏷 " + formatCategory(category));
        lblCategory.setFont(Font.font("System", FontWeight.NORMAL, 12)); // Tăng size từ 11 -> 12
        lblCategory.setTextFill(categoryTextColor(category));
        lblCategory.setStyle(
                "-fx-background-color: " + categoryBg(category)    + ";" +
                        "-fx-border-color: "     + categoryBorder(category) + ";" +
                        "-fx-border-width: 1; -fx-border-radius: 6;" +
                        "-fx-background-radius: 6; -fx-padding: 4 12 4 12;"); // Tăng padding của badge cho nịnh mắt

        // ── Trạng thái ────────────────────────────────────────
        Label lblStatus = buildStatusLabel(auction.getStatus());

        // ── Giá hiện tại ──────────────────────────────────────
        Label lblPrice = new Label(String.format("Giá HT: %,.0fđ", auction.getCurrentPrice()));
        lblPrice.setTextFill(Color.WHITE);
        lblPrice.setFont(Font.font("System", FontWeight.BOLD, 14)); // Tăng nét và kích thước chữ giá tiền

        // ── Thời gian còn lại ─────────────────────────────────
        long minutesLeft = ChronoUnit.MINUTES.between(LocalDateTime.now(), auction.getEndTime());
        String timeText = minutesLeft > 0
                ? String.format("Còn: %d giờ %d phút", minutesLeft / 60, minutesLeft % 60)
                : "Đã kết thúc";
        Label lblTime = new Label(timeText);
        lblTime.setTextFill(minutesLeft > 0 ? Color.LIGHTGREEN : Color.SALMON);
        lblTime.setFont(Font.font("System", FontWeight.NORMAL, 13)); // Tăng size chữ thời gian

        // ── Lịch sử đặt giá — chỉ hiện khi có bid ────────────
        List<BidTransaction> bids = auction.getBids();
        boolean hasBids = bids != null && !bids.isEmpty();
        System.out.println(">>> " + auction.getItem().getName()
                + " bids=" + (bids == null ? "null" : bids.size()));

        // spacer đẩy btnRow xuống đáy card
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // ── Nút Sửa & Hủy phiên (Làm nút dày dặn và to hơn) ───
        HBox btnRow = new HBox(10); // Tăng khoảng cách giữa 2 nút lên 10
        Button btnEdit   = new Button("✏ Chỉnh sửa");
        Button btnCancel = new Button("⊘ Hủy phiên");

        btnEdit.setMaxWidth(Double.MAX_VALUE);
        btnCancel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnEdit,   Priority.ALWAYS);
        HBox.setHgrow(btnCancel, Priority.ALWAYS);

        // Nâng size chữ nút từ 11px -> 13px và thêm padding dọc 10px để nút dày dặn, dễ bấm
        btnEdit.setStyle(
                "-fx-background-color: #0d3d6e; -fx-text-fill: white;" +
                        "-fx-font-size: 13px; -fx-font-weight: bold; -fx-border-color: rgba(255,255,255,0.25);" +
                        "-fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6;" +
                        "-fx-cursor: hand; -fx-padding: 10 0 10 0;");
        btnCancel.setStyle(
                "-fx-background-color: rgba(180,30,30,0.55); -fx-text-fill: white;" +
                        "-fx-font-size: 13px; -fx-font-weight: bold; -fx-border-color: rgba(255,255,255,0.20);" +
                        "-fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6;" +
                        "-fx-cursor: hand; -fx-padding: 10 0 10 0;");

        boolean isActive = auction.getStatus() == AuctionStatus.OPEN
                || auction.getStatus() == AuctionStatus.RUNNING;
        boolean isPending = auction.getStatus() == AuctionStatus.PENDING_APPROVAL;
        btnEdit.setDisable(!isActive);
        btnCancel.setDisable(!isActive && !isPending);

        btnEdit.setOnAction(e -> handleEdit(auction));
        btnCancel.setOnAction(e -> handleCancel(auction));

        btnRow.getChildren().addAll(btnEdit, btnCancel);

        // Nội dung cố định
        card.getChildren().addAll(lblName, lblCategory, lblStatus, lblPrice, lblTime);

        // Lịch sử chỉ xuất hiện khi có dữ liệu
        if (hasBids) {
            Separator sep = new Separator();
            sep.setStyle("-fx-background-color: rgba(255,255,255,0.15);");

            Label lblHistoryTitle = new Label("LỊCH SỬ ĐẶT GIÁ");
            lblHistoryTitle.setTextFill(Color.web("rgba(255,255,255,0.50)"));
            lblHistoryTitle.setFont(Font.font("System", FontWeight.BOLD, 12)); // Tăng từ 10 -> 12

            card.getChildren().addAll(sep, lblHistoryTitle, buildHistoryBox(bids));
        }

        // spacer + nút luôn ở cuối card
        card.getChildren().addAll(spacer, btnRow);
        return card;
    }

    // ── Build vùng lịch sử đặt giá (Tăng cỡ chữ dòng lịch sử) ──────────────────
    private VBox buildHistoryBox(List<BidTransaction> bids) {
        VBox box = new VBox(6); // Tăng khoảng cách dòng lịch sử từ 3 -> 6

        // Hiển thị tối đa 3 bid mới nhất
        int start = Math.max(0, bids.size() - 3);
        for (int i = bids.size() - 1; i >= start; i--) {
            BidTransaction bid = bids.get(i);

            HBox row = new HBox(6);
            row.setStyle(
                    "-fx-border-color: rgba(255,255,255,0.08);" +
                            "-fx-border-width: 0 0 1 0; -fx-padding: 4 0 4 0;"); // Rộng rãi hơn chút

            Label lblUser = new Label(bid.getBidder().getUsername());
            lblUser.setTextFill(Color.web("rgba(255,255,255,0.75)"));
            lblUser.setFont(Font.font(12)); // Tăng font từ 10 -> 12
            HBox.setHgrow(lblUser, Priority.ALWAYS);
            lblUser.setMaxWidth(Double.MAX_VALUE);

            Label lblAmount = new Label(String.format("%,.0fđ", bid.getBidAmount()));
            lblAmount.setTextFill(Color.web("#00ccff"));
            lblAmount.setFont(Font.font("System", FontWeight.BOLD, 12)); // Tăng font từ 10 -> 12

            Label lblTime = new Label(bid.getTimestamp().format(TIME_FMT));
            lblTime.setTextFill(Color.web("rgba(255,255,255,0.35)"));
            lblTime.setFont(Font.font(11)); // Tăng font từ 9 -> 11

            row.getChildren().addAll(lblUser, lblAmount, lblTime);
            box.getChildren().add(row);
        }
        return box;
    }

    // ── Badge trạng thái ──────────────────────────────────────────────────────
    private Label buildStatusLabel(AuctionStatus status) {
        Label lbl = new Label();
        switch (status) {
            case PENDING_APPROVAL -> { lbl.setText("⏳ Chờ duyệt");    lbl.setTextFill(Color.web("#f59e0b")); }
            case OPEN      -> { lbl.setText("● Đang mở");     lbl.setTextFill(Color.LIGHTGREEN); }
            case RUNNING   -> { lbl.setText("● Đang chạy");   lbl.setTextFill(Color.web("#00ccff")); }
            case FINISHED  -> { lbl.setText("● Đã kết thúc"); lbl.setTextFill(Color.SALMON); }
            case CANCELLED -> { lbl.setText("● Đã hủy");      lbl.setTextFill(Color.GRAY); }
            default        -> { lbl.setText("● Không rõ");    lbl.setTextFill(Color.WHITE); }
        }
        lbl.setFont(Font.font("System", FontWeight.BOLD, 13)); // Tăng cỡ chữ từ 11 -> 13
        return lbl;
    }

    // ── Helpers màu danh mục ──────────────────────────────────────────────────
    private String formatCategory(String cat) {
        return switch (cat.toUpperCase()) {
            case "VEHICLE"     -> "Xe cộ";
            case "ART"         -> "Nghệ thuật";
            case "ELECTRONICS" -> "Công nghệ";
            case "FASHION"     -> "Thời trang";
            case "FURNITURE"   -> "Nội thất";
            case "JEWELRY"     -> "Trang sức";
            default            -> cat;
        };
    }

    private Color categoryTextColor(String cat) {
        return switch (cat.toUpperCase()) {
            case "VEHICLE"     -> Color.web("#00ccff");
            case "ART"         -> Color.web("#bf8eff");
            case "ELECTRONICS" -> Color.web("#00ff88");
            case "FASHION"     -> Color.web("#ff80c0");
            case "FURNITURE"   -> Color.web("#ffb347");
            case "JEWELRY"     -> Color.web("#ffe87a");
            default            -> Color.web("rgba(255,255,255,0.70)");
        };
    }

    private String categoryBg(String cat) {
        return switch (cat.toUpperCase()) {
            case "VEHICLE"     -> "rgba(0,200,255,0.12)";
            case "ART"         -> "rgba(160,100,255,0.12)";
            case "ELECTRONICS" -> "rgba(0,255,120,0.10)";
            case "FASHION"     -> "rgba(255,100,180,0.12)";
            case "FURNITURE"   -> "rgba(255,160,50,0.12)";
            case "JEWELRY"     -> "rgba(255,230,80,0.12)";
            default            -> "rgba(255,255,255,0.08)";
        };
    }

    private String categoryBorder(String cat) {
        return switch (cat.toUpperCase()) {
            case "VEHICLE"     -> "rgba(0,200,255,0.30)";
            case "ART"         -> "rgba(160,100,255,0.30)";
            case "ELECTRONICS" -> "rgba(0,255,120,0.28)";
            case "FASHION"     -> "rgba(255,100,180,0.30)";
            case "FURNITURE"   -> "rgba(255,160,50,0.30)";
            case "JEWELRY"     -> "rgba(255,230,80,0.30)";
            default            -> "rgba(255,255,255,0.20)";
        };
    }

    // ── Xử lý Sửa & Hủy phiên ────────────────────────────────────────────────
    private void handleEdit(Auction auction) {
        SceneManager.switchScene("CreateAuction.fxml", auction);
    }

    private void handleCancel(Auction auction) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Hủy phiên \"" + auction.getItem().getName() + "\"?\nHành động này không thể hoàn tác.",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Xác nhận hủy phiên");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    ServerConnection.getInstance()
                            .sendMessage(new Message("CANCEL_AUCTION", auction.getId()));
                    auction.setStatus(AuctionStatus.CANCELLED);
                    updateStats();
                    applyFilter();
                } catch (Exception e) {
                    System.out.println("⚠ Không thể hủy phiên: " + e.getMessage());
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
                    ? "-fx-background-color: #0d3d6e; -fx-text-fill: white;" +
                    "-fx-border-color: rgba(255,255,255,0.25); -fx-border-width: 1;" +
                    "-fx-border-radius: 4; -fx-background-radius: 4;" +
                    "-fx-cursor: hand; -fx-min-width: 32; -fx-min-height: 32;"
                    : "-fx-background-color: rgba(255,255,255,0.10); -fx-text-fill: white;" +
                    "-fx-border-color: rgba(255,255,255,0.20); -fx-border-width: 1;" +
                    "-fx-border-radius: 4; -fx-background-radius: 4;" +
                    "-fx-cursor: hand; -fx-min-width: 32; -fx-min-height: 32;");
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
    @FXML private void handleGoCreateAuction() { SceneManager.switchScene("CreateAuction.fxml"); }

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