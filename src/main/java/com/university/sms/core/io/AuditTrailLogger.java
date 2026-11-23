package com.university.sms.core.io;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * UNIT 3 - File streams (Anjay).
 * Appends one line per important action (login, CSV import, backup, archive export) to
 * {@code logs/audit-trail.log} using {@link FileOutputStream} in <b>append mode</b>, and reads the
 * latest entries back with {@link FileReader} + {@link BufferedReader}.
 * Methods are {@code synchronized} because many web requests (threads) can log at once.
 */
public class AuditTrailLogger {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final Path logFile;

    public AuditTrailLogger(Path logFile) {
        this.logFile = logFile;
    }

    public synchronized void log(String actor, String action, String details) {
        try {
            if (logFile.getParent() != null) {
                Files.createDirectories(logFile.getParent());
            }
            try (PrintWriter out = new PrintWriter(new OutputStreamWriter(
                    new FileOutputStream(logFile.toFile(), true), StandardCharsets.UTF_8))) {
                out.printf("%s | %-10s | %-18s | %s%n", LocalDateTime.now().format(FMT), actor, action, details);
            }
        } catch (IOException e) {
            // auditing must never break the main request - report and continue
            System.err.println("Audit log write failed: " + e.getMessage());
        }
    }

    /** Returns the last {@code n} lines (newest first) using a bounded Deque as a sliding window. */
    public synchronized List<String> tail(int n) {
        Deque<String> window = new ArrayDeque<>(n);
        if (!Files.exists(logFile)) {
            return List.of();
        }
        try (BufferedReader in = new BufferedReader(new FileReader(logFile.toFile(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = in.readLine()) != null) {
                if (window.size() == n) {
                    window.removeFirst();
                }
                window.addLast(line);
            }
        } catch (IOException e) {
            return List.of("Could not read audit log: " + e.getMessage());
        }
        List<String> newestFirst = new ArrayList<>(window);
        java.util.Collections.reverse(newestFirst);
        return newestFirst;
    }

    public Path getLogFile() {
        return logFile;
    }
}
