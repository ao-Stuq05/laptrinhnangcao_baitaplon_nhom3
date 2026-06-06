package com.auction.client.controller;

import com.auction.client.SceneManager;
import com.auction.client.network.ServerConnection;
import com.auction.shared.model.Auction;
import com.auction.shared.model.Message;
import com.auction.shared.model.Seller;
import com.auction.shared.model.User;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

public class CreateAuctionController {

    // ── FXML bindings ──────────────────────────────────────────────────────────

    @FXML private TextField  txtProductName;
    @FXML private ComboBox<String> cbCategory;
    @FXML private ComboBox<String> cbCondition;
    @FXML private TextArea   txtDescription;
    @FXML private TextField  txtStartPrice;

    /** Hiển thị thời gian bắt đầu cố định = thời điểm mở form (read-only) */
    @FXML private Label      lblStartTime;

    /** Nhập số giờ thủ công */
    @FXML private TextField txtDurationHours;

    /** Nhập số phút thủ công */
    @FXML private TextField txtDurationMinutes;

    @FXML private Label      lblImageHint;
    @FXML private Label      lblError;
    @FXML private Button     btnSubmit;

    /** Đường dẫn ảnh người dùng đã chọn */
    private File selectedImageFile;

    /** Nếu != null → đang ở chế độ chỉnh sửa phiên */
    private Auction editingAuction = null;

    // ── Khởi tạo ──────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        // Chặn Bidder truy cập màn hình này
        User user = ServerConnection.getInstance().getCurrentUser();
        if (!(user instanceof Seller)) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Không có quyền truy cập");
            alert.setHeaderText(null);
            alert.setContentText("Tính năng này chỉ dành cho Seller!");
            alert.showAndWait();
            SceneManager.switchScene("UI.fxml");
            return;
        }

        cbCategory.setItems(FXCollections.observableArrayList(
                "Đồng hồ & Trang sức",
                "Điện tử",
                "Nghệ thuật",
                "Cổ vật",
                "Xe cộ",
                "Khác"
        ));

        cbCondition.setItems(FXCollections.observableArrayList(
                "Mới 100%",
                "Đã qua sử dụng",
                "Cổ vật / Cũ"
        ));

        // ── Thời gian bắt đầu: cố định = thời điểm mở form ──────────────────
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        lblStartTime.setText(LocalDateTime.now().format(fmt));

        // ── Nhận auction từ SceneManager nếu đang chỉnh sửa ─────────────────
        Object data = SceneManager.getAndClearData();
        if (data instanceof Auction auction) {
            editingAuction = auction;
            populateForm(auction);
        }
    }

    /** Điền sẵn dữ liệu auction vào form khi chỉnh sửa */
    private void populateForm(Auction auction) {
        txtProductName.setText(auction.getItem().getName());
        txtDescription.setText(auction.getItem().getDescription());
        txtStartPrice.setText(String.valueOf((long) auction.getCurrentPrice()));

        // Category: map ngược từ enum về tên hiển thị trong ComboBox
        String rawCat = auction.getItem().getCategory();
        if (rawCat != null) {
            String displayCat = switch (rawCat.toUpperCase()) {
                case "VEHICLE"     -> "Xe cộ";
                case "ELECTRONICS" -> "Điện tử";
                case "ART"         -> "Nghệ thuật & Cổ vật";
                case "JEWELRY"     -> "Đồng hồ & Trang sức";
                case "OTHER"       -> "Khác";
                default            -> "Khác";
            };
            cbCategory.setValue(displayCat);
        }

        // [FIX] Dung SECONDS.between + round up de tranh floor mat ~1 phut khi edit
        long totalSecs = java.time.temporal.ChronoUnit.SECONDS.between(
                LocalDateTime.now(), auction.getEndTime());
        long totalMinutes = (totalSecs + 59) / 60; // round up: 1:59:45 -> 120 phut
        if (totalMinutes > 0) {
            txtDurationHours.setText(String.valueOf(totalMinutes / 60));
            txtDurationMinutes.setText(String.valueOf(totalMinutes % 60));
        }

        // Đổi text nút và tiêu đề sang chế độ chỉnh sửa
        if (btnSubmit != null) btnSubmit.setText("CẬP NHẬT PHIÊN");
    }

    // ── Xử lý chọn ảnh ────────────────────────────────────────────────────────

    @FXML
    private void handleChooseImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Chọn ảnh sản phẩm");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Ảnh (PNG, JPG)", "*.png", "*.jpg", "*.jpeg")
        );

        Stage stage = SceneManager.getStage();
        selectedImageFile = chooser.showOpenDialog(stage);

        if (selectedImageFile != null) {
            lblImageHint.setText("✅ " + selectedImageFile.getName());
        }
    }

    // ── Xử lý đăng bán / cập nhật ────────────────────────────────────────────

    @FXML
    private void handleSubmit() {
        // --- Validate ---
        String name      = txtProductName.getText().trim();
        String category  = cbCategory.getValue();
        String condition = cbCondition.getValue();
        String desc      = txtDescription.getText().trim();
        String priceText = txtStartPrice.getText().trim();

        if (name.isEmpty()) { showError("Vui lòng nhập tên sản phẩm!"); return; }
        if (category == null) { showError("Vui lòng chọn danh mục!"); return; }
        // Chỉ bắt buộc condition khi tạo mới — khi edit, condition không lưu trong DB nên có thể null
        if (condition == null && editingAuction == null) { showError("Vui lòng chọn tình trạng sản phẩm!"); return; }
        if (priceText.isEmpty()) { showError("Vui lòng nhập giá khởi điểm!"); return; }

        double startPrice;
        try {
            startPrice = Double.parseDouble(priceText.replace(",", "").replace(".", ""));
            if (startPrice <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showError("Giá khởi điểm không hợp lệ!");
            return;
        }

        // Lấy số giờ và phút từ TextField
        int durationHours, durationMinutes;
        try {
            String hText = txtDurationHours.getText().trim();
            durationHours = hText.isEmpty() ? 0 : Integer.parseInt(hText);
            if (durationHours < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showError("Số giờ không hợp lệ!");
            return;
        }
        try {
            String mText = txtDurationMinutes.getText().trim();
            durationMinutes = mText.isEmpty() ? 0 : Integer.parseInt(mText);
            if (durationMinutes < 0 || durationMinutes > 59) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showError("Số phút không hợp lệ (0 – 59)!");
            return;
        }

        if (durationHours == 0 && durationMinutes == 0) {
            showError("Thời lượng phải ít nhất 1 phút!");
            return;
        }

        hideError();

        // [FIX] Encode anh TRUOC khi gui, va gui durationSeconds thay vi endDateTime.
        // Server tu tinh endTime = LocalDateTime.now() + durationSeconds bang dong ho
        // cua chinh server, tranh lech clock giua client va server (hoac do tre upload).
        String imageBase64 = null;
        if (selectedImageFile != null) {
            try {
                byte[] imageBytes = java.nio.file.Files.readAllBytes(selectedImageFile.toPath());
                imageBase64 = java.util.Base64.getEncoder().encodeToString(imageBytes);
            } catch (Exception imgEx) {
                System.out.println("[CreateAuction] Khong doc duoc file anh: " + imgEx.getMessage());
            }
        }
        long durationSeconds = (long) durationHours * 3600L + (long) durationMinutes * 60L;

        try {
            HashMap<String, Object> payload = new HashMap<>();
            payload.put("name",            name);
            payload.put("category",        category);
            payload.put("condition",       condition);
            payload.put("description",     desc);
            payload.put("startPrice",      startPrice);
            payload.put("durationSeconds", durationSeconds);
            if (imageBase64 != null) payload.put("imageBase64", imageBase64);


            if (editingAuction != null) {
                // ── Chế độ chỉnh sửa: gửi UPDATE_AUCTION kèm auctionId ──────
                payload.put("auctionId", editingAuction.getId());
                ServerConnection.getInstance().sendMessage(new Message("UPDATE_AUCTION", payload));
                System.out.println(">>> Đã gửi yêu cầu CẬP NHẬT phiên: " + editingAuction.getId());
            } else {
                // ── Chế độ tạo mới ──────────────────────────────────────────
                ServerConnection.getInstance().sendMessage(new Message("CREATE_AUCTION", payload));
                System.out.println(">>> Đã gửi yêu cầu ĐĂNG BÁN lên Server!");
            }

        } catch (Exception e) {
            showError("Không thể kết nối tới server!");
            System.out.println("⚠ Không thể gửi lên Server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ── Điều hướng ─────────────────────────────────────────────────────────────

    @FXML private void handleGoBack()        { SceneManager.switchScene("UI.fxml"); }
    @FXML private void handleGoHome()        { SceneManager.switchScene("UI.fxml"); }
    @FXML private void handleGoProfile()     { SceneManager.switchScene("Profile.fxml"); }

    @FXML
    private void handleLogout() {
        try {
            Message msg = new Message("LOGOUT", null);
            ServerConnection.getInstance().sendMessage(msg);
        } catch (Exception e) {
            System.out.println("⚠ Không thể gửi lệnh đăng xuất tới Server!");
        }
        SceneManager.switchScene("login.fxml");
    }
    @FXML private void handleGoMyAuction() {
        SceneManager.switchScene("MyAuctions.fxml");
    }

    // ── Helper ─────────────────────────────────────────────────────────────────

    private void showError(String msg) {
        if (lblError != null) { lblError.setText(msg); lblError.setVisible(true); }
    }

    private void hideError() {
        if (lblError != null) { lblError.setVisible(false); }
    }
}