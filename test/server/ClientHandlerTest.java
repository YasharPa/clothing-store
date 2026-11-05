package server;

import common.Request;
import common.RequestType;
import common.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientHandlerTest {

    // process() does not use the socket, so the tests do not need a real connection.
    private final ClientHandler handler = new ClientHandler(null);

    @Test
    void echoSendsTheSameTextBack() {
        Response response = handler.process(new Request(RequestType.ECHO, "hello"));

        assertTrue(response.isSuccess());
        assertEquals("Echo: hello", response.getMessage());
    }

    @Test
    void requestWithoutATypeGetsAnError() {
        Response response = handler.process(new Request(null, "hello"));

        assertFalse(response.isSuccess());
        assertEquals("The request has no type", response.getMessage());
    }
}
