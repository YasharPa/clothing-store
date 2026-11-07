package common;

import common.DataTransfer.LogEntry;
import common.DataTransfer.LogQuery;
import common.DataTransfer.ReportMode;
import common.DataTransfer.ReportRequest;
import common.DataTransfer.ReportResult;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Checks that what part 3 sends over the connection survives being turned into bytes and back. */
class DataTransferSerializationTest {

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
    void reportRequestSurvivesTheTrip() throws Exception {
        List<Integer> products = new ArrayList<>();
        products.add(1);
        List<String> categories = new ArrayList<>();
        categories.add("Tops");
        ReportRequest request = new ReportRequest(ReportMode.BY_PRODUCT, 2, LocalDate.of(2026, 1, 5), products, categories);

        Object received = sendAndReceive(request);

        assertEquals(request, received);
    }

    @Test
    void reportResultSurvivesTheTrip() throws Exception {
        List<String> lines = new ArrayList<>();
        lines.add("line 1");
        lines.add("line 2");
        ReportResult result = new ReportResult("Title", lines);

        Object received = sendAndReceive(result);

        assertEquals(result, received);
    }

    @Test
    void logQuerySurvivesTheTrip() throws Exception {
        LogQuery query = new LogQuery(LogType.SALE, 20);

        Object received = sendAndReceive(query);

        assertEquals(query, received);
    }

    @Test
    void logEntrySurvivesTheTrip() throws Exception {
        LogEntry entry = new LogEntry(LocalDateTime.of(2026, 1, 5, 10, 30), LogType.LOGIN, "moshe", "Logged in");

        Object received = sendAndReceive(entry);

        assertEquals(entry, received);
    }

    @Test
    void aCustomerSurvivesTheTripWithItsHistory() throws Exception {
        Customer customer = Customer.create(CustomerType.VIP, "300000002", "Liat", "0502222222", 4, 800.5);

        Customer received = (Customer) sendAndReceive(customer);

        assertEquals(CustomerType.VIP, received.getType());
        assertEquals("Liat", received.getFullName());
        assertEquals(4, received.getPurchaseCount());
        assertEquals(800.5, received.getTotalSpent(), 0.001);
    }
}
