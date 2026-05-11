package com.auction.shared.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.io.Serializable;

public class Auction extends Entity implements Serializable {

    private static final long serialVersionUID = 1L;

    // ── Fields ────────────────────────────────────────────────
    private Item item;
    private Seller seller;
    private AuctionStatus status;
    private double currentPrice;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Bidder highestBidder;
    private Bidder winner;
    private List<BidTransaction> bidHistory;

    // Observer Pattern — thread-safe
    private transient List<AuctionObserver> observers = new CopyOnWriteArrayList<>();

    // ── Constructors ──────────────────────────────────────────

    /**
     * Constructor 1: Tạo phiên mới đầy đủ (có seller)
     * Dùng khi Seller tạo phiên đấu giá
     */
    public Auction(String id, Item item, Seller seller,
                   LocalDateTime startTime, LocalDateTime endTime) {
        super(id);
        this.item         = item;
        this.seller       = seller;   // ← gán seller
        this.startTime    = startTime;
        this.endTime      = endTime;
        this.currentPrice = item.getBasePrice();
        this.status       = AuctionStatus.OPEN;
        this.bidHistory   = new CopyOnWriteArrayList<>();
    }

    /**
     * Constructor 2: Load từ Database — đầy đủ tất cả fields
     * AuctionDAO.mapRowToAuction() dùng constructor này
     */
    public Auction(String id, Item item, Seller seller,
                   AuctionStatus status, double currentPrice,
                   LocalDateTime start, LocalDateTime end,
                   Bidder leadingBidder, Bidder winner) {
        super(id);
        this.item          = item;
        this.seller        = seller;
        this.status        = status;
        this.currentPrice  = currentPrice;
        this.startTime     = start;
        this.endTime       = end;
        this.highestBidder = leadingBidder;
        this.winner        = winner;
        this.bidHistory    = new CopyOnWriteArrayList<>();
    }

    /**
     * Constructor 3: Rút gọn — Seller chỉ truyền item + endTime
     */
    public Auction(Item item, LocalDateTime endTime) {
        // Tự tạo ID duy nhất và gọi lại Constructor 1 bằng từ khóa this()
        // Hoặc gọi trực tiếp super()
        super("AUC-" + System.currentTimeMillis());

        this.item = item;
        this.startTime = LocalDateTime.now(); // Tự động lấy giờ hiện tại
        this.endTime = endTime;
        this.currentPrice = item.getBasePrice();
        this.status = AuctionStatus.OPEN;

        // Đã sửa lỗi: Khởi tạo bidHistory để tránh NullPointerException
        this.bidHistory = new CopyOnWriteArrayList<>();
    }

    // ── Core methods ──────────────────────────────────────────

    public void startAuction() {
        this.status = AuctionStatus.RUNNING;
        System.out.println(">> Phiên đấu giá [" + item.getName() + "] đã BẮT ĐẦU!");
    }

    public void cancel() {
        if (status == AuctionStatus.OPEN || status == AuctionStatus.RUNNING) {
            this.status = AuctionStatus.CANCELLED;
            System.out.println("Phiên " + getId() + " đã bị hủy.");
        } else {
            System.out.println("Lỗi: Không thể hủy phiên đã kết thúc.");
        }
    }

    // Đã thêm: Hàm kết thúc phiên đấu giá
    public void endAuction() {
        if (this.status == AuctionStatus.FINISHED || this.status == AuctionStatus.CANCELLED) {
            return; // Nếu đã kết thúc hoặc bị hủy rồi thì thôi
        }

        this.status = AuctionStatus.FINISHED;

        // Nếu có người từng đặt giá, người đó chính là Winner
        if (highestBidder != null) {
            this.winner = highestBidder;
            System.out.println(">> Phiên kết thúc! Người thắng: " + winner.getUsername() + " với giá " + currentPrice);
        } else {
            System.out.println(">> Phiên kết thúc! Không có ai đặt giá.");
        }
    }

    public synchronized void placeBid(Bidder bidder, double amount) {
        if (status != AuctionStatus.OPEN && status != AuctionStatus.RUNNING) {
            System.out.println("Lỗi: Phiên không mở!");
            return;
        }
        if (amount <= currentPrice) {
            System.out.println("Lỗi: Giá phải cao hơn " + currentPrice);
            return;
        }

        currentPrice       = amount;
        highestBidder      = bidder;
        status             = AuctionStatus.RUNNING;

        BidTransaction tx = new BidTransaction(bidder, amount, getId());
        bidHistory.add(tx);

        System.out.println("✓ " + bidder.getUsername() + " đặt giá " + amount);
        notifyObservers(tx);
    }

    // ── Observer Pattern ──────────────────────────────────────

    public void addObserver(AuctionObserver observer) {
        if (observer != null && !observers.contains(observer)) {
            observers.add(observer);
        }
    }

    public void removeObserver(AuctionObserver observer) {
        observers.remove(observer);
    }

    public void notifyObservers(BidTransaction transaction) {
        for (AuctionObserver observer : observers) {
            observer.onBidPlaced(transaction);
        }
    }

    // ── printInfo() — bắt buộc từ Entity ─────────────────────

    @Override
    public void printInfo() {
        System.out.println("Auction: " + getId()
                + " | " + item.getName()
                + " | " + status
                + " | Giá: " + currentPrice);
    }

    // ── Getters ───────────────────────────────────────────────

    public Item getItem()                { return item; }
    public Seller getSeller()            { return seller; }
    public AuctionStatus getStatus()     { return status; }
    public double getCurrentPrice()      { return currentPrice; }
    public LocalDateTime getStartTime()  { return startTime; }
    public LocalDateTime getEndTime()    { return endTime; }
    public Bidder getLeadingBidder()     { return highestBidder; }
    public Bidder getWinner()            { return winner; }
    public List<BidTransaction> getBids(){ return bidHistory; }

    // ── Setters — AuctionManager cần ─────────────────────────

    public void setStatus(AuctionStatus status)   { this.status = status; }
    public void setWinner(Bidder winner)           { this.winner = winner; }
    public void setEndTime(LocalDateTime endTime)  { this.endTime = endTime; }
}