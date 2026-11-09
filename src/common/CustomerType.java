package common;


public enum CustomerType {
    NEW("New"),
    RETURNING("Returning"),
    VIP("VIP");

    private final String label;

    CustomerType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
