package com.auction.shared.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Auction extends Entity {
    private Item item;
    private AuctionStatus status;
    private double currentPrice;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    // TODO: Chờ Thành viên A và C hoàn thành các class này thì mở comment
    // private Bidder highestBidder;
    // private List<BidTransaction> bidHistory;

    // ------------------------------------------------------------------------
    // NHIỆM VỤ B: THÊM OBSERVER PATTERN (TRẠM PHÁT SÓNG)
    // ------------------------------------------------------------------------
    // Dùng CopyOnWriteArrayList để đảm bảo An toàn Đa luồng (Thread-safe)
    private transient List<AuctionObserver> observers = new CopyOnWriteArrayList<>();

    // Thêm người nghe (giao diện) vào danh sách
    public void addObserver(AuctionObserver observer) {
        if (observer != null && !observers.contains(observer)) {
            observers.add(observer);
        }
    }

    // Xóa người nghe khỏi danh sách (khi họ tắt giao diện)
    public void removeObserver(AuctionObserver observer) {
        observers.remove(observer);
    }

    // Phát thông báo cho tất cả người nghe khi có giá mới
    public void notifyObservers(BidTransaction transaction) {
        for (AuctionObserver observer : observers) {
            observer.onBidPlaced(transaction);
        }
    }
    // ------------------------------------------------------------------------


    public Auction(String id, Item item, LocalDateTime startTime, LocalDateTime endTime) {
        super(id);
        this.item = item; // Giờ thì cả 2 bên đều là kiểu Item rồi, hết cãi nhau nhé!
        this.status = AuctionStatus.OPEN;
        this.currentPrice = item.getBasePrice();
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public void startAuction() {
        this.status = AuctionStatus.RUNNING;
        System.out.println(">> Phiên đấu giá cho [" + item.getName() + "] đã CHÍNH THỨC BẮT ĐẦU!");
    }

    public Item getItem() { return item; }
    public AuctionStatus getStatus() { return status; }
    public double getCurrentPrice() { return currentPrice; }
}