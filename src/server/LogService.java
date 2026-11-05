package server;

import shared.LogType;
import shared.dataTransfer.LogEntry;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Every line: time | type | user | details, appended to data/system.log.
 */
public final class LogService {
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static LogService instance;

    private final Path file;

    private LogService(Path file) {
        this.file = file;
    }

    public static synchronized LogService getInstance() {
        if (instance == null) {
            Path dir = Paths.get(System.getProperty("data.dir", "data"));
            try {
                Files.createDirectories(dir);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            instance = new LogService(dir.resolve("system.log"));
        }
        return instance;
    }

    public synchronized void log(LogType type, String user, String details) {
        String clean;
        if (details == null) {
            clean = "";
        } else {
            clean = details.replace('\r', ' ').replace('\n', ' ');
        }
        String userText = user;
        if (user == null || user.isBlank()) {
            userText = "-";
        }
        String line = TS.format(LocalDateTime.now()) + " | " + type + " | " + userText + " | " + clean;
        System.out.println("[LOG] " + line);
        try {
            Files.writeString(file, line + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("[LOG] Writing to the log failed: " + e.getMessage());
        }
    }

    public synchronized List<LogEntry> read(LogType filter, int max) {
        List<LogEntry> result = new ArrayList<>();
        if (!Files.exists(file)) {
            return result;
        }
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String[] p = line.split(" \\| ", 4);
                if (p.length < 4) {
                    continue;
                }
                try {
                    // p[0] = time | p[1] = type | p[2] = user | p[3] = details
                    LogType typ = LogType.valueOf(p[1]);
                    if (filter == null || filter == typ) {
                        result.add(new LogEntry(LocalDateTime.parse(p[0], TS), typ, p[2], p[3]));
                    }
                } catch (RuntimeException ignored) {
                    // Skip a faulty line
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Reading the log failed: " + e.getMessage(), e);
        }
        // keep only the newest "max" entries (at least one)
        int count = Math.max(1, max);
        int from = result.size() - count;
        if (from < 0) {
            from = 0;
        }
        return new ArrayList<>(result.subList(from, result.size()));
    }
}
