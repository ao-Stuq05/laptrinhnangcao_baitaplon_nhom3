package com.auction.server.db;

import com.auction.shared.model.*;
import com.auction.shared.model.PasswordUtil; // Đảm bảo class này đã có hàm hash và verify
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.UUID;

public class AuctionSecurityTest {
    public static void main(String[] args) {
        System.out.println("=== TEST HỆ THỐNG: BẢO MẬT USER + ITEM + AUCTION ===");

        UserDAO userDAO = new UserDAO();
        ItemDAO itemDAO = new ItemDAO();
        AuctionDAO auctionDAO = new AuctionDAO();

        try {
            // --- BƯỚC 1: ĐĂNG KÝ NGƯỜI BÁN VỚI BCRYPT ---
            System.out.println("\n[1] Đang tạo người bán với mật khẩu đã mã hóa...");
            String rawPassword = "sellerSecret123";
            String hashedPassword = PasswordUtil.hash(rawPassword); // Dùng BCrypt hash

            String sellerId = UUID.randomUUID().toString();
            Seller seller = new Seller(
                sellerId, 
                "antique_master_" + System.currentTimeMillis(), 
                "seller" + System.currentTimeMillis() + "@gmail.com", 
                hashedPassword, // Lưu Hash vào đối tượng
                "Tiệm Đồ Cổ Heritage", 
                5.0
            );
            userDAO.save(seller);
            System.out.println("[OK] Đã lưu Seller vào MySQL (Password đã được bảo vệ).");

            // --- BƯỚC 2: GIẢ LẬP ĐĂNG NHẬP ĐỂ ĐĂNG HÀNG ---
            System.out.println("\n[2] Kiểm tra mật khẩu người bán trước khi cho phép tạo Item...");
            if (PasswordUtil.verify(rawPassword, seller.getPasswordHash())) {
                System.out.println("[SUCCESS] Xác thực chủ shop thành công. Đang tiến hành tạo sản phẩm...");

                // --- BƯỚC 3: TẠO ITEM ---
                String itemId = UUID.randomUUID().toString();
                // Giả sử tạo một món đồ cổ (Art)
                Art antiqueVase = new Art(
                    itemId, 
                    "Bình gốm Chu Đậu", 
                    "Bình gốm cổ thế kỷ 15, nguyên bản", 
                    2000.0, 
                    seller, 
                    "Nghệ nhân vô danh", 
                    1450
                );
                itemDAO.save(antiqueVase);
                System.out.println("[OK] Đã lưu Item: " + antiqueVase.getName());

                // --- BƯỚC 4: TẠO PHIÊN ĐẤU GIÁ ---
                String auctionId = UUID.randomUUID().toString();
                Auction auction = new Auction(
                    auctionId, 
                    antiqueVase, 
                    seller, 
                    AuctionStatus.OPEN, 
                    2000.0, 
                    LocalDateTime.now(), 
                    LocalDateTime.now().plusDays(3), 
                    null, 
                    null
                );
                auctionDAO.save(auction);
                System.out.println("[OK] Đã tạo phiên đấu giá thành công cho " + antiqueVase.getName());
            } else {
                System.err.println("[FAIL] Sai mật khẩu, không thể tạo hàng!");
            }

            System.out.println("\n=== KẾT THÚC BÀI TEST BẢO MẬT TÍCH HỢP ===");

        } catch (SQLException e) {
            System.err.println("[LOI SQL]: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("[LOI HE THONG]: " + e.getMessage());
            e.printStackTrace();
        }
    }
}