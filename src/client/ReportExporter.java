package client;

import shared.DataTransfer.ReportResult;
import java.awt.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** save a report txt file and open it in Notepad */
public final class ReportExporter {
    private ReportExporter() {
    }

    public static Path save(ReportResult report) throws IOException {
        Path dir = Paths.get("reports");
        Files.createDirectories(dir);
        String time = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now());
        Path file = dir.resolve("report-" + time + ".txt");

        // \uFEFF at the start is the BOM: it tells Notepad the file is UTF-8
        String text = "\uFEFF" + report.asText().replace("\r\n", "\n").replace("\n", "\r\n") + "\r\n";
        Files.writeString(file, text, StandardCharsets.UTF_8);
        return file;
    }

    /** Opens the file in Notepad (Windows); on other systems falls back to the default text editor. */
    public static boolean openInNotepad(Path file) {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            try {
                new ProcessBuilder("notepad.exe", file.toAbsolutePath().toString()).start();
                return true;
            } catch (IOException e) {
                // fall through to the desktop fallback
            }
        }
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(file.toFile());
                return true;
            }
        } catch (IOException | RuntimeException e) {
            // cannot open
        }
        return false;
    }
}
