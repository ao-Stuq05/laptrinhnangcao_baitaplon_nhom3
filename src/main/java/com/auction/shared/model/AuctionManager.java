import java.util.ArrayList;
import java.util.List;

class AuctionManager {
    private static AuctionManager instance;
    private List<Auction> auctions;

    private AuctionManager() {
        auctions = new ArrayList<>();
    }

    public static AuctionManager getInstance() {
        if (instance == null) {
            instance = new AuctionManager();
        }
        return instance;
    }

    public void registerAuction(Auction auction) {
        auctions.add(auction);
    }

    public List<Auction> getActive() {
        return auctions;
    }
}