package client;

import common.DataTransfer.ReportResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Checks the report file that is opened in Notepad. */
class ReportExporterTest {

    @TempDir
    Path folder; // JUnit gives each test its own empty temporary folder

    private final ReportResult report = new ReportResult("Sales report", List.of("Line one", "Line two ₪"));

    @Test
    void savesATextFileInTheGivenFolder() throws IOException {
        Path file = ReportExporter.save(report, folder);

        assertTrue(Files.exists(file));
        assertEquals(folder, file.getParent());
        assertTrue(file.getFileName().toString().matches("report-\\d{8}-\\d{6}\\.txt"));
    }

    @Test
    void createsTheFolderIfItDoesNotExist() throws IOException {
        Path file = ReportExporter.save(report, folder.resolve("reports"));

        assertTrue(Files.exists(file));
    }

    @Test
    void theFileStartsWithTheUtf8MarkSoNotepadShowsTheShekelSign() throws IOException {
        Path file = ReportExporter.save(report, folder);

        byte[] bytes = Files.readAllBytes(file);
        assertEquals((byte) 0xEF, bytes[0]);
        assertEquals((byte) 0xBB, bytes[1]);
        assertEquals((byte) 0xBF, bytes[2]);
    }

    @Test
    void linesAreSeparatedTheWindowsWay() throws IOException {
        Path file = ReportExporter.save(report, folder);

        String text = Files.readString(file, StandardCharsets.UTF_8);
        assertEquals("﻿Sales report\r\nLine one\r\nLine two ₪\r\n", text);
    }
}
