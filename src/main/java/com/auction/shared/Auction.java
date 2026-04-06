package auction.model;

import java.time.LocalDateTime;

public class Auction extends Entity {
    private Item item;
    private AuctionStatus status;
    private double currentPrice;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    // TODO: Chờ Thành viên A và C hoàn thành các class này thì mở comment
    // private Bidder highestBidder;
    // private List<BidTransaction> bidHistory;

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