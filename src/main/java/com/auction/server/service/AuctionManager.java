package com.auction.server.service;

import com.auction.shared.model.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;

public class AuctionManager {

    // ── Singleton ─────────────────────────────────────────────
    private static AuctionManager instance;

    private AuctionManager() {
        this.scheduler         = Executors.newScheduledThreadPool(2);
        this.auctions          = new ConcurrentHashMap<>();
        this.scheduledClosures = new ConcurrentHashMap<>();
        System.out.println("[AuctionManager] Khởi động.");
    }

    public static synchronized AuctionManager getInstance() {
        if (instance == null) instance = new AuctionManager();
        return instance;
    }

    // ── Fields ────────────────────────────────────────────────
    private final ScheduledExecutorService scheduler;
    private final Map<String, Auction> auctions;
    private final Map<String, ScheduledFuture<?>> scheduledClosures;

    // ── Task 1: Đăng ký + lên lịch đóng phiên ────────────────

    public void registerAuction(Auction auction) {
        if (auction == null) return;
        auctions.put(auction.getId(), auction);
        System.out.println("[Manager] Đăng ký phiên: " + auction.getId());
        scheduleAutoClose(auction);
    }

    private void scheduleAutoClose(Auction auction) {
        long delay = ChronoUnit.SECONDS.between(
            LocalDateTime.now(), auction.getEndTime());

        if (delay <= 0) { closeAuction(auction.getId()); return; }

        System.out.printf("[Manager] Phiên %s đóng sau %ds%n",
            auction.getId(), delay);

        ScheduledFuture<?> future = scheduler.schedule(
            () -> closeAuction(auction.getId()),
            delay, TimeUnit.SECONDS
        );
        scheduledClosures.put(auction.getId(), future);
    }

    public synchronized void extendAuction(String id, long extraSec) {
        Auction a = auctions.get(id);
        if (a == null) return;
        ScheduledFuture<?> old = scheduledClosures.get(id);
        if (old != null && !old.isDone()) old.cancel(false);
        a.setEndTime(a.getEndTime().plusSeconds(extraSec));
        scheduleAutoClose(a);
    }

    // ── Task 2: Đóng phiên + xác định winner ─────────────────

    public synchronized void closeAuction(String auctionId) {
        Auction a = auctions.get(auctionId);
        if (a == null) return;

        AuctionStatus s = a.getStatus();
        if (s == AuctionStatus.FINISHED || s == AuctionStatus.PAID
                || s == AuctionStatus.CANCELLED) return;

        a.setStatus(AuctionStatus.FINISHED);
        scheduledClosures.remove(auctionId);
        System.out.println("[Manager] Phiên " + auctionId + " → FINISHED");

        determineWinner(a);
    }

    private void determineWinner(Auction auction) {
        Bidder winner = auction.getLeadingBidder();

        if (winner == null) {
            auction.setStatus(AuctionStatus.CANCELLED);
            System.out.println("[Manager] → CANCELLED (không có bid)");
            return;
        }

        auction.setWinner(winner);
        System.out.printf("[Manager] → Winner: %s | Giá: %,.0f VNĐ%n",
            winner.getUsername(), auction.getCurrentPrice());
    }

    // ── Query ─────────────────────────────────────────────────

    public Auction getAuction(String id) { return auctions.get(id); }

    public List<Auction> getActiveAuctions() {
        List<Auction> result = new ArrayList<>();
        for (Auction a : auctions.values()) {
            if (a.getStatus() == AuctionStatus.OPEN
                    || a.getStatus() == AuctionStatus.RUNNING)
                result.add(a);
        }
        return result;
    }

    // ── Tắt server ────────────────────────────────────────────

    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS))
                scheduler.shutdownNow();
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        System.out.println("[Manager] Đã tắt.");
    }
}