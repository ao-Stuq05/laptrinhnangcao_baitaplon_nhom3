package com.auction.client.controller;

import com.auction.client.SceneManager;
import com.auction.client.network.ServerConnection;
import com.auction.shared.model.Auction;
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
import java.util.List;

public class UIController {

    @FXML private TextField        txtSearch;
    @FXML private ComboBox<String> cbFilter;
    @FXML private GridPane         gridAuctions;

    @FXML
    public void initialize() {
        cbFilter.getItems().addAll("Tất cả", "Điện tử", "Nghệ thuật", "Xe cộ", "Đồng hồ & Trang sức");
        cbFilter.setValue("Tất cả");

        ServerConnection.getInstance().setAuctionListCallback(this::displayAuctions);

        try {
            ServerConnection.getInstance().sendMessage(new Message("GET_AUCTIONS", null));
        } catch (Exception e) {
            System.out.println("⚠ Không thể gửi GET_AUCTIONS: " + e.getMessage());
        }
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

            int col = 0, row = 0;
            for (Auction auction : auctions) {
                VBox card = buildAuctionCard(auction);
                gridAuctions.add(card, col, row);
                col++;
                if (col == 3) { col = 0; row++; }
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
        System.out.println("Chuyển sang màn hình đấu giá tôi");
    }

    @FXML private void handleGoProductSeller() {
        SceneManager.switchScene("ProductSeller.fxml");
    }

    // MỚI: Chuyển sang màn hình Hồ sơ cá nhân
    @FXML private void handleGoProfile() {
        SceneManager.switchScene("Profile.fxml");
    }
}