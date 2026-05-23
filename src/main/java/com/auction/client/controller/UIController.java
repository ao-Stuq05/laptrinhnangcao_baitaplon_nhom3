package com.auction.client.controller;

import com.auction.client.SceneManager;
import com.auction.client.network.ServerConnection;
import com.auction.shared.model.Auction;
import com.auction.shared.model.Bidder;
import com.auction.shared.model.Message;
import com.auction.shared.model.Seller;
import com.auction.shared.model.User;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

public class UIController {

    @FXML private TextField        txtSearch;
    @FXML private ComboBox<String> cbFilter;
    @FXML private GridPane         gridAuctions;

    // Sidebar: số dư + nút đăng bán
    @FXML private VBox   paneBalance;
    @FXML private Label  lblBalance;
    @FXML private Button btnSellProduct;

    private static final NumberFormat VND_FMT = NumberFormat.getInstance(new Locale("vi", "VN"));

    @FXML
    public void initialize() {
        cbFilter.getItems().addAll("Tất cả", "Điện tử", "Nghệ thuật", "Xe cộ", "Đồng hồ & Trang sức");
        cbFilter.setValue("Tất cả");

        // Thiết lập UI theo role ngay khi mở
        setupUIByRole();

        // Đăng ký callback nhận danh sách phiên
        ServerConnection.getInstance().setAuctionListCallback(this::displayAuctions);

        // Đăng ký callback cập nhật số dư khi nạp tiền từ màn hình khác
        ServerConnection.getInstance().setTopUpCallback(this::onBalanceUpdated);

        // Yêu cầu danh sách phiên đấu giá
        try {
            ServerConnection.getInstance().sendMessage(new Message("GET_AUCTIONS", null));
        } catch (Exception e) {
            System.out.println("⚠ Không thể gửi GET_AUCTIONS: " + e.getMessage());
        }
    }

    /** Thiết lập hiển thị số dư và nút đăng bán dựa theo role */
    private void setupUIByRole() {
        User user = ServerConnection.getInstance().getCurrentUser();
        if (user == null) return;

        if (user instanceof Bidder bidder) {
            // Hiện số dư
            paneBalance.setVisible(true);
            paneBalance.setManaged(true);
            updateBalanceLabel(bidder.getBalance());
            // Ẩn nút đăng bán
            btnSellProduct.setVisible(false);
            btnSellProduct.setManaged(false);

        } else if (user instanceof Seller) {
            // Ẩn số dư
            paneBalance.setVisible(false);
            paneBalance.setManaged(false);
            // Hiện nút đăng bán
            btnSellProduct.setVisible(true);
            btnSellProduct.setManaged(true);

        } else {
            // Admin hoặc khác — ẩn cả hai
            paneBalance.setVisible(false);
            paneBalance.setManaged(false);
            btnSellProduct.setVisible(false);
            btnSellProduct.setManaged(false);
        }
    }

    private void updateBalanceLabel(double balance) {
        lblBalance.setText(VND_FMT.format((long) balance) + " đ");
    }

    /** Callback khi Bidder nạp tiền thành công (từ màn hình Profile) */
    private void onBalanceUpdated(Double newBalance) {
        Platform.runLater(() -> updateBalanceLabel(newBalance));
    }

    public void displayAuctions(List<Auction> auctions) {
        Platform.runLater(() -> {
            gridAuctions.getChildren().clear();

            if (auctions == null || auctions.isEmpty()) {
                Label empty = new Label("Chưa có phiên đấu giá nào.");
                empty.setTextFill(Color.WHITE);
                gridAuctions.add(empty, 0, 0);
                return;
            }

            // Lọc theo filter
            String filter = cbFilter.getValue();
            int col = 0, row = 0;
            for (Auction auction : auctions) {
                if (filter != null && !filter.equals("Tất cả")
                        && !filter.equals(auction.getItem().getCategory())) {
                    continue;
                }
                VBox card = buildAuctionCard(auction);
                gridAuctions.add(card, col, row);
                col++;
                if (col == 3) { col = 0; row++; }
            }

            if (gridAuctions.getChildren().isEmpty()) {
                Label empty = new Label("Không có phiên nào trong danh mục này.");
                empty.setTextFill(Color.web("rgba(255,255,255,0.6)"));
                gridAuctions.add(empty, 0, 0);
            }
        });
    }

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

        Label lblPrice = new Label("Giá HT: " + VND_FMT.format((long) auction.getCurrentPrice()) + "đ");
        lblPrice.setTextFill(Color.web("#e2ff00"));

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
        SceneManager.switchScene("Profile.fxml");
    }

    @FXML private void handleGoProductSeller() {
        // Kiểm tra lại role phòng trường hợp gọi trực tiếp
        User user = ServerConnection.getInstance().getCurrentUser();
        if (!(user instanceof Seller)) {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.WARNING,
                    "Chỉ tài khoản Seller mới được đăng bán sản phẩm!");
            alert.setHeaderText("Không có quyền");
            alert.showAndWait();
            return;
        }
        SceneManager.switchScene("ProductSeller.fxml");
    }

    @FXML private void handleGoProfile() {
        SceneManager.switchScene("Profile.fxml");
    }
}
