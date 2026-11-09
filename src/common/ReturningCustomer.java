package common;

/** A returning customer: 5% discount ; becomes VIP at 6 purchases / 3500 ILS */
public class ReturningCustomer extends Customer {
    public static final int VIP_PURCHASES = 6;
    public static final double VIP_CASH = 3500;
    public static final double DISCOUNT = 0.05;

    public ReturningCustomer(String id, String fullName, String phone, int purchaseCount, double totalSpent) {
        super(id, fullName, phone, purchaseCount, totalSpent);
    }

    @Override
    public CustomerType getType() {
        return CustomerType.RETURNING;
    }

    @Override
    public double applyPromotion(double total) {
        return round2(total * (1 - DISCOUNT));
    }

    @Override
    public String getPurchaseTrack() {
        return "Returning customer track: 5% discount on every purchase; "
                + "moves to VIP after " + VIP_PURCHASES + " purchases or a total spend of " + (int) VIP_CASH + " ₪";
    }

    @Override
    public String getPromotionDescription() {
        return "Returning customer 5% discount";
    }

    @Override
    public CustomerType jobTransition() {
        if (getPurchaseCount() >= VIP_PURCHASES || getTotalSpent() >= VIP_CASH) {
            return CustomerType.VIP;
        }
        return CustomerType.RETURNING;
    }
}