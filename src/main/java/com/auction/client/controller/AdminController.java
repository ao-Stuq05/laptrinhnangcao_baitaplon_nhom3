package com.auction.client.controller;

import com.auction.client.SceneManager;
import com.auction.client.network.ServerConnection;
import com.auction.shared.model.*;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class AdminController {

    // ── Sidebar buttons ──────────────────────────────────────────────────────
    @FXML private Button btnDashboard;
    @FXML private Button btnUsers;
    @FXML private Button btnApprovals;
    @FXML private Button btnLive;
    @FXML private Button btnHistory;
    @FXML private Button btnLogout;

    // ── Search ───────────────────────────────────────────────────────────────
    @FXML private TextField txtSearchAdmin;

    // ── Stat cards ───────────────────────────────────────────────────────────
    @FXML private Label lblTotalUsers;
    @FXML private Label lblPending;
    @FXML private Label lblLive;

    // ── Users table ──────────────────────────────────────────────────────────
    @FXML private Label                      lblUserCount;
    @FXML private TableView<User>            tableUsers;
    @FXML private TableColumn<User, String>  colUserName;
    @FXML private TableColumn<User, String>  colUsername;
    @FXML private TableColumn<User, String>  colRole;
    @FXML private TableColumn<User, String>  colBalance;
    @FXML private TableColumn<User, Void>    colUserAction;

    // ── Pending auctions table ────────────────────────────────────────────────
    @FXML private Label                         lblPendingCount;
    @FXML private TableView<Auction>            tablePending;
    @FXML private TableColumn<Auction, String>  colPendingItem;
    @FXML private TableColumn<Auction, String>  colPendingSeller;
    @FXML private TableColumn<Auction, String>  colPendingPrice;
    @FXML private TableColumn<Auction, Void>    colPendingAction;

    // ── Live auctions table ───────────────────────────────────────────────────
    @FXML private Label                         lblLiveCount;
    @FXML private TableView<Auction>            tableLiveAuctions;
    @FXML private TableColumn<Auction, String>  colLiveItem;
    @FXML private TableColumn<Auction, String>  colLiveSeller;
    @FXML private TableColumn<Auction, String>  colLivePrice;
    @FXML private TableColumn<Auction, String>  colLiveBids;
    @FXML private TableColumn<Auction, String>  colLiveTime;

    // ── Local data ────────────────────────────────────────────────────────────
    private List<User>    allUsers    = List.of();
    private List<Auction> allAuctions = List.of();

    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("dd/MM HH:mm");

    // ─────────────────────────────────────────────────────────────────────────
    //  INITIALIZE
    // ─────────────────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        setupUserTable();
        setupPendingTable();
        setupLiveTable();
        registerCallbacks();
        loadAll();

        txtSearchAdmin.textProperty().addListener((obs, o, n) -> applySearch(n));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  TABLE SETUP
    // ─────────────────────────────────────────────────────────────────────────
    private void setupUserTable() {
        colUserName.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(d.getValue().getEmail()));
        colUsername.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(d.getValue().getUsername()));
        colRole.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(d.getValue().getRole()));

        colBalance.setCellValueFactory(d -> {
            User u = d.getValue();
            String bal = (u instanceof Bidder b)
                    ? String.format("%,.0f đ", b.getBalance()) : "—";
            return new javafx.beans.property.SimpleStringProperty(bal);
        });

        // Action column: Ban / Unban
        colUserAction.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button();
            {
                btn.setOnAction(e -> handleBanUser(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                User u = getTableView().getItems().get(getIndex());
                if (u instanceof Admin) { setGraphic(null); return; }
                if (u.isActive()) {
                    btn.setText("🔒 Khóa");
                    btn.setStyle(
                            "-fx-background-color: rgba(220,38,38,0.75); -fx-text-fill: white;" +
                                    "-fx-background-radius: 5; -fx-border-radius: 5;" +
                                    "-fx-border-color: rgba(255,100,100,0.35); -fx-border-width: 1;" +
                                    "-fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 5 12; -fx-cursor: hand;");
                } else {
                    btn.setText("🔓 Mở khóa");
                    btn.setStyle(
                            "-fx-background-color: rgba(34,197,94,0.75); -fx-text-fill: white;" +
                                    "-fx-background-radius: 5; -fx-border-radius: 5;" +
                                    "-fx-border-color: rgba(100,255,150,0.35); -fx-border-width: 1;" +
                                    "-fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 5 12; -fx-cursor: hand;");
                }
                setGraphic(btn);
                setAlignment(Pos.CENTER);
            }
        });

        // Row factory: alternate color + banned đỏ
        tableUsers.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(User u, boolean empty) {
                super.updateItem(u, empty);
                if (empty || u == null) {
                    setStyle("-fx-background-color: transparent;");
                    return;
                }
                if (!u.isActive()) {
                    // Banned: đỏ nhạt
                    setStyle("-fx-background-color: rgba(220,38,38,0.18);");
                } else if (getIndex() % 2 == 0) {
                    setStyle("-fx-background-color: rgba(255,255,255,0.04);");
                } else {
                    setStyle("-fx-background-color: rgba(0,0,0,0.15);");
                }
            }
        });

        applyTableStyle(tableUsers, "Đang tải dữ liệu...");
        applyColumnHeaderStyle(tableUsers);
        applyCellTextStyle(tableUsers);
    }

    private void setupPendingTable() {
        colPendingItem.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(
                        d.getValue().getItem().getName()));
        colPendingSeller.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(
                        d.getValue().getSeller() != null
                                ? d.getValue().getSeller().getUsername() : "—"));
        colPendingPrice.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(
                        String.format("%,.0f đ", d.getValue().getItem().getBasePrice())));

        // Action: Duyệt + Từ chối
        colPendingAction.setCellFactory(col -> new TableCell<>() {
            private final Button btnApprove = new Button("✅ Duyệt");
            private final Button btnReject  = new Button("❌ Từ chối");
            private final HBox   box        = new HBox(6, btnApprove, btnReject);
            {
                box.setAlignment(Pos.CENTER);
                btnApprove.setStyle(
                        "-fx-background-color: rgba(34,197,94,0.75); -fx-text-fill: white;" +
                                "-fx-background-radius: 5; -fx-border-radius: 5;" +
                                "-fx-border-color: rgba(100,255,150,0.35); -fx-border-width: 1;" +
                                "-fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 5 10; -fx-cursor: hand;");
                btnReject.setStyle(
                        "-fx-background-color: rgba(220,38,38,0.75); -fx-text-fill: white;" +
                                "-fx-background-radius: 5; -fx-border-radius: 5;" +
                                "-fx-border-color: rgba(255,100,100,0.35); -fx-border-width: 1;" +
                                "-fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 5 10; -fx-cursor: hand;");
                btnApprove.setOnAction(e -> handleApprove(getTableView().getItems().get(getIndex())));
                btnReject.setOnAction(e  -> handleReject(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                setGraphic(box);
                setStyle("-fx-background-color: transparent;");
            }
        });

        // Alternate rows
        tablePending.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Auction a, boolean empty) {
                super.updateItem(a, empty);
                if (empty || a == null) {
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setStyle(getIndex() % 2 == 0
                            ? "-fx-background-color: rgba(255,255,255,0.04);"
                            : "-fx-background-color: rgba(0,0,0,0.15);");
                }
            }
        });

        applyTableStyle(tablePending, "Không có phiên chờ duyệt");
        applyColumnHeaderStyle(tablePending);
        applyCellTextStyle(tablePending);
    }

    private void setupLiveTable() {
        colLiveItem.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(
                        d.getValue().getItem().getName()));
        colLiveSeller.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(
                        d.getValue().getSeller() != null
                                ? d.getValue().getSeller().getUsername() : "—"));
        colLivePrice.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(
                        String.format("%,.0f đ", d.getValue().getCurrentPrice())));
        colLiveBids.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(
                        String.valueOf(d.getValue().getBidCount())));
        colLiveTime.setCellValueFactory(d -> {
            LocalDateTime end = d.getValue().getEndTime();
            String txt = (end != null) ? end.format(DT_FMT) : "—";
            return new javafx.beans.property.SimpleStringProperty(txt);
        });

        // Alternate rows với màu xanh nhạt cho live
        tableLiveAuctions.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Auction a, boolean empty) {
                super.updateItem(a, empty);
                if (empty || a == null) {
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setStyle(getIndex() % 2 == 0
                            ? "-fx-background-color: rgba(74,222,128,0.05);"
                            : "-fx-background-color: rgba(0,0,0,0.15);");
                }
            }
        });

        applyTableStyle(tableLiveAuctions, "Không có phiên đang diễn ra");
        applyColumnHeaderStyle(tableLiveAuctions);
        applyCellTextStyle(tableLiveAuctions);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  TABLE STYLING HELPERS  (không dùng CSS file)
    // ─────────────────────────────────────────────────────────────────────────

    /** Container + placeholder */
    private <T> void applyTableStyle(TableView<T> tv, String placeholderText) {
        tv.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-border-color: rgba(255,255,255,0.10);" +
                        "-fx-border-radius: 8;" +
                        "-fx-background-radius: 8;" +
                        "-fx-table-cell-border-color: rgba(255,255,255,0.05);");

        // Placeholder label
        Label ph = new Label(placeholderText);
        ph.setStyle("-fx-text-fill: rgba(255,255,255,0.40); -fx-font-size: 13px; -fx-font-style: italic;");
        tv.setPlaceholder(ph);

        // Ẩn focus highlight của TableView
        tv.focusedProperty().addListener((obs, o, focused) ->
                tv.setStyle(tv.getStyle()));
    }

    /** Style header của từng cột: nền đậm, chữ trắng bold */
    private <T> void applyColumnHeaderStyle(TableView<T> tv) {
        for (TableColumn<T, ?> col : tv.getColumns()) {
            col.setStyle(
                    "-fx-background-color: rgba(0,0,0,0.35);" +
                            "-fx-text-fill: rgba(255,255,255,0.80);" +
                            "-fx-font-size: 12px;" +
                            "-fx-font-weight: bold;" +
                            "-fx-alignment: CENTER-LEFT;" +
                            "-fx-border-color: rgba(255,255,255,0.08);" +
                            "-fx-border-width: 0 1 1 0;" +
                            "-fx-padding: 8 10;");
        }
    }

    /** Style text cell: chữ trắng, căn lề, padding đều */
    private <T> void applyCellTextStyle(TableView<T> tv) {
        // Dùng default cell factory wrapper để style từng cell
        for (TableColumn<T, ?> col : tv.getColumns()) {
            // Chỉ apply cho String columns (Void columns tự xử lý trong setCellFactory)
            if (col.getCellObservableValue(0) == null) continue;
            @SuppressWarnings("unchecked")
            TableColumn<T, String> strCol = (TableColumn<T, String>) col;
            strCol.setCellFactory(c -> new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setStyle("-fx-background-color: transparent;");
                    } else {
                        setText(item);
                        setStyle(
                                "-fx-text-fill: rgba(255,255,255,0.88);" +
                                        "-fx-font-size: 13px;" +
                                        "-fx-padding: 8 10;" +
                                        "-fx-background-color: transparent;" +
                                        "-fx-border-color: transparent;");
                    }
                }
            });
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  SERVER CALLBACKS
    // ─────────────────────────────────────────────────────────────────────────
    private void registerCallbacks() {
        ServerConnection sc = ServerConnection.getInstance();

        sc.setAdminUserCallback(users -> {
            allUsers = users;
            refreshUserTable(users);
            lblTotalUsers.setText(String.valueOf(
                    users.stream().filter(u -> !(u instanceof Admin)).count()));
        });

        sc.setAdminAuctionCallback(auctions -> {
            allAuctions = auctions;
            refreshAuctionTables(auctions);
        });

        sc.setAdminApproveCallback(auctionId -> {
            if (auctionId == null) return;
            allAuctions = allAuctions.stream()
                    .map(a -> { if (a.getId().equals(auctionId)) a.setStatus(AuctionStatus.OPEN); return a; })
                    .collect(Collectors.toList());
            refreshAuctionTables(allAuctions);
            showInfo("Đã duyệt phiên", "Phiên đấu giá đã được mở.");
        });

        sc.setAdminRejectCallback(auctionId -> {
            if (auctionId == null) return;
            allAuctions = allAuctions.stream()
                    .filter(a -> !a.getId().equals(auctionId))
                    .collect(Collectors.toList());
            refreshAuctionTables(allAuctions);
            showInfo("Đã từ chối", "Phiên đấu giá đã bị từ chối và hủy bỏ.");
        });

        sc.setAdminBanCallback(userId -> {
            if (userId == null) return;
            allUsers = allUsers.stream()
                    .map(u -> {
                        if (u.getId().equals(userId)) u.setActive(!u.isActive());
                        return u;
                    })
                    .collect(Collectors.toList());
            refreshUserTable(allUsers);
            lblTotalUsers.setText(String.valueOf(
                    allUsers.stream().filter(u -> !(u instanceof Admin)).count()));
        });

        sc.setAdminNewPendingCallback(auction -> {
            if (auction == null) return;
            boolean exists = allAuctions.stream().anyMatch(a -> a.getId().equals(auction.getId()));
            if (!exists) {
                java.util.List<Auction> updated = new java.util.ArrayList<>(allAuctions);
                updated.add(0, auction);
                allAuctions = updated;
                refreshAuctionTables(allAuctions);
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  LOAD / REFRESH
    // ─────────────────────────────────────────────────────────────────────────
    private void loadAll() {
        try {
            ServerConnection sc = ServerConnection.getInstance();
            sc.sendMessage(new Message("GET_ALL_USERS",    null));
            sc.sendMessage(new Message("GET_ALL_AUCTIONS", null));
        } catch (IOException e) {
            showError("Lỗi kết nối", "Không thể tải dữ liệu từ server: " + e.getMessage());
        }
    }

    private void refreshUserTable(List<User> users) {
        tableUsers.getItems().setAll(users);
        lblUserCount.setText("(" + users.size() + ")");
    }

    private void refreshAuctionTables(List<Auction> auctions) {
        List<Auction> pending = auctions.stream()
                .filter(a -> a.getStatus() == AuctionStatus.PENDING_APPROVAL)
                .collect(Collectors.toList());
        List<Auction> live = auctions.stream()
                .filter(a -> a.getStatus() == AuctionStatus.OPEN
                        || a.getStatus() == AuctionStatus.RUNNING)
                .collect(Collectors.toList());

        tablePending.getItems().setAll(pending);
        tableLiveAuctions.getItems().setAll(live);

        lblPendingCount.setText("(" + pending.size() + ")");
        lblLiveCount.setText("(" + live.size() + " live)");
        lblPending.setText(String.valueOf(pending.size()));
        lblLive.setText(String.valueOf(live.size()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  ACTIONS
    // ─────────────────────────────────────────────────────────────────────────
    private void handleBanUser(User u) {
        String action = u.isActive() ? "KHÓA" : "MỞ KHÓA";
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Bạn có chắc muốn " + action + " tài khoản \"" + u.getUsername() + "\"?",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(action + " TÀI KHOẢN");
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                try { ServerConnection.getInstance().sendMessage(new Message("BAN_USER", u.getId())); }
                catch (IOException e) { showError("Lỗi", e.getMessage()); }
            }
        });
    }

    private void handleApprove(Auction a) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Duyệt phiên đấu giá \"" + a.getItem().getName() + "\"?",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("XÁC NHẬN DUYỆT PHIÊN");
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                try { ServerConnection.getInstance().sendMessage(new Message("APPROVE_AUCTION", a.getId())); }
                catch (IOException e) { showError("Lỗi", e.getMessage()); }
            }
        });
    }

    private void handleReject(Auction a) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Từ chối phiên \"" + a.getItem().getName() + "\"? Phiên sẽ bị hủy.",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("XÁC NHẬN TỪ CHỐI");
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                try { ServerConnection.getInstance().sendMessage(new Message("REJECT_AUCTION", a.getId())); }
                catch (IOException e) { showError("Lỗi", e.getMessage()); }
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  SEARCH
    // ─────────────────────────────────────────────────────────────────────────
    private void applySearch(String query) {
        if (query == null || query.isBlank()) {
            refreshUserTable(allUsers);
            refreshAuctionTables(allAuctions);
            return;
        }
        String q = query.toLowerCase();
        List<User> filteredUsers = allUsers.stream()
                .filter(u -> u.getUsername().toLowerCase().contains(q)
                        || u.getEmail().toLowerCase().contains(q))
                .collect(Collectors.toList());
        tableUsers.getItems().setAll(filteredUsers);
        lblUserCount.setText("(" + filteredUsers.size() + ")");

        List<Auction> filteredPending = allAuctions.stream()
                .filter(a -> a.getStatus() == AuctionStatus.PENDING_APPROVAL)
                .filter(a -> a.getItem().getName().toLowerCase().contains(q)
                        || (a.getSeller() != null && a.getSeller().getUsername().toLowerCase().contains(q)))
                .collect(Collectors.toList());
        tablePending.getItems().setAll(filteredPending);

        List<Auction> filteredLive = allAuctions.stream()
                .filter(a -> a.getStatus() == AuctionStatus.OPEN || a.getStatus() == AuctionStatus.RUNNING)
                .filter(a -> a.getItem().getName().toLowerCase().contains(q)
                        || (a.getSeller() != null && a.getSeller().getUsername().toLowerCase().contains(q)))
                .collect(Collectors.toList());
        tableLiveAuctions.getItems().setAll(filteredLive);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  FXML HANDLERS — Sidebar
    // ─────────────────────────────────────────────────────────────────────────
    @FXML private void handleShowDashboard()  { highlightNav(btnDashboard); loadAll(); }
    @FXML private void handleShowUsers()      { highlightNav(btnUsers);     tableUsers.scrollTo(0); }
    @FXML private void handleShowApprovals()  { highlightNav(btnApprovals); tablePending.scrollTo(0); }
    @FXML private void handleShowLive()       { highlightNav(btnLive);      tableLiveAuctions.scrollTo(0); }
    @FXML private void handleShowHistory()    { highlightNav(btnHistory);   loadAll(); }

    @FXML private void handleRefreshUsers()   { try { ServerConnection.getInstance().sendMessage(new Message("GET_ALL_USERS",    null)); } catch (IOException ignored) {} }
    @FXML private void handleRefreshPending() { try { ServerConnection.getInstance().sendMessage(new Message("GET_ALL_AUCTIONS", null)); } catch (IOException ignored) {} }
    @FXML private void handleRefreshLive()    { try { ServerConnection.getInstance().sendMessage(new Message("GET_ALL_AUCTIONS", null)); } catch (IOException ignored) {} }

    @FXML
    private void handleLogout() {
        try { ServerConnection.getInstance().close(); } catch (IOException ignored) {}
        SceneManager.switchScene("login.fxml");
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────────────────────────────────
    private void highlightNav(Button active) {
        String activeStyle = "-fx-background-color: rgba(255,255,255,0.15); -fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold; -fx-alignment: CENTER-LEFT; -fx-padding: 10 14; -fx-background-radius: 6; -fx-cursor: hand;";
        String normalStyle = "-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold; -fx-alignment: CENTER-LEFT; -fx-padding: 10 14; -fx-cursor: hand;";
        for (Button b : new Button[]{btnDashboard, btnUsers, btnApprovals, btnLive, btnHistory}) {
            b.setStyle(b == active ? activeStyle : normalStyle);
        }
    }

    private void showInfo(String header, String content) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, content);
        a.setHeaderText(header);
        a.showAndWait();
    }

    private void showError(String header, String content) {
        Alert a = new Alert(Alert.AlertType.ERROR, content);
        a.setHeaderText(header);
        a.showAndWait();
    }
}