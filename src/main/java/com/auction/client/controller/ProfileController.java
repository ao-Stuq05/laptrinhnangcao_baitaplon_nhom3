package com.auction.client.controller;

import com.auction.client.SceneManager;
import com.auction.client.network.ServerConnection;
import com.auction.shared.model.Auction;
import com.auction.shared.model.Bidder;
import com.auction.shared.model.BidTransaction;
import com.auction.shared.model.Message;
import com.auction.shared.model.Seller;
import com.auction.shared.model.User;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.paint.Color;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class ProfileController {

    // ── FXML bindings ──────────────────────────────────────────────────────────
    @FXML private Label  lblAvatarInitials;
    @FXML private Label  lblFullName;
    @FXML private Label  lblRole;
    @FXML private Label  lblJoinDate;

    @FXML private Label     lblBalance;
    @FXML private TextField txtTopUpAmount;
    @FXML private Label     lblTopUpError;

    @FXML private TextField     txtFullName;
    @FXML private TextField     txtUsername;
    @FXML private TextField     txtEmail;
    @FXML private PasswordField txtPassword;
    @FXML private Label         lblProfileMsg;
    @FXML private Button        btnEdit;

    @FXML private Button                         btnTabSell;
    @FXML private Button                         btnTabBuy;
    @FXML private TableView<AuctionRow>          tableHistory;
    @FXML private TableColumn<AuctionRow,String> colProduct;
    @FXML private TableColumn<AuctionRow,String> colStartPrice;
    @FXML private TableColumn<AuctionRow,String> colFinalPrice;
    @FXML private TableColumn<AuctionRow,String> colWinner;
    @FXML private TableColumn<AuctionRow,String> colEndDate;

    // ── State ──────────────────────────────────────────────────────────────────
    private boolean editMode = false;
    private boolean isSeller = false;

    private static final NumberFormat     VND_FMT  = NumberFormat.getInstance(new Locale("vi", "VN"));
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATE_SHORT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ── initialize ─────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        User user = ServerConnection.getInstance().getCurrentUser();
        if (user == null) {
            SceneManager.switchScene("login.fxml");
            return;
        }

        isSeller = (user instanceof Seller);

        fillUserInfo(user);
        setFormEditable(false);
        setupTableColumns();

        // Đăng ký callbacks
        ServerConnection conn = ServerConnection.getInstance();
        conn.setTopUpCallback(this::onTopUpResult);
        conn.setMyAuctionCallback(this::onMyAuctionsLoaded);
        conn.setMyBidCallback(this::onMyBidsLoaded);

        // Thiết lập tab theo role và load dữ liệu mặc định
        setupTabsByRole(user);
    }

    // ── Thiết lập tab theo role ────────────────────────────────────────────────
    private void setupTabsByRole(User user) {
        if (user instanceof Seller) {
            // Seller: chỉ thấy tab "Lịch sử đăng bán", ẩn tab mua
            btnTabSell.setVisible(true);
            btnTabBuy.setVisible(false);
            btnTabBuy.setManaged(false);
            activateTabSell();
            requestMyAuctions();
        } else if (user instanceof Bidder) {
            // Bidder: chỉ thấy tab "Lịch sử mua", ẩn tab đăng bán
            btnTabSell.setVisible(false);
            btnTabSell.setManaged(false);
            btnTabBuy.setVisible(true);
            activateTabBuy();
            requestMyBids();
        } else {
            // Admin: ẩn cả hai tab, bảng trống
            btnTabSell.setVisible(false);
            btnTabSell.setManaged(false);
            btnTabBuy.setVisible(false);
            btnTabBuy.setManaged(false);
        }
    }

    // ── Điền thông tin user ────────────────────────────────────────────────────
    private void fillUserInfo(User user) {
        String uname    = user.getUsername();
        String initials = uname.length() >= 2
                ? uname.substring(0, 2).toUpperCase()
                : uname.toUpperCase();
        lblAvatarInitials.setText(initials);

        lblFullName.setText(uname);
        txtFullName.setText(uname);
        txtUsername.setText(uname);
        txtEmail.setText(user.getEmail() != null ? user.getEmail() : "");
        txtPassword.setText("");

        if (user.getCreatedAt() != null) {
            lblJoinDate.setText("Tham gia: " + user.getCreatedAt().format(DATE_SHORT));
        } else {
            lblJoinDate.setText("Tham gia: —");
        }

        switch (user.getRole()) {
            case "SELLER" -> {
                lblRole.setText("● Seller");
                lblRole.setStyle(badgeStyle("#00ff88", "rgba(0,200,100,0.18)", "rgba(0,200,100,0.40)"));
            }
            case "ADMIN" -> {
                lblRole.setText("● Admin");
                lblRole.setStyle(badgeStyle("#ff6b6b", "rgba(200,50,50,0.18)", "rgba(200,50,50,0.40)"));
            }
            default -> {
                lblRole.setText("● Bidder");
                lblRole.setStyle(badgeStyle("#60b4ff", "rgba(50,130,200,0.18)", "rgba(50,130,200,0.40)"));
            }
        }

        if (user instanceof Bidder bidder) {
            updateBalanceLabel(bidder.getBalance());
        } else {
            lblBalance.setText("N/A");
        }
    }

    private void updateBalanceLabel(double balance) {
        lblBalance.setText(VND_FMT.format((long) balance) + " đ");
    }

    private String badgeStyle(String textFill, String bg, String border) {
        return "-fx-background-color: " + bg + "; -fx-text-fill: " + textFill + ";" +
                "-fx-border-color: " + border + "; -fx-border-radius: 10; -fx-background-radius: 10;" +
                "-fx-border-width: 1; -fx-padding: 2 10; -fx-font-size: 11px; -fx-font-weight: bold;";
    }

    // ── Chỉnh sửa thông tin cá nhân ───────────────────────────────────────────
    @FXML
    private void handleEditProfile() {
        if (!editMode) {
            setFormEditable(true);
            btnEdit.setText("💾  Lưu");
            editMode = true;
            lblProfileMsg.setVisible(false);
        } else {
            saveProfile();
        }
    }

    private void saveProfile() {
        String newEmail = txtEmail.getText().trim();
        String newPass  = txtPassword.getText().trim();

        if (newEmail.isEmpty() || !newEmail.contains("@")) {
            showProfileMsg("Email không hợp lệ!", false);
            return;
        }

        try {
            HashMap<String, Object> payload = new HashMap<>();
            payload.put("email", newEmail);
            if (!newPass.isEmpty()) {
                payload.put("password", newPass);
            }

            ServerConnection.getInstance().sendMessage(new Message("UPDATE_PROFILE", payload));
            System.out.println(">>> Đã gửi UPDATE_PROFILE lên Server!");

            // Cập nhật UI ngay (optimistic)
            setFormEditable(false);
            btnEdit.setText("✎  Chỉnh sửa");
            editMode = false;
            txtPassword.setText("");
            showProfileMsg("✅ Đã lưu thành công!", true);

            // Cập nhật object in-memory
            User user = ServerConnection.getInstance().getCurrentUser();
            if (user != null) user.setEmail(newEmail);

        } catch (Exception e) {
            showProfileMsg("Không thể kết nối server!", false);
            e.printStackTrace();
        }
    }

    private void setFormEditable(boolean editable) {
        txtEmail.setEditable(editable);
        txtPassword.setEditable(editable);
        // txtUsername và txtFullName không cho sửa
        txtFullName.setEditable(false);
        txtUsername.setEditable(false);

        String activeStyle = "-fx-background-color: rgba(255,255,255,0.92);" +
                "-fx-border-color: #e2ff00; -fx-border-width: 1; -fx-border-radius: 5;" +
                "-fx-background-radius: 5; -fx-font-size: 13px; -fx-padding: 0 12;";
        String lockedStyle = "-fx-background-color: rgba(255,255,255,0.40);" +
                "-fx-border-color: transparent; -fx-background-radius: 5;" +
                "-fx-font-size: 13px; -fx-padding: 0 12; -fx-text-fill: #888;";

        txtEmail.setStyle(editable ? activeStyle : lockedStyle);
        txtPassword.setStyle(editable ? activeStyle : lockedStyle);
        txtFullName.setStyle(lockedStyle);
        txtUsername.setStyle(lockedStyle);
    }

    private void showProfileMsg(String msg, boolean success) {
        lblProfileMsg.setText(msg);
        lblProfileMsg.setTextFill(success ? Color.web("#00ff88") : Color.web("#ff6b6b"));
        lblProfileMsg.setVisible(true);
    }

    // ── Nạp tiền ──────────────────────────────────────────────────────────────
    @FXML private void handleQuickTopUp100k() { txtTopUpAmount.setText("100000"); }
    @FXML private void handleQuickTopUp500k() { txtTopUpAmount.setText("500000"); }
    @FXML private void handleQuickTopUp1m()   { txtTopUpAmount.setText("1000000"); }

    @FXML
    private void handleTopUp() {
        User user = ServerConnection.getInstance().getCurrentUser();
        if (!(user instanceof Bidder)) {
            showTopUpMsg("Chỉ tài khoản Bidder mới nạp tiền được!", false);
            return;
        }

        String input = txtTopUpAmount.getText().trim();
        if (input.isEmpty()) {
            showTopUpMsg("Vui lòng nhập số tiền cần nạp!", false);
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(input.replace(".", "").replace(",", ""));
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showTopUpMsg("Số tiền không hợp lệ! Chỉ nhập số dương.", false);
            return;
        }

        if (amount < 10_000) {
            showTopUpMsg("Số tiền nạp tối thiểu là 10,000đ!", false);
            return;
        }

        try {
            HashMap<String, Object> payload = new HashMap<>();
            payload.put("amount", amount);
            ServerConnection.getInstance().sendMessage(new Message("TOP_UP", payload));
            System.out.println(">>> Đã gửi TOP_UP: " + amount + "đ lên Server!");

            txtTopUpAmount.clear();
            showTopUpMsg("Đang xử lý...", true);

        } catch (Exception e) {
            showTopUpMsg("Không thể kết nối server!", false);
            e.printStackTrace();
        }
    }

    /** Callback từ ServerConnection khi nhận TOP_UP_SUCCESS */
    public void onTopUpResult(Double newBalance) {
        Platform.runLater(() -> {
            updateBalanceLabel(newBalance);
            showTopUpMsg("✅ Nạp tiền thành công! Số dư: " + VND_FMT.format(newBalance.longValue()) + "đ", true);

            User user = ServerConnection.getInstance().getCurrentUser();
            if (user instanceof Bidder bidder) bidder.setBalance(newBalance);
        });
    }

    private void showTopUpMsg(String msg, boolean success) {
        lblTopUpError.setText(msg);
        lblTopUpError.setTextFill(success ? Color.web("#00ff88") : Color.web("#ff6b6b"));
        lblTopUpError.setVisible(true);
    }

    // ── Cột bảng ──────────────────────────────────────────────────────────────
    private void setupTableColumns() {
        colProduct.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().productName));
        colStartPrice.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().startPrice));
        colFinalPrice.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().finalPrice));
        colWinner.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().winner));
        colEndDate.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().endDate));
    }

    // ── Gửi yêu cầu lịch sử ──────────────────────────────────────────────────
    private void requestMyAuctions() {
        try {
            ServerConnection.getInstance().sendMessage(new Message("GET_MY_AUCTIONS", null));
        } catch (Exception e) {
            System.out.println("⚠ Không thể gửi GET_MY_AUCTIONS: " + e.getMessage());
        }
    }

    private void requestMyBids() {
        try {
            ServerConnection.getInstance().sendMessage(new Message("GET_MY_BIDS", null));
        } catch (Exception e) {
            System.out.println("⚠ Không thể gửi GET_MY_BIDS: " + e.getMessage());
        }
    }

    /** Callback: nhận lịch sử đăng bán của Seller */
    public void onMyAuctionsLoaded(List<Auction> auctions) {
        Platform.runLater(() -> {
            ObservableList<AuctionRow> rows = FXCollections.observableArrayList();
            if (auctions != null) {
                for (Auction a : auctions) {
                    String productName = a.getItem() != null ? a.getItem().getName() : "—";
                    String startPrice  = a.getItem() != null
                            ? VND_FMT.format((long) a.getItem().getBasePrice()) + "đ" : "—";
                    String finalPrice  = a.getCurrentPrice() > 0
                            ? VND_FMT.format((long) a.getCurrentPrice()) + "đ" : "—";
                    String winner      = a.getWinner() != null
                            ? a.getWinner().getUsername() : "Chưa có";
                    String endDate     = a.getEndTime() != null
                            ? a.getEndTime().format(DATE_SHORT) : "—";
                    rows.add(new AuctionRow(productName, startPrice, finalPrice, winner, endDate));
                }
            }
            tableHistory.setItems(rows);
            System.out.println("[Profile] Hiển thị " + rows.size() + " phiên đăng bán.");
        });
    }

    /** Callback: nhận lịch sử đặt giá của Bidder */
    public void onMyBidsLoaded(List<BidTransaction> bids) {
        Platform.runLater(() -> {
            ObservableList<AuctionRow> rows = FXCollections.observableArrayList();
            if (bids != null) {
                for (BidTransaction tx : bids) {
                    // auctionId đã được server thay bằng tên sản phẩm (từ findByBidderWithItem)
                    String productName = tx.getAuctionId();
                    String startPrice  = "—";
                    String finalPrice  = VND_FMT.format((long) tx.getBidAmount()) + "đ";
                    String status      = tx.isWinning() ? "✅ Thắng" : "Tham gia";
                    String time        = tx.getTimestamp() != null
                            ? tx.getTimestamp().format(DATE_FMT) : "—";
                    rows.add(new AuctionRow(productName, startPrice, finalPrice, status, time));
                }
            }
            tableHistory.setItems(rows);
            System.out.println("[Profile] Hiển thị " + rows.size() + " lần đặt giá.");
        });
    }

    // ── Chuyển tab ─────────────────────────────────────────────────────────────
    @FXML
    private void handleTabSell() {
        activateTabSell();
        requestMyAuctions();
    }

    @FXML
    private void handleTabBuy() {
        activateTabBuy();
        requestMyBids();
    }

    private void activateTabSell() {
        if (btnTabSell.isVisible()) {
            btnTabSell.setStyle(TAB_ACTIVE_LEFT);
        }
        if (btnTabBuy.isVisible()) {
            btnTabBuy.setStyle(TAB_INACTIVE_RIGHT);
        }
        // Đổi tiêu đề cột cho phù hợp với lịch sử bán
        colStartPrice.setText("Giá khởi điểm");
        colFinalPrice.setText("Giá cuối");
        colWinner.setText("Người thắng");
        colEndDate.setText("Ngày kết thúc");
    }

    private void activateTabBuy() {
        if (btnTabSell.isVisible()) {
            btnTabSell.setStyle(TAB_INACTIVE_LEFT);
        }
        if (btnTabBuy.isVisible()) {
            btnTabBuy.setStyle(TAB_ACTIVE_RIGHT);
        }
        // Đổi tiêu đề cột cho phù hợp với lịch sử mua
        colStartPrice.setText("—");
        colFinalPrice.setText("Giá đặt");
        colWinner.setText("Kết quả");
        colEndDate.setText("Thời gian");
    }

    // Tab styles
    private static final String TAB_ACTIVE_LEFT =
            "-fx-background-color: #0d3d6e; -fx-text-fill: white; -fx-font-weight: bold;" +
                    "-fx-font-size: 12px; -fx-padding: 7 18;" +
                    "-fx-background-radius: 4 0 0 4; -fx-border-radius: 4 0 0 4; -fx-cursor: hand;";
    private static final String TAB_INACTIVE_LEFT =
            "-fx-background-color: rgba(255,255,255,0.10); -fx-text-fill: rgba(255,255,255,0.65);" +
                    "-fx-font-size: 12px; -fx-padding: 7 18;" +
                    "-fx-background-radius: 4 0 0 4; -fx-border-radius: 4 0 0 4; -fx-cursor: hand;";
    private static final String TAB_ACTIVE_RIGHT =
            "-fx-background-color: #0d3d6e; -fx-text-fill: white; -fx-font-weight: bold;" +
                    "-fx-font-size: 12px; -fx-padding: 7 18;" +
                    "-fx-background-radius: 0 4 4 0; -fx-border-radius: 0 4 4 0; -fx-cursor: hand;";
    private static final String TAB_INACTIVE_RIGHT =
            "-fx-background-color: rgba(255,255,255,0.10); -fx-text-fill: rgba(255,255,255,0.65);" +
                    "-fx-font-size: 12px; -fx-padding: 7 18;" +
                    "-fx-background-radius: 0 4 4 0; -fx-border-radius: 0 4 4 0; -fx-cursor: hand;";

    // ── Điều hướng ─────────────────────────────────────────────────────────────
    @FXML private void handleGoBack()          { cleanupCallbacks(); SceneManager.switchScene("UI.fxml"); }
    @FXML private void handleGoHome()          { cleanupCallbacks(); SceneManager.switchScene("UI.fxml"); }
    @FXML private void handleGoMyAuction()     { cleanupCallbacks(); SceneManager.switchScene("UI.fxml"); }
    @FXML private void handleGoProductSeller() { cleanupCallbacks(); SceneManager.switchScene("ProductSeller.fxml"); }

    @FXML
    private void handleLogout() {
        cleanupCallbacks();
        try {
            ServerConnection.getInstance().sendMessage(new Message("LOGOUT", null));
        } catch (Exception e) {
            e.printStackTrace();
        }
        SceneManager.switchScene("login.fxml");
    }

    private void cleanupCallbacks() {
        ServerConnection conn = ServerConnection.getInstance();
        conn.setTopUpCallback(null);
        conn.setMyAuctionCallback(null);
        conn.setMyBidCallback(null);
    }

    // ── Inner class: Row model cho TableView ──────────────────────────────────
    public static class AuctionRow {
        public final String productName;
        public final String startPrice;
        public final String finalPrice;
        public final String winner;
        public final String endDate;

        public AuctionRow(String productName, String startPrice,
                          String finalPrice, String winner, String endDate) {
            this.productName = productName;
            this.startPrice  = startPrice;
            this.finalPrice  = finalPrice;
            this.winner      = winner;
            this.endDate     = endDate;
        }
    }
}
