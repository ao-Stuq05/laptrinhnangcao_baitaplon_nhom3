package com.auction.shared.model;

// FIX: import đúng package của AuctionManager
import com.auction.server.service.AuctionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class AuctionManagerTest {

    private AuctionManager manager;
    private Auction auction1;

    @BeforeEach
    void setUp() {
        manager = AuctionManager.getInstance();

       
        Seller seller = new Seller(
                "seller-test-01",
                "test_seller",
                "seller@test.com",
                "hashed_pass",
                "TestShop",
                5.0
        );
        Electronics item = new Electronics(
                "ITM-01",               // id
                "Iphone 15",            // name
                "Điện thoại Apple",     // description
                1_000_000.0,            // basePrice
                seller,                 // seller — KHÔNG được null
                12
        );

        
        auction1 = new Auction(
                item,
                LocalDateTime.now().plusDays(2)
        );
    }

    @Test
    void testRegisterAuction() {
        // Lấy số phiên trước khi đăng ký
        int sizeBefore = manager.getActiveAuctions().size();

        // Đăng ký phiên mới
        manager.registerAuction(auction1);

        // FIX: dùng getActiveAuctions() thay vì getActive()
        List<Auction> activeList = manager.getActiveAuctions();

        // Số phiên phải tăng thêm 1
        assertEquals(sizeBefore + 1, activeList.size(),
                "Sau registerAuction(), danh sách phải tăng thêm 1 phiên");

        // Phiên vừa đăng ký phải có trong danh sách
        assertTrue(activeList.contains(auction1),
                "Danh sách phải chứa phiên vừa đăng ký");
    }

    @Test
    void testAuctionStatusAfterRegister() {
        manager.registerAuction(auction1);

        // Sau khi đăng ký, status vẫn là OPEN (chưa có bid)
        assertEquals(AuctionStatus.OPEN, auction1.getStatus(),
                "Status phải là OPEN ngay sau khi đăng ký");
    }

    @Test
    void testGetAuctionById() {
        manager.registerAuction(auction1);

        // Lấy lại phiên theo ID — phải tìm thấy
        Auction found = manager.getAuction(auction1.getId());
        assertNotNull(found, "Phải tìm thấy phiên theo ID");
        assertEquals(auction1.getId(), found.getId(),
                "ID phải khớp");
    }
}

/*
 * GHI CHÚ: Nếu muốn test độc lập (không bị ảnh hưởng bởi các test khác),
 * hãy thêm method này vào AuctionManager:
 *
 *   // Chỉ dùng cho testing — không dùng trong production
 *   @VisibleForTesting
 *   public void clearForTest() {
 *       auctions.clear();
 *       scheduledClosures.forEach((k, f) -> f.cancel(false));
 *       scheduledClosures.clear();
 *   }
 *
 * Rồi trong @BeforeEach gọi: manager.clearForTest();
 */
