package common;

/** VIP customer: 25% discount and a gift with every purchase */
public class VIPCustomer extends Customer {
    public static final double DISCOUNT = 0.25;

    public VIPCustomer(String id, String fullName, String phone, int purchaseCount, double totalSpent) {
        super(id, fullName, phone, purchaseCount, totalSpent);
    }

    @Override
    public CustomerType getType() {
        return CustomerType.VIP;
    }

    @Override
    public double applyPromotion(double total) {
        return round2(total * (1 - DISCOUNT));
    }

    @Override
    public String getPurchaseTrack() {
        return "VIP track: 25% discount and a gift with every purchase.";
    }

    @Override
    public String getPromotionDescription() {
        return "VIP 25% discount + gift";
    }

    @Override
    public String getGift(double paid) {
        return "A gift accessory of the customer's choice";
    }

    @Override
    public CustomerType jobTransition() {
        return CustomerType.VIP;
    }
}