package com.university.sms.core.io;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;

/**
 * UNIT 3 - Byte streams (Anjay).
 * Classic byte-by-buffer copy loop with {@link InputStream#read(byte[])}. Used to stream the
 * SQLite archive file to the browser and to copy backup files.
 */
public final class StreamUtils {

    private static final int BUFFER_SIZE = 8 * 1024;

    private StreamUtils() {
    }

    /** Copies everything from in to out, returns the number of bytes copied. */
    public static long copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        long total = 0;
        int read;
        while ((read = in.read(buffer)) != -1) {   // -1 means end of stream
            out.write(buffer, 0, read);
            total += read;
        }
        out.flush();
        return total;
    }

    /** Streams a file with FileInputStream (closed automatically). */
    public static long copyFile(Path source, OutputStream out) throws IOException {
        try (FileInputStream in = new FileInputStream(source.toFile())) {
            return copy(in, out);
        }
    }

    /** Human-readable size, e.g. 12.4 KB. */
    public static String humanSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }
}
