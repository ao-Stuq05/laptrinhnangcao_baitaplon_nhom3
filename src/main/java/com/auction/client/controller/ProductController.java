package com.auction.client.controller;

import com.auction.client.SceneManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

public class ProductController {

    // ===== FXML FIELDS =====
    @FXML private ImageView  imgProduct;
    @FXML private Label      lblProductName;
    @FXML private Label      lblCategory;
    @FXML private Label      lblStartPrice;
    @FXML private Label      lblStatus;
    @FXML private Label      lblCurrentPrice;
    @FXML private Label      lblHours;
    @FXML private Label      lblMinutes;
    @FXML private Label      lblSeconds;
    @FXML private Label      lblBidError;
    @FXML private TextField  txtBidAmount;
    @FXML private TableView  tableBidHistory;
    @FXML private TableColumn colBidder;
    @FXML private TableColumn colPrice;
    @FXML private TableColumn colTime;

    // Đồng hồ đếm ngược
    private Timeline countdown;
    private int totalSeconds = 2 * 3600 + 39 * 60 + 46; // 02:39:46

    // Giá hiện tại (tạm hardcode — sau thay bằng data từ server)
    private double currentPrice = 62_000_000;

    // ===== KHỞI TẠO =====
    @FXML
    public void initialize() {
        loadProductData();
        setupTableColumns();
        startCountdown();
    }

    private void loadProductData() {
        // Tạm hardcode — sau này nhận từ màn hình trước qua static field hoặc Singleton
        lblProductName.setText("Đồng hồ Rolex cổ");
        lblCategory.setText("Đồng hồ & Trang sức");
        lblStartPrice.setText("50,000,000đ");
        lblStatus.setText("● Đang mở bán");
        lblCurrentPrice.setText(formatPrice(currentPrice));
    }

    private void setupTableColumns() {
        colBidder.setCellValueFactory(new PropertyValueFactory<>("bidderName"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("amount"));
        colTime.setCellValueFactory(new PropertyValueFactory<>("time"));
    }

    // ===== ĐỒNG HỒ ĐẾM NGƯỢC =====
    private void startCountdown() {
        countdown = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (totalSeconds <= 0) {
                countdown.stop();
                lblStatus.setText("● Đã kết thúc");
                lblStatus.setStyle("-fx-text-fill: #ff6b6b;");
                return;
            }
            totalSeconds--;
            int h = totalSeconds / 3600;
            int m = (totalSeconds % 3600) / 60;
            int s = totalSeconds % 60;
            lblHours.setText(String.format("%02d", h));
            lblMinutes.setText(String.format("%02d", m));
            lblSeconds.setText(String.format("%02d", s));
        }));
        countdown.setCycleCount(Timeline.INDEFINITE);
        countdown.play();
    }

    // ===== ĐẶT GIÁ =====
    @FXML
    private void handleBid() {
        String input = txtBidAmount.getText().trim();

        if (input.isEmpty()) {
            showBidError("Vui lòng nhập giá đấu!");
            return;
        }

        try {
            double amount = Double.parseDouble(input.replace(",", "").replace(".", ""));

            if (amount <= currentPrice) {
                showBidError("Giá đặt phải cao hơn giá hiện tại (" + formatPrice(currentPrice) + ")!");
                return;
            }

            // Cập nhật giá mới
            currentPrice = amount;
            lblCurrentPrice.setText(formatPrice(currentPrice));
            lblBidError.setVisible(false);
            txtBidAmount.clear();
            System.out.println("✅ Đặt giá: " + formatPrice(amount));

            // Sau này: gửi lên server qua ServerConnection

        } catch (NumberFormatException e) {
            showBidError("Giá không hợp lệ! Chỉ nhập số.");
        }
    }

    // ===== NAVIGATION =====
    @FXML
    private void handleGoBack() {
        stopCountdown();
        SceneManager.switchScene("UI.fxml");
    }

    @FXML
    private void handleGoHome() {
        stopCountdown();
        SceneManager.switchScene("UI.fxml");
    }

    @FXML
    private void handleLogout() {
        stopCountdown();
        SceneManager.switchScene("login.fxml");
    }

    // ===== HELPERS =====
    private void showBidError(String msg) {
        lblBidError.setText(msg);
        lblBidError.setVisible(true);
    }

    private void stopCountdown() {
        if (countdown != null) countdown.stop();
    }

    private String formatPrice(double price) {
        return String.format("%,.0fđ", price);
    }
}
