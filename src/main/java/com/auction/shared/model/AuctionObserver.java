package com.auction.shared.model;

public interface AuctionObserver {
    // Hàm này sẽ được gọi khi có bất kỳ ai đặt giá thành công
    void onBidPlaced(BidTransaction transaction);
}