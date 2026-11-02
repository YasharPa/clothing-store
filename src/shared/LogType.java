package shared;

/** Kinds of actions written to the system log. */
public enum LogType {
    EMPLOYEE_REGISTRATION("Employee registration"),
    EMPLOYEE_UPDATED("Employee update"),
    EMPLOYEE_DELETED("Employee deletion"),
    CUSTOMER_REGISTRATION("Customer registration"),
    CUSTOMER_UPDATED("Customer update"),
    CUSTOMER_DELETED("Customer deletion"),
    LOGIN("Login"),
    LOGIN_FAILED("Failed login"),
    LOGOUT("Logout"),
    POLICY_CHANGED("Password policy change"),
    SALE("Sale"),
    STOCK_PURCHASE("Stock purchase"),
    PRODUCT_ADDED("Product added"),
    REPORT("Report generated"),
    CHAT_STARTED("Chat opened"),
    CHAT_ENDED("Chat ended"),
    CHAT_QUEUED("Chat request queued");

    private final String label;

    LogType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
