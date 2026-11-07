package common;


/** A new customer: 10% welcome discount, becomes RETURNING after the first purchase. */
public class NewCustomer extends Customer {
    public static final double DISCOUNT = 0.10;

    public NewCustomer(String id, String fullName, String phone, int purchaseCount, double totalSpent) {
        super(id, fullName, phone, purchaseCount, totalSpent);
    }

    @Override
    public CustomerType getType() {
        return CustomerType.NEW;
    }

    @Override
    public double applyPromotion(double total) {
        return round2(total * (1 - DISCOUNT));
    }

    @Override
    public String getPurchaseTrack() {
        return "New customer track: first purchase with a 10% welcome discount; after the purchase the customer moves to the 'returning' track automatically.";
    }

    @Override
    public String getPromotionDescription() {
        return "10% welcome discount";
    }

    @Override
    public CustomerType jobTransition() {
        if (getPurchaseCount() >= 1) {
            return CustomerType.RETURNING;
        }
        return CustomerType.NEW;
    }
}
