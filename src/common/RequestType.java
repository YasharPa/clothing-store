package common;

/** The kinds of requests a client can send to the server. */
public enum RequestType {
    /** Payload: Credentials. The server answers with the logged-in User, or an error. */
    LOGIN,
    /** Payload: a String. The server sends the same text back. Needs a logged-in user. */
    ECHO
}
