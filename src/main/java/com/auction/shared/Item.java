package auction.model;

public abstract class Item extends Entity {
    protected String name;
    protected String description;
    protected double basePrice;

    public Item(String id, String name, String description, double basePrice) {
        super(id); // Gọi constructor của lớp cha Entity
        this.name = name;
        this.description = description;
        this.basePrice = basePrice;
    }

    // Phương thức trừu tượng, bắt buộc các lớp con phải tự định nghĩa
    public abstract void displayItemDetails();

    public String getName() { return name; }
    public double getBasePrice() { return basePrice; }
}