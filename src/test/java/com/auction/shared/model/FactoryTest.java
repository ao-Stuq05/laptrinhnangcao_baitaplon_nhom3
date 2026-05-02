package com.auction.shared.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FactoryTest {

    @Test
    void testCreateElectronicsItem() {
        // Kịch bản 1: Truyền đủ 7 tham số
        Item laptop = ItemFactory.createItem(
                "ELECTRONICS",    // 1. Loại sản phẩm (String)
                "LAP-01",         // 2. ID sản phẩm (String)
                "Macbook Pro",    // 3. Tên sản phẩm (String)
                "Hàng chính hãng",// 4. Mô tả sản phẩm (String) ---> ĐÃ THÊM Ở ĐÂY
                2000.0,           // 5. Giá tiền (double)
                null,             // 6. Người bán (Seller)
                new Object[]{12}  // 7. Thông số phụ (Object[])
        );

        // Kiểm tra
        assertNotNull(laptop, "Sản phẩm tạo ra không được null");
        assertTrue(laptop instanceof Electronics, "Sản phẩm phải thuộc loại Điện tử");
    }

    @Test
    void testCreateInvalidItemThrowsException() {
        // Kịch bản 2: Loại hàng không hợp lệ
        assertThrows(IllegalArgumentException.class, () -> {
            ItemFactory.createItem(
                    "UFO",            // 1. Loại
                    "UFO-01",         // 2. ID
                    "Đĩa bay",        // 3. Tên
                    "Đến từ sao hỏa", // 4. Mô tả ---> ĐÃ THÊM Ở ĐÂY
                    9999.0,           // 5. Giá
                    null,             // 6. Seller
                    new Object[]{}    // 7. Object[]
            );
        }, "Phải báo lỗi IllegalArgumentException khi loại sản phẩm không hợp lệ");
    }
}