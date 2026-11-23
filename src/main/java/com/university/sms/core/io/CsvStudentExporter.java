package com.university.sms.core.io;

import com.university.sms.core.model.StudentRecord;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * UNIT 3 - I/O Streams, character streams (Anjay).
 * Writes the performance register as CSV. Works with any {@link Writer} (file, HTTP response,
 * in-memory string) - the program is written against the abstract stream class
 * (polymorphism applied to I/O).
 */
public class CsvStudentExporter {

    public static final String HEADER =
            "rollNumber,name,department,semester,attendancePercent,averagePercent,sgpa,backlogs,attendanceStatus";

    /** Core method: writes to any character stream. */
    public int write(List<StudentRecord> students, Writer out) throws IOException {
        PrintWriter pw = new PrintWriter(new BufferedWriter(out));
        pw.println(HEADER);
        for (StudentRecord s : students) {
            pw.printf("%s,%s,%s,%d,%.1f,%.1f,%.2f,%d,%s%n",
                    s.getRoll(), escape(s.getFullName()), s.getDepartmentCode(), s.getSemester(),
                    s.getAttendancePercentage(), s.getAveragePercentage(), s.getSgpa(), s.getBacklogs(),
                    s.isAttendanceShort() ? "SHORTAGE" : "ELIGIBLE");
        }
        pw.flush();
        if (pw.checkError()) {
            throw new IOException("Error while writing CSV");
        }
        return students.size();
    }

    /** Byte-stream overload: wraps an OutputStream (e.g. the HTTP response) in a UTF-8 writer. */
    public int write(List<StudentRecord> students, OutputStream out) throws IOException {
        Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
        int n = write(students, writer);
        writer.flush();
        return n;
    }

    /** File overload: creates the parent folder and writes the file (closed by try-with-resources). */
    public int write(List<StudentRecord> students, Path file) throws IOException {
        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }
        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            return write(students, writer);
        }
    }

    private static String escape(String value) {
        if (value.contains(",") || value.contains("\"")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
