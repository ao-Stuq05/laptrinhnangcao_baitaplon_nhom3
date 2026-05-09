package com.auction.client.controller;

import com.auction.client.SceneManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

public class ProductController {

    // Kết nối với fx:id trong Product.fxml
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

    private Timeline countdown;
    private int totalSeconds = 2 * 3600 + 39 * 60 + 46;
    private double currentPrice = 62_000_000;

    // Chạy tự động khi FXML load xong
    @FXML
    public void initialize() {
        loadProductData();
        startCountdown();
    }

    private void loadProductData() {
        lblProductName.setText("Đồng hồ Rolex cổ");
        lblCategory.setText("Đồng hồ & Trang sức");
        lblStartPrice.setText("50,000,000đ");
        lblStatus.setText("● Đang mở bán");
        lblCurrentPrice.setText(formatPrice(currentPrice));
        lblHours.setText("02");
        lblMinutes.setText("39");
        lblSeconds.setText("46");
    }

    private void startCountdown() {
        countdown = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (totalSeconds <= 0) {
                countdown.stop();
                lblStatus.setText("● Đã kết thúc");
                return;
            }
            totalSeconds--;
            lblHours.setText(String.format("%02d", totalSeconds / 3600));
            lblMinutes.setText(String.format("%02d", (totalSeconds % 3600) / 60));
            lblSeconds.setText(String.format("%02d", totalSeconds % 60));
        }));
        countdown.setCycleCount(Timeline.INDEFINITE);
        countdown.play();
    }

    // Kết nối với onAction="#handleBid" trong Product.fxml
    @FXML
    private void handleBid() {
        String input = txtBidAmount.getText().trim();

        if (input.isEmpty()) {
            showBidError("Vui lòng nhập giá đấu!");
            return;
        }

        try {
            double amount = Double.parseDouble(input.replace(",", ""));
            if (amount <= currentPrice) {
                showBidError("Giá phải cao hơn " + formatPrice(currentPrice) + "!");
                return;
            }
            currentPrice = amount;
            lblCurrentPrice.setText(formatPrice(currentPrice));
            lblBidError.setVisible(false);
            txtBidAmount.clear();
            System.out.println("✅ Đặt giá: " + formatPrice(amount));
            // Sau này gửi lên server
        } catch (NumberFormatException e) {
            showBidError("Giá không hợp lệ! Chỉ nhập số.");
        }
    }

    // Kết nối với onAction="#handleGoBack" trong Product.fxml
    @FXML
    private void handleGoBack() {
        stopCountdown();
        SceneManager.switchScene("UI.fxml");
    }

    // Kết nối với onAction="#handleLogout" trong Product.fxml
    @FXML
    private void handleLogout() {
        stopCountdown();
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

    private String formatPrice(double price) {
        return String.format("%,.0fđ", price);
    }
}