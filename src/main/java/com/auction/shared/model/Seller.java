import java.util.ArrayList;
import java.util.List;

class Seller extends User {
    private String shopName;
    private List<Item> listedItems;
    private double rating;

    public Seller(String id, String username, String email, String pw, String shopName) {
        super(id, username, email, pw);
        this.shopName = shopName;
        this.listedItems = new ArrayList<>();
    }

    @Override
    public String getRole() {
        return "Seller";
    }

    public void listItem(Item item) {
        listedItems.add(item);
    }

    public void createAuction(Item item) {
        Auction auction = new Auction("AUC-" + System.currentTimeMillis(), item);
        AuctionManager.getInstance().registerAuction(auction);
    }
}