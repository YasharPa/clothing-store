package client;

import common.DataTransfer.LogEntry;
import common.DataTransfer.LogQuery;
import common.LogType;
import common.ServiceException;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Checks the log screen with typed input, and without a server: the entries come from a small fake source. */
class LogsMenuTest {

    private final LogEntry entry = new LogEntry(LocalDateTime.of(2026, 1, 5, 10, 30, 15), LogType.LOGIN, "moshe", "Logged in");

    /** The fake source: remembers what was asked, and either answers with one entry or refuses. */
    private static class FakeSource implements java.util.function.Function<LogQuery, List<LogEntry>> {
        final List<LogQuery> asked = new ArrayList<>();
        final List<LogEntry> answer;
        final boolean refuse;

        FakeSource(List<LogEntry> answer, boolean refuse) {
            this.answer = answer;
            this.refuse = refuse;
        }

        @Override
        public List<LogEntry> apply(LogQuery query) {
            asked.add(query);
            if (refuse) {
                throw new ServiceException("Viewing logs is allowed for admins or shift manager only");
            }
            return answer;
        }
    }

    /** Runs the menu with the typed lines, and returns everything it printed. */
    private String run(String typed, FakeSource source) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(bytes, true, StandardCharsets.UTF_8);

        new LogsMenu(new BufferedReader(new StringReader(typed)), out, source).run();

        return bytes.toString(StandardCharsets.UTF_8);
    }

    @Test
    void formatShowsTimeTypeUserAndDetails() {
        String text = LogsMenu.format(List.of(entry));

        assertTrue(text.contains("1 latest entries"));
        assertTrue(text.contains("05/01/2026 10:30:15 | Login | moshe | Logged in"));
    }

    @Test
    void choiceOneAsksForAllActionTypes() throws IOException {
        FakeSource source = new FakeSource(List.of(entry), false);

        String screen = run("1\n0\n", source);

        assertEquals(1, source.asked.size());
        assertNull(source.asked.get(0).type());
        assertTrue(screen.contains("moshe"));
    }

    @Test
    void choosingAnActionTypeAsksForThatType() throws IOException {
        FakeSource source = new FakeSource(List.of(), false);
        int choiceOfSale = LogType.SALE.ordinal() + 2; // 0 = back, 1 = all, then the types in order

        run(choiceOfSale + "\n0\n", source);

        assertEquals(LogType.SALE, source.asked.get(0).type());
    }

    @Test
    void aRefusalIsShownAndTheMenuGoesOn() throws IOException {
        FakeSource source = new FakeSource(List.of(), true);

        String screen = run("1\n0\n", source);

        assertTrue(screen.contains("Error: Viewing logs is allowed"));
    }

    @Test
    void aWrongChoiceIsAskedAgain() throws IOException {
        FakeSource source = new FakeSource(List.of(), false);

        String screen = run("abc\n99\n0\n", source);

        assertTrue(screen.contains("Please type a number from 0 to"));
        assertTrue(source.asked.isEmpty());
    }
}
