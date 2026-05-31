package com.auction.client.controller;

import com.auction.client.SceneManager;
import com.auction.client.network.ServerConnection;
import com.auction.shared.model.Auction;
import com.auction.shared.model.Message;
import com.auction.shared.model.User;
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

public class UIController {

    @FXML private TextField        txtSearch;
    @FXML private ComboBox<String> cbFilter;
    @FXML private GridPane         gridAuctions;

    // Nút « và » trong FXML
    @FXML private Button btnPrev;
    @FXML private Button btnNext;
    @FXML private Button btnMyAuction;

    // HBox chứa các nút số trang (inject động)
    @FXML private HBox   hboxPageButtons;

    // Nút đăng bán — chỉ hiển thị với SELLER
    @FXML private Button btnSellProduct;

    private static final int    ITEMS_PER_PAGE = 6; // 3 cột x 2 hàng
    private List<Auction>       allAuctions    = new ArrayList<>();
    private int                 currentPage    = 1;

    @FXML
    public void initialize() {
        cbFilter.getItems().addAll("Tất cả", "Điện tử", "Nghệ thuật", "Xe cộ", "Đồng hồ & Trang sức");
        cbFilter.setValue("Tất cả");

        // Kiểm tra role: chỉ SELLER mới thấy nút "+ Đăng bán SP"
        User currentUser = ServerConnection.getInstance().getCurrentUser();
        boolean isSeller = currentUser != null && "SELLER".equals(currentUser.getRole());
        if (btnSellProduct != null) {
            btnSellProduct.setVisible(isSeller);
            btnSellProduct.setManaged(isSeller);
        }

        // Ẩn nút Đấu giá tôi nếu không phải Seller
        if (btnMyAuction != null) {
            btnMyAuction.setVisible(isSeller);
            btnMyAuction.setManaged(isSeller);
        }
        ServerConnection.getInstance().setAuctionListCallback(this::displayAuctions);

        try {
            ServerConnection.getInstance().sendMessage(new Message("GET_AUCTIONS", null));
        } catch (Exception e) {
            System.out.println("⚠ Không thể gửi GET_AUCTIONS: " + e.getMessage());
        }
    }

    // Được gọi từ ServerConnection khi nhận data từ server
    public void displayAuctions(List<Auction> auctions) {
        Platform.runLater(() -> {
            if (gridAuctions == null) return;
            this.allAuctions = (auctions != null) ? auctions : new ArrayList<>();
            this.currentPage = 1;
            renderPage(currentPage);
            renderPageButtons();
        });
    }

    // Render các card của trang hiện tại vào GridPane
    private void renderPage(int page) {
        gridAuctions.getChildren().clear();

        if (allAuctions.isEmpty()) {
            Label empty = new Label("Chưa có phiên đấu giá nào.");
            empty.setTextFill(Color.WHITE);
            gridAuctions.add(empty, 0, 0);
            return;
        }

        int from = (page - 1) * ITEMS_PER_PAGE;
        int to   = Math.min(from + ITEMS_PER_PAGE, allAuctions.size());

        int col = 0, row = 0;
        for (int i = from; i < to; i++) {
            VBox card = buildAuctionCard(allAuctions.get(i));
            gridAuctions.add(card, col, row);
            col++;
            if (col == 3) { col = 0; row++; }
        }
    }

    // Tạo động các nút số trang vào hboxPageButtons
    private void renderPageButtons() {
        hboxPageButtons.getChildren().clear();

        int totalPages = (int) Math.ceil((double) allAuctions.size() / ITEMS_PER_PAGE);

        // Ẩn/hiện toàn bộ khu vực pagination
        boolean show = totalPages > 1;
        btnPrev.setVisible(show);
        btnPrev.setManaged(show);
        btnNext.setVisible(show);
        btnNext.setManaged(show);
        hboxPageButtons.setVisible(show);
        hboxPageButtons.setManaged(show);

        for (int i = 1; i <= totalPages; i++) {
            final int pageNum = i;
            Button btn = new Button(String.valueOf(i));
            boolean isActive = (i == currentPage);
            btn.setStyle(isActive
                    ? "-fx-background-color: #0d3d6e; -fx-text-fill: white; -fx-border-color: rgba(255,255,255,0.25); -fx-border-width: 1; -fx-border-radius: 4; -fx-background-radius: 4; -fx-cursor: hand; -fx-min-width: 32; -fx-min-height: 32;"
                    : "-fx-background-color: rgba(255,255,255,0.10); -fx-text-fill: white; -fx-border-color: rgba(255,255,255,0.20); -fx-border-width: 1; -fx-border-radius: 4; -fx-background-radius: 4; -fx-cursor: hand; -fx-min-width: 32; -fx-min-height: 32;"
            );
            btn.setOnAction(e -> goToPage(pageNum));
            hboxPageButtons.getChildren().add(btn);
        }
    }

    private void goToPage(int page) {
        int totalPages = (int) Math.ceil((double) allAuctions.size() / ITEMS_PER_PAGE);
        if (page < 1 || page > totalPages) return;
        currentPage = page;
        renderPage(currentPage);
        renderPageButtons();
    }

    @FXML private void handlePrev() { goToPage(currentPage - 1); }
    @FXML private void handleNext() { goToPage(currentPage + 1); }

    // ----------------------------------------------------------------
    // Phần còn lại giữ nguyên như cũ
    // ----------------------------------------------------------------

    private VBox buildAuctionCard(Auction auction) {
        VBox card = new VBox(6);
        card.setStyle("-fx-background-color: rgba(255,255,255,0.10);" +
                "-fx-border-color: rgba(255,255,255,0.18);" +
                "-fx-background-radius: 8; -fx-border-radius: 8;" +
                "-fx-border-width: 1; -fx-padding: 10;");

        Label lblName = new Label(auction.getItem().getName());
        lblName.setTextFill(Color.WHITE);
        lblName.setFont(Font.font("System", FontWeight.BOLD, 13));
        lblName.setWrapText(true);

        Label lblPrice = new Label(String.format("Giá HT: %,.0fđ", auction.getCurrentPrice()));
        lblPrice.setTextFill(Color.WHITE);

        long minutesLeft = ChronoUnit.MINUTES.between(LocalDateTime.now(), auction.getEndTime());
        String timeText = minutesLeft > 0
                ? String.format("Còn: %d giờ %d phút", minutesLeft / 60, minutesLeft % 60)
                : "Đã kết thúc";
        Label lblTime = new Label(timeText);
        lblTime.setTextFill(minutesLeft > 0 ? Color.LIGHTGREEN : Color.SALMON);

        Label lblCategory = new Label(auction.getItem().getCategory());
        lblCategory.setTextFill(Color.LIGHTYELLOW);
        lblCategory.setFont(Font.font(11));

        Button btnEnter = new Button("Vào phòng");
        btnEnter.setMaxWidth(Double.MAX_VALUE);
        btnEnter.setStyle("-fx-background-color: #0d3d6e; -fx-text-fill: white;" +
                "-fx-font-size: 12px; -fx-border-color: rgba(255,255,255,0.25);" +
                "-fx-border-width: 1; -fx-border-radius: 5;" +
                "-fx-background-radius: 5; -fx-cursor: hand;");

        btnEnter.setOnAction(e -> SceneManager.switchScene("Product.fxml", auction));

        card.getChildren().addAll(lblName, lblPrice, lblTime, lblCategory, btnEnter);
        return card;
    }

    @FXML private void handleLogout() {
        try { ServerConnection.getInstance().sendMessage(new Message("LOGOUT", null)); }
        catch (Exception e) { e.printStackTrace(); }
        SceneManager.switchScene("login.fxml");
    }

    @FXML private void handleGoMyAuction() {
        SceneManager.switchScene("MyAuctions.fxml");
    }

    @FXML private void handleGoCreateAuction() {
        SceneManager.switchScene("CreateAuction.fxml");
    }

    @FXML private void handleGoProfile() {
        SceneManager.switchScene("Profile.fxml");
    }
}