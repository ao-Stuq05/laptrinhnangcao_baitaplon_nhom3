package com.auction.shared.exception;

import com.auction.shared.model.AuctionStatus;

public class AuctionClosedException extends Exception {
    private final AuctionStatus actualStatus; // trạng thái phiên lúc lỗi xảy ra
    private final String auctionId; // phiên xảy ra lỗi

    public AuctionClosedException (String auctionId, AuctionStatus actualStatus) {
        super(String.format(
                "Phiên %s không thể đặt giá. Trạng thái hiện tại: %s",
                auctionId, actualStatus));
        this.auctionId = auctionId;
        this.actualStatus = actualStatus;
    }

    public AuctionClosedException (String message) {
        super(message);
        this.auctionId    = null;
        this.actualStatus = null;
    }

    public AuctionStatus getStatus() { return actualStatus; }
    public String getAuctionId() { return auctionId; }
}
