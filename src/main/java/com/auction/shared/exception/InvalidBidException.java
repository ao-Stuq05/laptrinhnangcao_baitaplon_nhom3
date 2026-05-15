package com.auction.shared.exception;

public class InvalidBidException extends Exception {

    private final double attemptedAmount; // Số tiền người dùng đã nhập
    private final double currentPrice;    // Giá hiện tại của phiên lúc đó

    public InvalidBidException(double attemptedAmount, double currentPrice) {
        // Gọi constructor của Exception cha, truyền message tự động tạo
        super(String.format(
                "Giá đặt %.0f phải cao hơn giá hiện tại %.0f",
                attemptedAmount, currentPrice));
        this.attemptedAmount = attemptedAmount;
        this.currentPrice    = currentPrice;
    }

    public InvalidBidException(String message) {
        super(message);
        this.attemptedAmount = -1; // -1 = không có thông tin
        this.currentPrice    = -1;
    }

    // Getter để GUI lấy số và tự format theo ý muốn
    public double getAttemptedAmount() { return attemptedAmount; }
    public double getCurrentPrice()    { return currentPrice; }
}
