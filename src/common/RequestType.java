package common;

/** The kinds of requests a client can send to the server. */
public enum RequestType {
    /** Payload: Credentials. The server answers with the logged-in User, or an error. */
    LOGIN,
    /** No payload. Logs out the user of this connection right away. */
    LOGOUT,
    /** Payload: NewAccount. Creates a user. Only an ADMIN may do this. */
    ADD_USER,
    /** Payload: a String. The server sends the same text back. Needs a logged-in user. */
    ECHO,
    /** No payload. The answer's data is an ArrayList of Customer. */
    GET_CUSTOMERS,
    /** Payload: a String (the customer's ID). The answer's data is a Customer. */
    GET_CUSTOMER,
    /** Payload: CustomerForm. Registers a customer. */
    ADD_CUSTOMER,
    /** Payload: CustomerForm (the type is ignored). Updates the name and phone of a customer. */
    UPDATE_CUSTOMER,
    /** Payload: ReportRequest. The answer's data is a ReportResult. */
    GET_REPORT,
    /** Payload: LogQuery. The answer's data is an ArrayList of LogEntry. Admin or shift manager only. */
    GET_LOGS,
    /** Payload: a String (a branch name) or null for your own branch. The answer's data is an ArrayList of StockItem. */
    GET_INVENTORY,
    /** Payload: SaleForm. Sells from your branch, records the sale and updates the customer. Cashier or shift manager. */
    SELL_ITEM,
    /** Payload: StockChange. Buys stock for your branch. Shift manager only. */
    RESTOCK_ITEM,
    /** No payload. The answer's data is an ArrayList of String, one per branch: "number - name". */
    GET_BRANCHES
}
