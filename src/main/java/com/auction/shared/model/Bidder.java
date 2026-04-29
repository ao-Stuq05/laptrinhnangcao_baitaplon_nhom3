package com.auction.shared.model;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Người đấu giá (Bidder) - là một loại User tham gia đấu giá.
 * 
 * <p><b>PasswordHash:</b> Hiện tại đang lưu trực tiếp dạng plain text.
 * Trong production nên dùng BCrypt hoặc Argon2 để hash password.</p>
 * 
 * <p>Ví dụ sử dụng BCrypt:</p>
 * <pre>
 *   String passwordHash = BCrypt.hashpw(rawPassword, BCrypt.gensalt());
 *   Bidder bidder = new Bidder(username, email, passwordHash);
 * </pre>
 */
public class Bidder extends User {
    private double balance;
    private List<BidTransaction> bidHistory;

    /**
     * Constructor đầy đủ - dùng khi load dữ liệu từ database.
     * 
     * @param id           ID duy nhất của bidder (từ DB)
     * @param username     Tên đăng nhập
     * @param email        Email
     * @param passwordHash Đã được hash trước đó (khuyến nghị dùng BCrypt)
     * @param balance      Số dư tài khoản
     */
    public Bidder(String id, String username, String email, String passwordHash, double balance) {
        super(id, username, email, passwordHash);
        this.balance = balance;
        this.bidHistory = new ArrayList<>();
    }

    /**
     * Constructor tự tạo ID - dùng khi tạo bidder mới từ code.
     * ID sẽ được tự động sinh bằng UUID.
     * 
     * @param username     Tên đăng nhập
     * @param email        Email
     * @param passwordHash Đã được hash trước đó (khuyến nghị dùng BCrypt)
     * @param balance      Số dư tài khoản
     */
    public Bidder(String username, String email, String passwordHash, double balance) {
        super(UUID.randomUUID().toString(), username, email, passwordHash);
        this.balance = balance;
        this.bidHistory = new ArrayList<>();
    }

    /**
     * Constructor cho đăng ký mới - balance mặc định là 0.
     * Dùng khi user đăng ký tài khoản qua form đăng ký.
     * 
     * @param username     Tên đăng nhập
     * @param email        Email
     * @param passwordHash Đã được hash trước đó (khuyến nghị dùng BCrypt)
     */
    public Bidder(String username, String email, String passwordHash) {
        super(UUID.randomUUID().toString(), username, email, passwordHash);
        this.balance = 0.0;  // mặc định 0
        this.bidHistory = new ArrayList<>();
    }

    @Override
    public String getRole() {
        return "BIDDER";
    }

    public void bid(Auction auction, double amount) {
        auction.placeBid(this, amount);
    }

    public List<BidTransaction> getBidHistory() {
        return bidHistory;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }
}