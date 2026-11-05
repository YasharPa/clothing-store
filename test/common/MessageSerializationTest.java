package common;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Checks that our messages survive being turned into bytes and back (what a socket does). */
class MessageSerializationTest {

    /** Writes the object to bytes and reads it back, like sending it over the network. */
    private static Object sendAndReceive(Object message) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(message);
        }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            return in.readObject();
        }
    }

    @Test
    void requestSurvivesTheTrip() throws Exception {
        Request received = (Request) sendAndReceive(new Request(RequestType.ECHO, "hello"));

        assertEquals(RequestType.ECHO, received.getType());
        assertEquals("hello", received.getText());
    }

    @Test
    void responseSurvivesTheTrip() throws Exception {
        Response received = (Response) sendAndReceive(new Response(true, "ok"));

        assertTrue(received.isSuccess());
        assertEquals("ok", received.getMessage());
    }

    @Test
    void userSurvivesTheTrip() throws Exception {
        User received = (User) sendAndReceive(new User("dana", Role.CASHIER, "Tel Aviv"));

        assertEquals("dana", received.getUsername());
        assertEquals(Role.CASHIER, received.getRole());
        assertEquals("Tel Aviv", received.getBranch());
    }
}
