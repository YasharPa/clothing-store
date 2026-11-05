package common;

/** Values that the client and the server must agree on. */
public final class Protocol {

    /** The computer the server runs on. "localhost" = this same computer; change it to the server's address later. */
    public static final String HOST = "localhost";

    /** The port the server listens on and the client connects to. */
    public static final int PORT = 5000;

    // Nobody should create a Protocol object; it only holds constants.
    private Protocol() {
    }
}
