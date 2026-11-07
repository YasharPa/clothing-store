package common;

public enum Role {
    ADMIN("System admin"),
    SHIFT_MANAGER("Shift manager"),
    CASHIER("Cashier"),
    SELLER("Seller");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
