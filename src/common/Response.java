package common;

import java.io.Serializable;

/**
 * A message from the server back to the client: did it work, a text message,
 * and optionally some data (for example the User after a successful login).
 */
public class Response implements Serializable {

    private static final long serialVersionUID = 1L;

    private final boolean success;
    private final String message;
    private final Serializable data;

    /** A response without extra data. */
    public Response(boolean success, String message) {
        this(success, message, null);
    }

    /**
     * @param success true if the request worked
     * @param message a text for the user: the answer, or what went wrong
     * @param data    extra content for the client (for example the User after a login), or null
     */
    public Response(boolean success, String message, Serializable data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    /** Extra content, or null if there is none. */
    public Serializable getData() {
        return data;
    }
}
