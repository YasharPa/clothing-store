package client;

import common.Request;
import common.Response;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * One open connection to the server: send a Request, get back the Response.
 * The windows use this so they never have to deal with sockets and streams themselves.
 */
public class ServerConnection implements AutoCloseable {

    private final Socket socket;
    private final ObjectOutputStream out;
    private final ObjectInputStream in;

    /** Connects to the server. Throws an IOException (ConnectException) if the server is not running. */
    public ServerConnection(String host, int port) throws IOException {
        socket = new Socket(host, port);
        try {
            // Output stream first, then input stream (see the note in ClientHandler.handleClient).
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            socket.close();
            throw e;
        }
    }

    /**
     * Sends one request and waits for the answer.
     * synchronized: only one request at a time on this connection, so answers never get mixed up.
     */
    public synchronized Response send(Request request) throws IOException {
        out.writeObject(request);
        out.flush();
        try {
            return (Response) in.readObject();
        } catch (ClassNotFoundException | ClassCastException e) {
            throw new IOException("The server sent something unexpected", e);
        }
    }

    /** Closes the connection. If someone was still logged in on it, the server logs them out. */
    @Override
    public void close() {
        try {
            socket.close();
        } catch (IOException e) {
            // Already closed or broken: nothing left to do.
        }
    }
}
