package com.auction.client.controller;

import com.auction.client.SceneManager;
import com.auction.client.network.ServerConnection;
import com.auction.shared.model.Message;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.time.LocalDate;
import java.util.HashMap;

public class ProductSellerController {

    // ── FXML bindings ──────────────────────────────────────────────────────────

    @FXML private TextField  txtProductName;
    @FXML private ComboBox<String> cbCategory;
    @FXML private ComboBox<String> cbCondition;
    @FXML private TextArea   txtDescription;
    @FXML private TextField  txtStartPrice;
    @FXML private DatePicker dpStartDate;
    @FXML private DatePicker dpEndDate;
    @FXML private Label      lblImageHint;
    @FXML private Label      lblError;

    /** Đường dẫn ảnh người dùng đã chọn */
    private File selectedImageFile;

    // ── Khởi tạo ──────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
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

        dpStartDate.setValue(LocalDate.now());
        dpEndDate.setValue(LocalDate.now().plusDays(1));
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

    // ── Xử lý đăng bán ────────────────────────────────────────────────────────

    @FXML
    private void handleSubmit() {
        // --- Validate ---
        String name      = txtProductName.getText().trim();
        String category  = cbCategory.getValue();
        String condition = cbCondition.getValue();
        String desc      = txtDescription.getText().trim();
        String priceText = txtStartPrice.getText().trim();
        LocalDate startDate = dpStartDate.getValue();
        LocalDate endDate   = dpEndDate.getValue();

        if (name.isEmpty()) {
            showError("Vui lòng nhập tên sản phẩm!");
            return;
        }
        if (category == null) {
            showError("Vui lòng chọn danh mục!");
            return;
        }
        if (condition == null) {
            showError("Vui lòng chọn tình trạng sản phẩm!");
            return;
        }
        if (priceText.isEmpty()) {
            showError("Vui lòng nhập giá khởi điểm!");
            return;
        }

        double startPrice;
        try {
            startPrice = Double.parseDouble(priceText.replace(",", "").replace(".", ""));
            if (startPrice <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showError("Giá khởi điểm không hợp lệ!");
            return;
        }

        if (startDate == null || endDate == null) {
            showError("Vui lòng chọn ngày bắt đầu và kết thúc!");
            return;
        }
        if (!endDate.isAfter(startDate)) {
            showError("Ngày kết thúc phải sau ngày bắt đầu!");
            return;
        }

        hideError();

        // --- Đóng gói payload và gửi lên server ---
        try {
            HashMap<String, Object> payload = new HashMap<>();
            payload.put("name",        name);
            payload.put("category",    category);
            payload.put("condition",   condition);
            payload.put("description", desc);
            payload.put("startPrice",  startPrice);
            payload.put("startDate",   startDate.toString());
            payload.put("endDate",     endDate.toString());
            if (selectedImageFile != null) {
                payload.put("imagePath", selectedImageFile.getAbsolutePath());
            }

            Message msg = new Message("CREATE_AUCTION", payload);
            ServerConnection.getInstance().sendMessage(msg);
            System.out.println(">>> Đã gửi yêu cầu ĐĂNG BÁN lên Server!");

            // ✅ KHÔNG chuyển màn hình ở đây
            // Việc chuyển màn hình sẽ do ServerConnection.handleServerResponse()
            // thực hiện sau khi nhận CREATE_AUCTION_SUCCESS từ server

        } catch (Exception e) {
            showError("Không thể kết nối tới server!");
            System.out.println("⚠ Không thể gửi lên Server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ── Điều hướng ─────────────────────────────────────────────────────────────

    @FXML
    private void handleGoBack() {
        SceneManager.switchScene("UI.fxml");
    }

    @FXML
    private void handleGoHome() {
        SceneManager.switchScene("UI.fxml");
    }

    @FXML
    private void handleGoMyAuction() {
        SceneManager.switchScene("UI.fxml");
    }

    @FXML
    private void handleLogout() {
        try {
            Message msg = new Message("LOGOUT", null);
            ServerConnection.getInstance().sendMessage(msg);
            System.out.println(">>> Đã gửi yêu cầu ĐĂNG XUẤT lên Server!");
        } catch (Exception e) {
            System.out.println("⚠ Không thể gửi lệnh đăng xuất tới Server!");
            e.printStackTrace();
        }
        SceneManager.switchScene("login.fxml");
    }

    // ── Helper ─────────────────────────────────────────────────────────────────

    private void showError(String msg) {
        if (lblError != null) {
            lblError.setText(msg);
            lblError.setVisible(true);
        }
    }

    private void hideError() {
        if (lblError != null) {
            lblError.setVisible(false);
        }
    }
}