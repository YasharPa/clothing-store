package common;

import java.io.Serializable;

/** A message from the client to the server: what kind of request it is, and its content. */
public class Request implements Serializable {

    // Version number for the Serializable mechanism. Keep it as is.
    private static final long serialVersionUID = 1L;

    private final RequestType type;
    private final String text;

    public Request(RequestType type, String text) {
        this.type = type;
        this.text = text;
    }

    public RequestType getType() {
        return type;
    }

    public String getText() {
        return text;
    }
}
