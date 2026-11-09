package client;

import common.DataTransfer.LogEntry;
import common.DataTransfer.LogQuery;
import common.LogType;
import common.ServiceException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;

/**
 * The log screen: the admin chooses an action type / all and sees the newest entries
 */
public class LogsMenu {
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final int SHOWN_LINES = 100;

    private final BufferedReader in;
    private final PrintStream out;
    private final Function<LogQuery, List<LogEntry>> source;

    public LogsMenu(BufferedReader in, PrintStream out, Function<LogQuery, List<LogEntry>> source) {
        this.in = in;
        this.out = out;
        this.source = source;
    }

    public void run() throws IOException {
        LogType[] types = LogType.values();
        while (true) {
            out.println();
            out.println("=== System log ===");
            out.println("0. Back");
            out.println("1. All actions");
            for (int i = 0; i < types.length; i++) {
                out.println((i + 2) + ". " + types[i].getLabel() + "  [" + types[i] + "]");
            }
            int choice = askChoice(types.length + 1);
            if (choice == 0) {
                return;
            }
            LogType filter = null;
            if (choice > 1) {
                filter = types[choice - 2];
            }
            show(filter);
        }
    }

    private void show(LogType filter) {
        try {
            List<LogEntry> entries = source.apply(new LogQuery(filter, SHOWN_LINES));
            out.println(format(entries));
        } catch (ServiceException e) {
            out.println("Error: " + e.getMessage());
        }
    }

    /** the text of a list of entries */
    public static String format(List<LogEntry> entries) {
        StringBuilder sb = new StringBuilder("-- " + entries.size() + " latest entries --");
        for (LogEntry e : entries) {
            sb.append(System.lineSeparator())
                    .append(TS.format(e.time())).append(" | ")
                    .append(e.type().getLabel()).append(" | ")
                    .append(e.user()).append(" | ")
                    .append(e.details());
        }
        return sb.toString();
    }

    /** asks until the user types a whole number from 0 to max */
    private int askChoice(int max) throws IOException {
        while (true) {
            out.print("Choice: ");
            out.flush();
            String line = in.readLine();
            if (line == null) {
                return 0;
            }
            try {
                int value = Integer.parseInt(line.trim());
                if (value >= 0 && value <= max) {
                    return value;
                }
            } catch (NumberFormatException e) {
                // ask again
            }
            out.println("Please type a number from 0 to " + max);
        }
    }
}
