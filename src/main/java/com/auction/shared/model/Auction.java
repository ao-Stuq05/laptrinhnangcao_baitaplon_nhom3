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
    private Bidder highestBidder;
    private List<BidTransaction> bidHistory;

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
    // --- Constructor 1: Đầy đủ tham số (Dùng khi có sẵn ID) ---
    public Auction(String id, Item item, LocalDateTime startTime, LocalDateTime endTime) {
        super(id); // Gọi lên Entity
        this.item = item;
        this.startTime = startTime;
        this.endTime = endTime;
        this.currentPrice = item.getBasePrice();
        this.status = AuctionStatus.OPEN;
    }

    // --- Constructor 2: Rút gọn (Dùng cho lớp Seller gọi) ---
    public Auction(Item item, LocalDateTime endTime) {
        // Tự tạo ID duy nhất và gọi lại Constructor 1 bằng từ khóa this()
        // Hoặc gọi trực tiếp super()
        super("AUC-" + System.currentTimeMillis()); 
        
        this.item = item;
        this.startTime = LocalDateTime.now(); // Tự động lấy giờ hiện tại
        this.endTime = endTime;
        this.currentPrice = item.getBasePrice();
        this.status = AuctionStatus.OPEN;
    }


    

    public void startAuction() {
        this.status = AuctionStatus.RUNNING;
        System.out.println(">> Phiên đấu giá cho [" + item.getName() + "] đã CHÍNH THỨC BẮT ĐẦU!");
    }
    public void cancel(){
        if (this.status == AuctionStatus.OPEN || this.status == AuctionStatus.RUNNING) {
            this.status = AuctionStatus.CANCELLED;
            System.out.println(" Cuộc đấu giá " + getId() + " đã bị hủy.");
        } 
        else {
        System.out.println("Lỗi: Không thể hủy cuộc đấu giá đã kết thúc hoặc đã hủy trước đó.");
        }

    };
    public void placeBid(Bidder bidder, double amount){
        // 1. Kiểm tra trạng thái đấu giá (chỉ cho phép nếu đang OPEN)
        if (this.status != AuctionStatus.OPEN) {
            System.out.println("Lỗi: Cuộc đấu giá hiện không mở!");
            return;
        }

        // 2. Kiểm tra số tiền bid phải cao hơn giá hiện tại
        if (amount <= this.currentPrice) {
            System.out.println("Lỗi: Giá đấu phải cao hơn giá hiện tại (" + currentPrice + ")");
            return;
        }

        // 3. Cập nhật giá hiện tại và lưu lịch sử (nếu cần)
        this.currentPrice = amount;
    
        // Giả sử bạn muốn lưu vết ai là người đang giữ giá cao nhất
        // this.highestBidder = bidder; 

        System.out.println("Chúc mừng! " + bidder.getUsername() + " đã đặt giá " + amount);
    }
    public Auction(String id, Item item) {
        this(id, item, LocalDateTime.now(), LocalDateTime.now().plusDays(1)); 
        // Tự động lấy giờ hiện tại và kết thúc sau 1 ngày
    }

    public Item getItem() { return item; }
    public AuctionStatus getStatus() { return status; }
    public double getCurrentPrice() { return currentPrice; }

}

