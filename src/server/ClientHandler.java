package server;

import common.Request;
import common.Response;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;

/**
 * The worker that talks to ONE client. The server creates one ClientHandler
 * (running in its own thread) for every client that connects.
 */
public class ClientHandler implements Runnable {

    private final Socket socket;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    /** The job of the thread: talk to the client until it disconnects. */
    @Override
    public void run() {
        String client = String.valueOf(socket.getRemoteSocketAddress());
        // try (socket): closes the socket automatically when this client is done.
        try (socket) {
            System.out.println("Client connected: " + client);
            handleClient();
            System.out.println("Client disconnected: " + client);
        } catch (SocketException e) {
            // The connection broke (client crashed, window killed, network dropped).
            System.out.println("Lost connection to " + client + ": " + e.getMessage());
        } catch (IOException | ClassNotFoundException e) {
            // A problem with ONE client must not stop the server or the other clients.
            System.err.println("Problem with client " + client + ": " + e);
        }
    }

    /** Reads Requests from the client and sends back a Response for each, until the client disconnects. */
    private void handleClient() throws IOException, ClassNotFoundException {
        // The output stream must be created BEFORE the input stream (on both sides),
        // otherwise the two programs wait for each other forever.
        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

        while (true) {
            Object message;
            try {
                message = in.readObject();
            } catch (EOFException e) {
                return; // the client hung up normally
            }

            Response response;
            if (message instanceof Request request) {
                System.out.println("Received from " + socket.getRemoteSocketAddress() + ": "
                        + request.getType() + " \"" + request.getText() + "\"");
                response = process(request);
            } else {
                // Something that is not a Request arrived: answer with an error, don't crash.
                response = new Response(false, "Unknown message");
            }
            out.writeObject(response);
            out.flush();
        }
    }

    /**
     * Decides what to do with a request, based on its type.
     * Not private so that the tests in the same package can call it.
     */
    Response process(Request request) {
        if (request.getType() == null) {
            return new Response(false, "The request has no type");
        }
        return switch (request.getType()) {
            case ECHO -> new Response(true, "Echo: " + request.getText());
        };
    }
}
