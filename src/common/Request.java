package common;

import java.io.Serializable;

/**
 * A message from the client to the server: what kind of request it is,
 * and its payload (the content; what it should be depends on the type, see RequestType).
 */
public class Request implements Serializable {

    // Version number for the Serializable mechanism. Keep it as is.
    private static final long serialVersionUID = 1L;

    private final RequestType type;
    private final Serializable payload;

    public Request(RequestType type, Serializable payload) {
        this.type = type;
        this.payload = payload;
    }

    public RequestType getType() {
        return type;
    }

    public Serializable getPayload() {
        return payload;
    }
}
