package com.auction.client.controller;

import com.auction.client.SceneManager;
import com.auction.client.network.ServerConnection;
import com.auction.shared.model.Admin;
import com.auction.shared.model.Auction;
import com.auction.shared.model.AuctionStatus;
import com.auction.shared.model.Bidder;
import com.auction.shared.model.Message;
import com.auction.shared.model.User;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;

import java.text.NumberFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * AdminController — Màn hình Admin (Admin.fxml)
 *
 * Luồng hoạt động:
 *  1. initialize()        → kiểm tra instanceof Admin → setup bảng → loadDashboardData()
 *  2. loadDashboardData() → đăng ký callback lên ServerConnection → gửi GET_ALL_USERS + GET_ALL_AUCTIONS
 *  3. ServerConnection    → nhận GET_ALL_USERS_SUCCESS / GET_ALL_AUCTIONS_SUCCESS → gọi callback
 *  4. callback            → Platform.runLater → fill UI
 *  5. Approve/Reject/Ban  → gửi message → chờ SUCCESS callback → cập nhật UI
 */
public class AdminController {

    // ── Sidebar ───────────────────────────────────────────────
    @FXML private Button btnDashboard;
    @FXML private Button btnUsers;
    @FXML private Button btnApprovals;
    @FXML private Button btnLive;
    @FXML private Button btnHistory;
    @FXML private Button btnLogout;

    // ── Header ────────────────────────────────────────────────
    @FXML private TextField txtSearchAdmin;

    // ── Stat cards ────────────────────────────────────────────
    @FXML private Label lblTotalUsers;
    @FXML private Label lblPending;
    @FXML private Label lblLive;

    // ── Bảng User ─────────────────────────────────────────────
    @FXML private TableView<User>           tableUsers;
    @FXML private TableColumn<User, String> colUserName;
    @FXML private TableColumn<User, String> colUsername;
    @FXML private TableColumn<User, String> colRole;
    @FXML private TableColumn<User, String> colBalance;
    @FXML private TableColumn<User, String> colUserAction;
    @FXML private Label                     lblUserCount;

    // ── Bảng Chờ duyệt ────────────────────────────────────────
    @FXML private TableView<Auction>            tablePending;
    @FXML private TableColumn<Auction, String>  colPendingItem;
    @FXML private TableColumn<Auction, String>  colPendingSeller;
    @FXML private TableColumn<Auction, String>  colPendingPrice;
    @FXML private TableColumn<Auction, String>  colPendingAction;
    @FXML private Label                         lblPendingCount;

    // ── Bảng Live ─────────────────────────────────────────────
    @FXML private TableView<Auction>            tableLiveAuctions;
    @FXML private TableColumn<Auction, String>  colLiveItem;
    @FXML private TableColumn<Auction, String>  colLiveSeller;
    @FXML private TableColumn<Auction, String>  colLivePrice;
    @FXML private TableColumn<Auction, String>  colLiveBids;
    @FXML private TableColumn<Auction, String>  colLiveTime;
    @FXML private TableColumn<Auction, String>  colLiveStatus;
    @FXML private Label                         lblLiveCount;

    // ── State ─────────────────────────────────────────────────
    private final ObservableList<User>    userList    = FXCollections.observableArrayList();
    private final ObservableList<Auction> pendingList = FXCollections.observableArrayList();
    private final ObservableList<Auction> liveList    = FXCollections.observableArrayList();
    private List<User>    allUsers    = List.of();
    private List<Auction> allAuctions = List.of();

    private static final NumberFormat VND =
            NumberFormat.getInstance(new Locale("vi", "VN"));
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm dd/MM");

    // ─────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        // Kiểm tra quyền Admin dùng instanceof thay vì so sánh string
        User me = ServerConnection.getInstance().getCurrentUser();
        if (!(me instanceof Admin)) {
            SceneManager.switchScene("login.fxml");
            return;
        }

        setupUserTable();
        setupPendingTable();
        setupLiveTable();
        setupSearch();
        loadDashboardData();
    }

    // ── Cài đặt cột ──────────────────────────────────────────

    private void setupUserTable() {
        colUserName.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getEmail()));
        colUsername.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getUsername()));
        colRole.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getRole()));
        colBalance.setCellValueFactory(d -> {
            User u = d.getValue();
            if (u instanceof Bidder b)
                return new SimpleStringProperty(VND.format((long) b.getBalance()) + "đ");
            return new SimpleStringProperty("—");
        });

        // Cột Khoá/Mở khoá — cập nhật ngay khi SERVER xác nhận BAN_USER_SUCCESS
        colUserAction.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button();
            {
                btn.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    handleBanUser(u);
                });
            }
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    return;
                }
                User u = getTableView().getItems().get(getIndex());
                refreshBtn(u);
                setGraphic(btn);
            }
            private void refreshBtn(User u) {
                if (u.isActive()) {
                    btn.setText("Khoá");
                    btn.setStyle("-fx-background-color: rgba(255,100,100,0.25); -fx-text-fill: #ff6b6b;" +
                            " -fx-background-radius: 4; -fx-font-size: 11px; -fx-cursor: hand;");
                } else {
                    btn.setText("Mở khoá");
                    btn.setStyle("-fx-background-color: rgba(74,222,128,0.25); -fx-text-fill: #4ade80;" +
                            " -fx-background-radius: 4; -fx-font-size: 11px; -fx-cursor: hand;");
                }
            }
        });

        tableUsers.setItems(userList);
        applyTableStyle(tableUsers);
    }

    private void setupPendingTable() {
        colPendingItem.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getItem() != null ? d.getValue().getItem().getName() : "?"));
        colPendingSeller.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getSeller() != null ? d.getValue().getSeller().getUsername() : "?"));
        colPendingPrice.setCellValueFactory(d -> new SimpleStringProperty(
                VND.format((long) d.getValue().getCurrentPrice()) + "đ"));

        colPendingAction.setCellFactory(col -> new TableCell<>() {
            private final Button btnApprove = new Button("✓ Duyệt");
            private final Button btnReject  = new Button("✗ Từ chối");
            private final HBox   box        = new HBox(4, btnApprove, btnReject);
            {
                btnApprove.setStyle("-fx-background-color: rgba(74,222,128,0.25); -fx-text-fill: #4ade80;" +
                        " -fx-background-radius: 4; -fx-font-size: 10px; -fx-cursor: hand;");
                btnReject.setStyle("-fx-background-color: rgba(255,100,100,0.25); -fx-text-fill: #ff6b6b;" +
                        " -fx-background-radius: 4; -fx-font-size: 10px; -fx-cursor: hand;");
                btnApprove.setOnAction(e -> {
                    Auction a = getTableView().getItems().get(getIndex());
                    handleApprove(a);
                });
                btnReject.setOnAction(e -> {
                    Auction a = getTableView().getItems().get(getIndex());
                    handleReject(a);
                });
            }
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        tablePending.setItems(pendingList);
        applyTableStyle(tablePending);
    }

    private void setupLiveTable() {
        colLiveItem.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getItem() != null ? d.getValue().getItem().getName() : "?"));
        colLiveSeller.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getSeller() != null ? d.getValue().getSeller().getUsername() : "?"));
        colLivePrice.setCellValueFactory(d -> new SimpleStringProperty(
                VND.format((long) d.getValue().getCurrentPrice()) + "đ"));
        colLiveBids.setCellValueFactory(d -> new SimpleStringProperty(
                String.valueOf(d.getValue().getBids() != null ? d.getValue().getBids().size() : 0)));
        colLiveTime.setCellValueFactory(d -> {
            LocalDateTime end = d.getValue().getEndTime();
            if (end == null) return new SimpleStringProperty("—");
            long mins = Duration.between(LocalDateTime.now(), end).toMinutes();
            return new SimpleStringProperty(mins < 0 ? "Đã kết thúc" : mins + " phút");
        });
        colLiveStatus.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getStatus() != null ? d.getValue().getStatus().name() : "?"));

        tableLiveAuctions.setItems(liveList);
        applyTableStyle(tableLiveAuctions);
    }

    private void setupSearch() {
        txtSearchAdmin.textProperty().addListener(
                (obs, old, val) -> applySearch(val.trim().toLowerCase()));
    }

    // ── Load dữ liệu từ server ────────────────────────────────

    private void loadDashboardData() {
        ServerConnection conn = ServerConnection.getInstance();

        // Đăng ký callback nhận danh sách user
        conn.setAdminUserCallback(users -> Platform.runLater(() -> {
            allUsers = users;
            fillUserTable(users);
            fillStats();
        }));

        // Đăng ký callback nhận danh sách auction
        conn.setAdminAuctionCallback(auctions -> Platform.runLater(() -> {
            allAuctions = auctions;
            fillPendingTable(auctions);
            fillLiveTable(auctions);
            fillStats();
        }));

        // Đăng ký callback khi approve thành công
        conn.setAdminApproveCallback(auctionId -> Platform.runLater(() -> {
            pendingList.removeIf(a -> a.getId().equals(auctionId));
            // Tìm auction trong allAuctions để thêm vào liveList
            allAuctions.stream()
                    .filter(a -> a.getId().equals(auctionId))
                    .findFirst()
                    .ifPresent(a -> {
                        a.setStatus(AuctionStatus.OPEN);
                        liveList.add(a);
                    });
            fillStats();
        }));

        // Đăng ký callback khi reject thành công
        conn.setAdminRejectCallback(auctionId -> Platform.runLater(() -> {
            pendingList.removeIf(a -> a.getId().equals(auctionId));
            fillStats();
        }));

        // Đăng ký callback khi ban/unban thành công
        conn.setAdminBanCallback(userId -> Platform.runLater(() -> {
            userList.stream()
                    .filter(u -> u.getId().equals(userId))
                    .findFirst()
                    .ifPresent(u -> u.setActive(!u.isActive()));
            tableUsers.refresh();
        }));

        try {
            conn.sendMessage(new Message("GET_ALL_USERS",    null));
            conn.sendMessage(new Message("GET_ALL_AUCTIONS", null));
        } catch (Exception e) {
            showAlert("Lỗi kết nối", "Không thể tải dữ liệu từ server: " + e.getMessage());
        }
    }

    // ── Điền dữ liệu lên UI ───────────────────────────────────

    private void fillStats() {
        lblTotalUsers.setText(String.valueOf(allUsers.size()));
        lblUserCount.setText("(" + allUsers.size() + ")");

        long pendingCount = allAuctions.stream()
                .filter(a -> a.getStatus() == AuctionStatus.PENDING_APPROVAL).count();
        // [FIX 3] Bổ sung RUNNING: phiên đang có người đặt thầu có status RUNNING, không chỉ OPEN
        long liveCount = allAuctions.stream()
                .filter(a -> a.getStatus() == AuctionStatus.OPEN
                        || a.getStatus() == AuctionStatus.RUNNING)
                .count();

        lblPending.setText(String.valueOf(pendingCount));
        lblPendingCount.setText("(" + pendingCount + ")");
        lblLive.setText(String.valueOf(liveCount));
        lblLiveCount.setText("(" + liveCount + " live)");
    }

    private void fillUserTable(List<User> users) {
        userList.setAll(users);
    }

    private void fillPendingTable(List<Auction> auctions) {
        pendingList.setAll(auctions.stream()
                .filter(a -> a.getStatus() == AuctionStatus.PENDING_APPROVAL)
                .collect(Collectors.toList()));
    }

    private void fillLiveTable(List<Auction> auctions) {
        // [FIX 3] Bổ sung RUNNING: phiên đang có người đặt thầu có status RUNNING
        liveList.setAll(auctions.stream()
                .filter(a -> a.getStatus() == AuctionStatus.OPEN
                        || a.getStatus() == AuctionStatus.RUNNING)
                .collect(Collectors.toList()));
    }

    // ── Tìm kiếm ─────────────────────────────────────────────

    private void applySearch(String keyword) {
        if (keyword.isEmpty()) {
            fillUserTable(allUsers);
            fillPendingTable(allAuctions);
            return;
        }
        fillUserTable(allUsers.stream()
                .filter(u -> u.getUsername().toLowerCase().contains(keyword)
                        || u.getEmail().toLowerCase().contains(keyword))
                .collect(Collectors.toList()));
        pendingList.setAll(allAuctions.stream()
                .filter(a -> a.getStatus() == AuctionStatus.PENDING_APPROVAL)
                .filter(a -> a.getItem() != null
                        && a.getItem().getName().toLowerCase().contains(keyword))
                .collect(Collectors.toList()));
    }

    // ── Thao tác Admin ────────────────────────────────────────

    /**
     * Gửi APPROVE_AUCTION lên server.
     * UI chỉ cập nhật sau khi nhận APPROVE_AUCTION_SUCCESS qua callback.
     */
    private void handleApprove(Auction auction) {
        try {
            ServerConnection.getInstance().sendMessage(
                    new Message("APPROVE_AUCTION", auction.getId()));
        } catch (Exception e) {
            showAlert("Lỗi", "Không thể phê duyệt: " + e.getMessage());
        }
    }

    /**
     * Gửi REJECT_AUCTION lên server.
     * UI chỉ cập nhật sau khi nhận REJECT_AUCTION_SUCCESS qua callback.
     */
    private void handleReject(Auction auction) {
        try {
            ServerConnection.getInstance().sendMessage(
                    new Message("REJECT_AUCTION", auction.getId()));
        } catch (Exception e) {
            showAlert("Lỗi", "Không thể từ chối: " + e.getMessage());
        }
    }

    /**
     * Gửi BAN_USER lên server.
     * UI toggle active chỉ sau khi nhận BAN_USER_SUCCESS qua callback.
     */
    private void handleBanUser(User user) {
        try {
            ServerConnection.getInstance().sendMessage(
                    new Message("BAN_USER", user.getId()));
        } catch (Exception e) {
            showAlert("Lỗi", "Không thể thay đổi trạng thái user: " + e.getMessage());
        }
    }

    // ── Sidebar navigation ────────────────────────────────────

    @FXML private void handleShowDashboard() {
        setActiveButton(btnDashboard);
        loadDashboardData();
    }

    @FXML private void handleShowUsers() {
        setActiveButton(btnUsers);
        tableUsers.scrollTo(0);
    }

    @FXML private void handleShowApprovals() {
        setActiveButton(btnApprovals);
        tablePending.scrollTo(0);
    }

    @FXML private void handleShowLive() {
        setActiveButton(btnLive);
        // [FIX 2] Restore lại ObservableList gốc để tableLiveAuctions hiển thị đúng live data
        // sau khi người dùng có thể đã xem History (History thay thế items bằng list tạm)
        tableLiveAuctions.setItems(liveList);
        tableLiveAuctions.scrollTo(0);
    }

    @FXML private void handleShowHistory() {
        setActiveButton(btnHistory);
        // [FIX 2] Dùng ObservableList riêng thay vì ghi đè liveList để tránh mất dữ liệu live.
        // Bổ sung AuctionStatus.PAID vào điều kiện lọc (trước chỉ có FINISHED và CANCELLED).
        List<Auction> history = allAuctions.stream()
                .filter(a -> a.getStatus() == AuctionStatus.FINISHED
                        || a.getStatus() == AuctionStatus.CANCELLED
                        || a.getStatus() == AuctionStatus.PAID)
                .collect(Collectors.toList());
        tableLiveAuctions.setItems(FXCollections.observableArrayList(history));
        tableLiveAuctions.scrollTo(0);
    }

    @FXML private void handleRefreshUsers() {
        try {
            ServerConnection.getInstance().sendMessage(new Message("GET_ALL_USERS", null));
        } catch (Exception e) {
            showAlert("Lỗi", e.getMessage());
        }
    }

    @FXML private void handleRefreshPending() {
        try {
            ServerConnection.getInstance().sendMessage(new Message("GET_ALL_AUCTIONS", null));
        } catch (Exception e) {
            showAlert("Lỗi", e.getMessage());
        }
    }

    @FXML private void handleRefreshLive() {
        try {
            ServerConnection.getInstance().sendMessage(new Message("GET_ALL_AUCTIONS", null));
        } catch (Exception e) {
            showAlert("Lỗi", e.getMessage());
        }
    }

    @FXML private void handleLogout() {
        try {
            ServerConnection.getInstance().sendMessage(new Message("LOGOUT", null));
        } catch (Exception ignored) {}
        SceneManager.switchScene("login.fxml");
    }

    // ── Helpers ───────────────────────────────────────────────

    private static final String STYLE_ACTIVE =
            "-fx-background-color: rgba(255,255,255,0.15); -fx-text-fill: white;" +
                    " -fx-font-size: 13px; -fx-font-weight: bold; -fx-alignment: CENTER-LEFT;" +
                    " -fx-padding: 10 14; -fx-background-radius: 6; -fx-cursor: hand;";
    private static final String STYLE_INACTIVE =
            "-fx-background-color: transparent; -fx-text-fill: white;" +
                    " -fx-font-size: 13px; -fx-font-weight: bold; -fx-alignment: CENTER-LEFT;" +
                    " -fx-padding: 10 14; -fx-cursor: hand;";

    private void setActiveButton(Button active) {
        for (Button b : new Button[]{btnDashboard, btnUsers, btnApprovals, btnLive, btnHistory}) {
            b.setStyle(b == active ? STYLE_ACTIVE : STYLE_INACTIVE);
        }
    }

    private <T> void applyTableStyle(TableView<T> table) {
        table.setStyle("-fx-background-color: transparent;" +
                " -fx-border-color: rgba(255,255,255,0.10); -fx-border-radius: 6;");
        Label placeholder = new Label("Không có dữ liệu");
        placeholder.setTextFill(Color.web("rgba(255,255,255,0.40)"));
        table.setPlaceholder(placeholder);
    }

    private void showAlert(String title, String msg) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(msg);
            alert.showAndWait();
        });
    }
}