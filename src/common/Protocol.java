package common;

/** Values that the client and the server must agree on. */
public final class Protocol {

    /** The port the server listens on and the client connects to. */
    public static final int PORT = 5000;

    // Nobody should create a Protocol object; it only holds constants.
    private Protocol() {
    }
}
