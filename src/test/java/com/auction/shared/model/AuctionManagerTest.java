package com.auction.shared.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class AuctionManagerTest {

    private AuctionManager manager;
    private Auction auction1;

    @BeforeEach
    void setUp() {
        // 1. Gọi quản lý
        manager = AuctionManager.getInstance();
        manager.getActive().clear();

        // 2. Tạo đúng 1 Món đồ (bỏ qua người bán bằng chữ null)
        Item item = new Electronics(
                "ITM-01",                     // id
                "Iphone 15",                  // name
                "Điện thoại Apple",           // description
                1000.0,                       // basePrice
                null,                         // seller (để null cho nhanh)
                12                            // warrantyMonths
        );

        auction1 = new Auction(item, LocalDateTime.now().plusDays(2));
    }

    @Test
    void testRegisterAuction() {
        // 3. Test chức năng thêm phiên đấu giá
        manager.registerAuction(auction1);

        assertEquals(1, manager.getActive().size(), "Danh sách quản lý phải có 1 phiên");
        assertTrue(manager.getActive().contains(auction1), "Danh sách phải chứa chính xác phiên vừa đăng ký");
    }
}