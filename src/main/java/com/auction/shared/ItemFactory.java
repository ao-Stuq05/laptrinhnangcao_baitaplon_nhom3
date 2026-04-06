package auction.model;

import auction.model.Art;
import auction.model.Electronics;
import auction.model.Item;
import auction.model.Vehicle;

public class ItemFactory {

    public static Item createItem(String type, String id, String name, String description, double basePrice, Object... extraParams) {
        switch (type.toUpperCase()) {
            case "ELECTRONICS":
                // extraParams[0] là warrantyMonths (kiểu Integer)
                return new Electronics(id, name, description, basePrice, (Integer) extraParams[0]);

            case "ART":
                // extraParams[0] là artistName (String), extraParams[1] là creationYear (Integer)
                return new Art(id, name, description, basePrice, (String) extraParams[0], (Integer) extraParams[1]);

            case "VEHICLE":
                // extraParams[0] là licensePlate (String), extraParams[1] là mileage (Integer)
                return new Vehicle(id, name, description, basePrice, (String) extraParams[0], (Integer) extraParams[1]);

            default:
                throw new IllegalArgumentException("Lỗi: Loại sản phẩm không được hỗ trợ -> " + type);
        }
    }
}