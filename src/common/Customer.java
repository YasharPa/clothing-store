package common;

import java.io.Serializable;

/**
 * customer type (NEW / RETURNING / VIP)
 * defines its own purchase track and promotion
 */
public abstract class Customer implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String id;
    private String fullName;
    private String phone;
    private int purchaseCount;
    private double totalSpent;

    protected Customer(String id, String fullName, String phone, int purchaseCount, double totalSpent) {
        this.id = id;
        this.fullName = fullName;
        this.phone = phone;
        this.purchaseCount = purchaseCount;
        this.totalSpent = totalSpent;
    }

    public abstract CustomerType getType();

    /** amount to pay after this customer type's promotion */
    public abstract double applyPromotion(double total);

    /** description of the purchase track of this customer type */
    public abstract String getPurchaseTrack();

    /** Short description of the promotion */
    public abstract String getPromotionDescription();

    /** The type this customer should have after the purchases recorded */
    public abstract CustomerType jobTransition();

    /** Gift attached to a purchase */
    public String getGift(double paid) {
        return null;
    }

    public void recordPurchase(double paid) {
        purchaseCount++;
        totalSpent = round2(totalSpent + paid);
    }

    protected static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    public String getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public int getPurchaseCount() {
        return purchaseCount;
    }

    public double getTotalSpent() {
        return totalSpent;
    }

    @Override
    public String toString() {
        return fullName + " (" + id + ", " + getType().getLabel() + ")";
    }

    public static Customer create(CustomerType type, String id, String fullName, String phone,
                                  int purchaseCount, double totalSpent) {
        switch (type) {
            case NEW:
                return new NewCustomer(id, fullName, phone, purchaseCount, totalSpent);
            case RETURNING:
                return new ReturningCustomer(id, fullName, phone, purchaseCount, totalSpent);
            case VIP:
                return new VIPCustomer(id, fullName, phone, purchaseCount, totalSpent);
            default:
                throw new ServiceException("Unsupported customer type");
        }
    }

    public static Customer create(CustomerType type, String id, String fullName, String phone) {
        return create(type, id, fullName, phone, 0, 0);
    }

    public static Customer convert(Customer c, CustomerType newType) {
        return create(newType, c.getId(), c.getFullName(), c.getPhone(), c.getPurchaseCount(), c.getTotalSpent());
    }
}
