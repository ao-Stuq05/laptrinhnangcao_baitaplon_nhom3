
package com.auction.shared.model;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;


public class BidTransaction extends Entity {

    // serialVersionUID bắt buộc khi implements Serializable
    // Nếu không có, Java tự tạo ngẫu nhiên → lỗi khi deserialize
    private static final long serialVersionUID = 1L;
    private Bidder bidder;
    private double bidAmount;
    private LocalDateTime timestamp;
    private boolean isWinning;                                    
    private String auctionId;
    /**
     * Constructor chinh
     * timestamp tu dong gan khi tao bid moi, khong can truyen tu ngoai vao. Vi du: new BidTransaction(bidder, 1000, auctionId)             
     */
    public BidTransaction(Bidder bidder, double bidAmount, String auctionId) {
        super(UUID.randomUUID().toString());
        // Validate đầu vào — không chờ DB mới phát hiện lỗi
        if (bidder == null) {
            throw new IllegalArgumentException("Bidder không được null");
        }
        if (bidAmount <= 0) {
            throw new IllegalArgumentException("Bid amount phải > 0, nhận được: " + bidAmount);
        }
        if (auctionId == null || auctionId.isBlank()) {
            throw new IllegalArgumentException("AuctionId không được rỗng");
        }

        this.bidder = bidder;
        this.bidAmount = bidAmount;
        this.auctionId = auctionId;
        this.timestamp = LocalDateTime.now();  
        this.isWinning = false;
    }

    /**
     * Constructor thứ hai — dùng khi load từ Database.
     * Cần truyền timestamp vì đây là dữ liệu đã lưu sẵn.
     */
    public BidTransaction(String id, Bidder bidder, double bidAmount,
                          String auctionId, LocalDateTime timestamp, boolean isWinning) {
        super(id);
        this.bidder = bidder;
        this.bidAmount = bidAmount;
        this.auctionId = auctionId;
        this.timestamp = timestamp;
        this.isWinning = isWinning;
    }


    @Override
    public void printInfo() {
        System.out.printf("[Bid] ID: %s | Bidder: %s | Giá: %.0f | Lúc: %s | Dẫn đầu: %s%n",
            getId(),
            bidder.getUsername(),
            bidAmount,
            timestamp.toString(),
            isWinning ? "CÓ" : "KHÔNG"
        );
    }

   // get/set

    public Bidder getBidder() { return bidder; }

    public double getBidAmount() { return bidAmount; }

    public LocalDateTime getTimestamp() { return timestamp; }

    public String getAuctionId() { return auctionId; }

    public boolean isWinning() { return isWinning; }

    // Setter cho isWinning — chỉ có thể thay đổi trạng thái dẫn đầu, không thay đổi thông tin bid khác
    public void setWinning(boolean winning) { this.isWinning = winning; }

    // ── Utility ──────────────────────────────────────────────

    @Override
    public String toString() {
        return String.format("BidTransaction{bidder='%s', amount=%.0f, winning=%b}",
            bidder.getUsername(), bidAmount, isWinning);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BidTransaction)) return false;
        BidTransaction other = (BidTransaction) o;
        return getId().equals(other.getId());  
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }
}
