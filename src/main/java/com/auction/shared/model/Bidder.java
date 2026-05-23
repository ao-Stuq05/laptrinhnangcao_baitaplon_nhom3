package com.auction.shared.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.io.Serializable;

/**
 * Bidder — người đấu giá.
 *
 * Logic tài chính:
 *  - balance      : số dư thực tế (có thể dùng)
 *  - frozenBalance: tổng tiền đang bị giữ (đặt cọc cho các phiên đang tham gia)
 *  - availableBalance = balance - frozenBalance
 *
 * Khi đặt giá:  frozenBalance += amount  (giữ tiền)
 * Khi bị outbid: frozenBalance -= oldBid (hoàn tiền đặt cọc cũ)
 * Khi thắng phiên: balance -= winAmount, frozenBalance -= winAmount (trừ thật)
 * Khi thua phiên:  frozenBalance -= bidAmount (giải phóng, không trừ balance)
 */
public class Bidder extends User implements Serializable {

    private static final long serialVersionUID = 1L;

    private double balance;
    /** Tiền đang bị giữ cho các phiên đang tham gia (chưa kết thúc) */
    private double frozenBalance;
    private List<BidTransaction> bidHistory;

    /** Map<auctionId, frozenAmount> — bao nhiêu tiền đang giữ ở mỗi phiên */
    private Map<String, Double> frozenPerAuction;

    public Bidder(String id, String username, String email, String passwordHash, double balance) {
        super(id, username, email, passwordHash);
        this.balance           = balance;
        this.frozenBalance     = 0.0;
        this.bidHistory        = new ArrayList<>();
        this.frozenPerAuction  = new HashMap<>();
    }

    public Bidder(String username, String email, String passwordHash, double balance) {
        super(UUID.randomUUID().toString(), username, email, passwordHash);
        this.balance           = balance;
        this.frozenBalance     = 0.0;
        this.bidHistory        = new ArrayList<>();
        this.frozenPerAuction  = new HashMap<>();
    }

    public Bidder(String username, String email, String passwordHash) {
        super(UUID.randomUUID().toString(), username, email, passwordHash);
        this.balance           = 0.0;
        this.frozenBalance     = 0.0;
        this.bidHistory        = new ArrayList<>();
        this.frozenPerAuction  = new HashMap<>();
    }

    @Override public String getRole() { return "BIDDER"; }

    // ── Số dư ─────────────────────────────────────────────────────────────────
    public double getBalance()          { return balance; }
    public void   setBalance(double b)  { this.balance = b; }

    public double getFrozenBalance()    { return frozenBalance; }
    public void   setFrozenBalance(double f) { this.frozenBalance = f; }

    /** Số dư khả dụng = tổng số dư - tiền đang bị giữ */
    public double getAvailableBalance() { return balance - frozenBalance; }

    // ── Freeze / unfreeze ──────────────────────────────────────────────────────

    /**
     * Giữ tiền khi đặt giá vào 1 phiên.
     * Nếu đã giữ tiền phiên này trước (outbid cũ), thay thế bằng giá mới.
     * @return true nếu đủ số dư khả dụng
     */
    public synchronized boolean freezeForAuction(String auctionId, double newAmount) {
        if (frozenPerAuction == null) frozenPerAuction = new HashMap<>();
        double oldFrozen = frozenPerAuction.getOrDefault(auctionId, 0.0);
        double delta     = newAmount - oldFrozen; // tiền cần giữ thêm

        if (getAvailableBalance() < delta) return false; // không đủ

        frozenBalance += delta;
        frozenPerAuction.put(auctionId, newAmount);
        return true;
    }

    /**
     * Giải phóng tiền đang giữ cho 1 phiên (khi phiên kết thúc hoặc bị outbid hoàn toàn).
     */
    public synchronized void unfreezeForAuction(String auctionId) {
        if (frozenPerAuction == null) return;
        double amount = frozenPerAuction.getOrDefault(auctionId, 0.0);
        frozenBalance = Math.max(0, frozenBalance - amount);
        frozenPerAuction.remove(auctionId);
    }

    /**
     * Khi thắng phiên: trừ thật từ balance, giải phóng frozen.
     */
    public synchronized void chargeWin(String auctionId) {
        if (frozenPerAuction == null) return;
        double amount = frozenPerAuction.getOrDefault(auctionId, 0.0);
        balance       = Math.max(0, balance - amount);
        frozenBalance = Math.max(0, frozenBalance - amount);
        frozenPerAuction.remove(auctionId);
    }

    public double getFrozenForAuction(String auctionId) {
        if (frozenPerAuction == null) return 0.0;
        return frozenPerAuction.getOrDefault(auctionId, 0.0);
    }

    // ── Bid history ───────────────────────────────────────────────────────────
    public void bid(Auction auction, double amount) { auction.placeBid(this, amount); }
    public List<BidTransaction> getBidHistory()     { return bidHistory; }

    @Override
    public void printInfo() {
        System.out.printf("[BIDDER] %s | balance=%.0f | frozen=%.0f | available=%.0f%n",
                getUsername(), balance, frozenBalance, getAvailableBalance());
    }
}
