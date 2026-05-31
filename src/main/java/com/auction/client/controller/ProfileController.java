package com.auction.client.controller;

import com.auction.client.SceneManager;
import com.auction.client.network.ServerConnection;
import com.auction.shared.model.Auction;
import com.auction.shared.model.Bidder;
import com.auction.shared.model.BidTransaction;
import com.auction.shared.model.Message;   // ← đúng: shared.model.Message (không phải shared.network)
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

/**
 * ProfileController — Màn hình Hồ sơ cá nhân (Profile.fxml)
 *
 * ─ NHỮNG GÌ CONTROLLER NÀY LÀM ────────────────────────────────────────────
 *  1. initialize()         : Lấy currentUser từ ServerConnection → điền UI
 *  2. fillUserInfo()       : Đổ username/email/balance/role lên các label
 *  3. handleEditProfile()  : Toggle xem ↔ chỉnh sửa, gửi UPDATE_PROFILE
 *  4. handleTopUp()        : Validate → gửi TOP_UP lên server
 *  5. handleQuickTopUp*()  : Điền nhanh 100k / 500k / 1tr vào ô nhập
 *  6. handleTabSell/Buy()  : Chuyển tab, gửi GET_MY_AUCTIONS / GET_MY_BIDS
 *  7. onTopUpResult()      : Callback khi nhận TOP_UP_SUCCESS từ server
 *  8. onMyAuctionsLoaded() : Callback khi nhận GET_MY_AUCTIONS_SUCCESS
 *  9. onMyBidsLoaded()     : Callback khi nhận GET_MY_BIDS_SUCCESS
 *  10. Điều hướng          : goBack/goHome/logout dùng SceneManager
 *
 * ─ CẦN THÊM VÀO SERVER (AuctionServer.java — switch) ──────────────────────
 *
 *  // Lấy phiên đấu giá của Seller hiện tại
 *  case "GET_MY_AUCTIONS" -> {
 *      if (currentUser instanceof Seller seller) {
 *          List<Auction> list = auctionDAO.findBySeller(seller.getId());  // DAO đã có sẵn
 *          out.writeObject(new Message("GET_MY_AUCTIONS_SUCCESS", (Serializable) list));
 *      } else {
 *          out.writeObject(new Message("GET_MY_AUCTIONS_SUCCESS", new ArrayList<>()));
 *      }
 *      out.flush();
 *  }
 *
 *  // Lấy lịch sử bid của Bidder hiện tại  (BidTransactionDAO.findByBidder() cần tự viết thêm)
 *  case "GET_MY_BIDS" -> {
 *      if (currentUser instanceof Bidder bidder) {
 *          List<BidTransaction> list = bidTransactionDAO.findByBidder(bidder.getId());
 *          out.writeObject(new Message("GET_MY_BIDS_SUCCESS", (Serializable) list));
 *      } else {
 *          out.writeObject(new Message("GET_MY_BIDS_SUCCESS", new ArrayList<>()));
 *      }
 *      out.flush();
 *  }
 *
 *  // Nạp tiền — UserDAO.updateBalance() đã có sẵn
 *  case "TOP_UP" -> {
 *      if (currentUser instanceof Bidder bidder) {
 *          HashMap<String, Object> p = (HashMap<String, Object>) request.getPayload();
 *          double amount = (double) p.get("amount");
 *          double newBal = bidder.getBalance() + amount;
 *          userDAO.updateBalance(bidder.getId(), newBal);
 *          bidder.setBalance(newBal);
 *          out.writeObject(new Message("TOP_UP_SUCCESS", newBal));
 *      } else {
 *          out.writeObject(new Message("TOP_UP_FAILED", "Chỉ Bidder mới nạp tiền được!"));
 *      }
 *      out.flush();
 *  }
 *
 *  // Cập nhật thông tin cá nhân
 *  case "UPDATE_PROFILE" -> {
 *      HashMap<String, Object> p = (HashMap<String, Object>) request.getPayload();
 *      // Tự implement userDAO.updateProfile(id, email, passwordHash)
 *      out.writeObject(new Message("UPDATE_PROFILE_SUCCESS", currentUser));
 *      out.flush();
 *  }
 *
 * ─ CẦN THÊM VÀO ServerConnection.handleServerResponse() ───────────────────
 *
 *  case "TOP_UP_SUCCESS" -> {
 *      Double newBalance = (Double) msg.getPayload();
 *      if (topUpCallback != null)
 *          Platform.runLater(() -> topUpCallback.accept(newBalance));
 *  }
 *  case "TOP_UP_FAILED" -> {
 *      String reason = (String) msg.getPayload();
 *      Platform.runLater(() -> showAlert("Nạp tiền thất bại", reason, Alert.AlertType.ERROR));
 *  }
 *  case "GET_MY_AUCTIONS_SUCCESS" -> {
 *      List<Auction> list = (List<Auction>) msg.getPayload();
 *      if (myAuctionCallback != null) myAuctionCallback.accept(list);
 *  }
 *  case "GET_MY_BIDS_SUCCESS" -> {
 *      List<BidTransaction> list = (List<BidTransaction>) msg.getPayload();
 *      if (myBidCallback != null) myBidCallback.accept(list);
 *  }
 *
 *  Thêm 3 field + setter vào ServerConnection:
 *  private Consumer<Double>               topUpCallback;
 *  private Consumer<List<Auction>>        myAuctionCallback;
 *  private Consumer<List<BidTransaction>> myBidCallback;
 *  public void setTopUpCallback(Consumer<Double> cb)               { this.topUpCallback = cb; }
 *  public void setMyAuctionCallback(Consumer<List<Auction>> cb)    { this.myAuctionCallback = cb; }
 *  public void setMyBidCallback(Consumer<List<BidTransaction>> cb) { this.myBidCallback = cb; }
 *
 * ─ CẦN THÊM vào UIController — nút "Hồ sơ" sidebar ───────────────────────
 *  @FXML private void handleGoProfile() {
 *      SceneManager.switchScene("Profile.fxml");
 *  }
 */
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
    @FXML private Button btnSellProduct;
    @FXML private Button btnMyAuction;

    @FXML private Button                         btnTabSell;
    @FXML private Button                         btnTabBuy;
    @FXML private TableView<AuctionRow>          tableHistory;
    @FXML private TableColumn<AuctionRow,String> colProduct;
    @FXML private TableColumn<AuctionRow,String> colStartPrice;
    @FXML private TableColumn<AuctionRow,String> colFinalPrice;
    @FXML private TableColumn<AuctionRow,String> colWinner;
    @FXML private TableColumn<AuctionRow,String> colEndDate;

    // ── State ──────────────────────────────────────────────────────────────────

    private boolean showingSellTab = true;
    private boolean editMode       = false;

    private static final NumberFormat    VND_FMT  = NumberFormat.getInstance(new Locale("vi","VN"));
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ── initialize ─────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        User user = ServerConnection.getInstance().getCurrentUser();
        if (user == null) {
            SceneManager.switchScene("login.fxml");
            return;
        }

        fillUserInfo(user);
        setFormEditable(false);
        setupTableColumns();

        // Đăng ký 3 callback vào ServerConnection
        // (Bạn cần thêm 3 setter này vào ServerConnection — xem comment trên)
        ServerConnection conn = ServerConnection.getInstance();
        conn.setTopUpCallback(this::onTopUpResult);
        conn.setMyAuctionCallback(this::onMyAuctionsLoaded);
        conn.setMyBidCallback(this::onMyBidsLoaded);

        // Mặc định load tab đăng bán
        requestMyAuctions();
    }

    // ── Điền thông tin user ────────────────────────────────────────────────────

    private void fillUserInfo(User user) {
        // Avatar initials — lấy 2 ký tự đầu username
        String uname    = user.getUsername();
        String initials = uname.length() >= 2
                ? uname.substring(0, 2).toUpperCase()
                : uname.toUpperCase();
        lblAvatarInitials.setText(initials);

        lblFullName.setText(uname);
        txtFullName.setText(uname);
        txtUsername.setText(uname);
        txtEmail.setText(user.getEmail() != null ? user.getEmail() : "");
        txtPassword.setText(""); // Không hiển thị hash

        // Ngày tham gia — Entity.getCreatedAt() có sẵn
        if (user.getCreatedAt() != null) {
            lblJoinDate.setText("Tham gia: " + user.getCreatedAt().format(DATE_FMT));
        } else {
            lblJoinDate.setText("Tham gia: —");
        }

        // Role badge
        switch (user.getRole()) {
            case "SELLER" -> {
                lblRole.setText("● Seller");
                lblRole.setStyle(badgeStyle("#00ff88", "rgba(0,200,100,0.18)", "rgba(0,200,100,0.40)"));
            }
            case "ADMIN" -> {
                lblRole.setText("● Admin");
                lblRole.setStyle(badgeStyle("#ff6b6b", "rgba(200,50,50,0.18)", "rgba(200,50,50,0.40)"));
            }
            default -> { // BIDDER
                lblRole.setText("● Bidder");
                lblRole.setStyle(badgeStyle("#60b4ff", "rgba(50,130,200,0.18)", "rgba(50,130,200,0.40)"));
            }
        }
        // Ẩn nút Đăng bán SP và Đấu giá tôi nếu không phải Seller
        boolean isSeller = "SELLER".equals(user.getRole());
        if (btnSellProduct != null) {
            btnSellProduct.setVisible(isSeller);
            btnSellProduct.setManaged(isSeller);
        }
        if (btnMyAuction != null) {
            btnMyAuction.setVisible(isSeller);
            btnMyAuction.setManaged(isSeller);
        }
        // Số dư — chỉ Bidder mới có balance
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
        String newName  = txtFullName.getText().trim();
        String newEmail = txtEmail.getText().trim();
        String newPass  = txtPassword.getText().trim();

        if (newName.isEmpty()) { showProfileMsg("Họ tên không được để trống!", false); return; }
        if (newEmail.isEmpty() || !newEmail.contains("@")) { showProfileMsg("Email không hợp lệ!", false); return; }

        try {
            HashMap<String, Object> payload = new HashMap<>();
            payload.put("name",  newName);
            payload.put("email", newEmail);
            if (!newPass.isEmpty()) payload.put("password", newPass);

            // Message dùng đúng class: com.auction.shared.model.Message
            ServerConnection.getInstance().sendMessage(new Message("UPDATE_PROFILE", payload));
            System.out.println(">>> Đã gửi UPDATE_PROFILE lên Server!");

            // Optimistic UI
            setFormEditable(false);
            btnEdit.setText("✎  Chỉnh sửa");
            editMode = false;
            showProfileMsg("Đã lưu thành công!", true);
            lblFullName.setText(newName);
            String initials = newName.length() >= 2 ? newName.substring(0,2).toUpperCase()
                    : newName.toUpperCase();
            lblAvatarInitials.setText(initials);

        } catch (Exception e) {
            showProfileMsg("Không thể kết nối server!", false);
            e.printStackTrace();
        }
    }

    private void setFormEditable(boolean editable) {
        txtFullName.setEditable(editable);
        txtEmail.setEditable(editable);
        txtPassword.setEditable(editable);

        String activeStyle = "-fx-background-color: rgba(255,255,255,0.92);" +
                "-fx-border-color: #e2ff00; -fx-border-width: 1; -fx-border-radius: 5;" +
                "-fx-background-radius: 5; -fx-font-size: 13px; -fx-padding: 0 12;";
        String lockedStyle = "-fx-background-color: rgba(255,255,255,0.40);" +
                "-fx-border-color: transparent; -fx-background-radius: 5;" +
                "-fx-font-size: 13px; -fx-padding: 0 12; -fx-text-fill: #888;";

        txtFullName.setStyle(editable ? activeStyle : lockedStyle);
        txtEmail.setStyle(editable ? activeStyle : lockedStyle);
        txtPassword.setStyle(editable ? activeStyle : lockedStyle);
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
        String input = txtTopUpAmount.getText().trim();

        if (input.isEmpty()) {
            showTopUpMsg("Vui lòng nhập số tiền cần nạp!", false); return;
        }

        double amount;
        try {
            // Chấp nhận "100.000" hoặc "100000"
            amount = Double.parseDouble(input.replace(".", "").replace(",", ""));
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showTopUpMsg("Số tiền không hợp lệ! Chỉ nhập số dương.", false); return;
        }

        if (amount < 10_000) {
            showTopUpMsg("Số tiền nạp tối thiểu là 10,000đ!", false); return;
        }

        // Kiểm tra user là Bidder mới cho nạp tiền
        User user = ServerConnection.getInstance().getCurrentUser();
        if (!(user instanceof Bidder)) {
            showTopUpMsg("Chỉ tài khoản Bidder mới nạp tiền được!", false); return;
        }

        try {
            HashMap<String, Object> payload = new HashMap<>();
            payload.put("amount", amount);
            ServerConnection.getInstance().sendMessage(new Message("TOP_UP", payload));
            System.out.println(">>> Đã gửi TOP_UP: " + amount + "đ lên Server!");

            txtTopUpAmount.clear();
            lblTopUpError.setText("Đang xử lý...");
            lblTopUpError.setTextFill(Color.web("#e2ff00"));
            lblTopUpError.setVisible(true);

        } catch (Exception e) {
            showTopUpMsg("Không thể kết nối server!", false);
            e.printStackTrace();
        }
    }

    /**
     * Callback từ ServerConnection khi nhận "TOP_UP_SUCCESS".
     * Cập nhật label số dư và object Bidder trong bộ nhớ.
     */
    public void onTopUpResult(Double newBalance) {
        Platform.runLater(() -> {
            updateBalanceLabel(newBalance);
            lblTopUpError.setText("✅ Nạp tiền thành công!");
            lblTopUpError.setTextFill(Color.web("#00ff88"));
            lblTopUpError.setVisible(true);

            // Cập nhật balance trong object đang giữ trong ServerConnection
            User user = ServerConnection.getInstance().getCurrentUser();
            if (user instanceof Bidder bidder) bidder.setBalance(newBalance);
        });
    }

    private void showTopUpMsg(String msg, boolean success) {
        lblTopUpError.setText(msg);
        lblTopUpError.setTextFill(success ? Color.web("#00ff88") : Color.web("#ff6b6b"));
        lblTopUpError.setVisible(true);
    }

    // ── Lịch sử đấu giá ───────────────────────────────────────────────────────

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

    private void requestMyAuctions() {
        try {
            ServerConnection.getInstance().sendMessage(new Message("GET_MY_AUCTIONS", null));
            System.out.println(">>> Đã gửi GET_MY_AUCTIONS lên Server!");
        } catch (Exception e) {
            System.out.println("⚠ Không thể gửi GET_MY_AUCTIONS: " + e.getMessage());
        }
    }

    private void requestMyBids() {
        try {
            ServerConnection.getInstance().sendMessage(new Message("GET_MY_BIDS", null));
            System.out.println(">>> Đã gửi GET_MY_BIDS lên Server!");
        } catch (Exception e) {
            System.out.println("⚠ Không thể gửi GET_MY_BIDS: " + e.getMessage());
        }
    }

    /**
     * Callback từ ServerConnection khi nhận "GET_MY_AUCTIONS_SUCCESS".
     * Dùng AuctionDAO.findBySeller() đã có sẵn.
     */
    public void onMyAuctionsLoaded(List<Auction> auctions) {
        Platform.runLater(() -> {
            ObservableList<AuctionRow> rows = FXCollections.observableArrayList();
            if (auctions != null) {
                for (Auction a : auctions) {
                    String productName = a.getItem() != null ? a.getItem().getName() : "—";
                    String startPrice  = a.getItem() != null
                            ? VND_FMT.format((long) a.getItem().getBasePrice()) + "đ" : "—";
                    String finalPrice  = a.getCurrentPrice() > a.getItem().getBasePrice()
                            ? VND_FMT.format((long) a.getCurrentPrice()) + "đ" : "—";
                    String winner      = a.getWinner() != null
                            ? a.getWinner().getUsername() : "—";
                    String endDate     = a.getEndTime() != null
                            ? a.getEndTime().format(DATE_FMT) : "—";
                    rows.add(new AuctionRow(productName, startPrice, finalPrice, winner, endDate));
                }
            }
            tableHistory.setItems(rows);
        });
    }

    /**
     * Callback từ ServerConnection khi nhận "GET_MY_BIDS_SUCCESS".
     * Cần thêm BidTransactionDAO.findByBidder(bidderId) ở server.
     */
    public void onMyBidsLoaded(List<BidTransaction> bids) {
        Platform.runLater(() -> {
            ObservableList<AuctionRow> rows = FXCollections.observableArrayList();
            if (bids != null) {
                for (BidTransaction tx : bids) {
                    String productName = tx.getAuctionId(); // Chỉ có auctionId, server nên trả tên
                    String startPrice  = "—";
                    String finalPrice  = VND_FMT.format((long) tx.getBidAmount()) + "đ";
                    String winner      = tx.isWinning() ? "✔ Bạn thắng" : "—";
                    String endDate     = tx.getTimestamp() != null
                            ? tx.getTimestamp().format(DATE_FMT) : "—";
                    rows.add(new AuctionRow(productName, startPrice, finalPrice, winner, endDate));
                }
            }
            tableHistory.setItems(rows);
        });
    }

    @FXML
    private void handleTabSell() {
        if (showingSellTab) return;
        showingSellTab = true;
        btnTabSell.setStyle(TAB_ACTIVE_LEFT);
        btnTabBuy.setStyle(TAB_INACTIVE_RIGHT);
        requestMyAuctions();
    }

    @FXML
    private void handleTabBuy() {
        if (!showingSellTab) return;
        showingSellTab = false;
        btnTabBuy.setStyle(TAB_ACTIVE_RIGHT);
        btnTabSell.setStyle(TAB_INACTIVE_LEFT);
        requestMyBids();
    }

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

    @FXML private void handleGoBack()          { SceneManager.switchScene("UI.fxml"); }
    @FXML private void handleGoHome()          { SceneManager.switchScene("UI.fxml"); }
    @FXML private void handleGoCreateAuction() { SceneManager.switchScene("CreateAuction.fxml"); }

    @FXML
    private void handleLogout() {
        // Dọn callback trước khi rời màn hình
        ServerConnection conn = ServerConnection.getInstance();
        conn.setTopUpCallback(null);
        conn.setMyAuctionCallback(null);
        conn.setMyBidCallback(null);

        try {
            conn.sendMessage(new Message("LOGOUT", null));
            System.out.println(">>> Đã gửi LOGOUT lên Server!");
        } catch (Exception e) {
            System.out.println("⚠ Không thể gửi LOGOUT!");
            e.printStackTrace();
        }
        SceneManager.switchScene("login.fxml");
    }
    @FXML private void handleGoMyAuction() {
        SceneManager.switchScene("MyAuctions.fxml");
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
