package com.university.sms.core.io;

import com.university.sms.core.exception.DataImportException;
import com.university.sms.core.generic.Result;
import com.university.sms.core.model.RollNumber;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * UNIT 3 - I/O Streams + UNIT 2 - Exceptions (Anjay).
 *
 * <p>Reads a bulk-admission CSV uploaded by the admin. The uploaded bytes arrive as an
 * {@link InputStream} (byte stream); {@link InputStreamReader} converts bytes to characters
 * using UTF-8 and {@link BufferedReader} reads them line by line.</p>
 *
 * <p>Every bad line raises a {@link DataImportException}; the exception is <b>caught per line</b>
 * so one bad row does not stop the whole import - the good rows are kept and the errors are
 * reported back (partial success via the generic {@link Result}).</p>
 *
 * Expected header:
 * <pre>rollNumber,firstName,lastName,email,phone,departmentCode,semester,section</pre>
 */
public class CsvStudentImporter {

    public static final String HEADER = "rollNumber,firstName,lastName,email,phone,departmentCode,semester,section";
    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");
    private static final int MAX_ROWS = 500;

    public Result<List<StudentImportRow>> read(InputStream input) {
        List<StudentImportRow> rows = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        Set<String> seenRolls = new HashSet<>();

        // try-with-resources: the reader (and the stream inside it) is closed automatically
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String header = reader.readLine();
            if (header == null) {
                return Result.failure("The file is empty");
            }
            if (!header.replace("﻿", "").trim().equalsIgnoreCase(HEADER)) {
                return Result.failure("Wrong header. Expected: " + HEADER);
            }

            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) {
                    continue;
                }
                if (rows.size() + errors.size() >= MAX_ROWS) {
                    errors.add("Stopped after " + MAX_ROWS + " rows (limit)");
                    break;
                }
                try {
                    StudentImportRow row = parseLine(lineNumber, line);
                    if (!seenRolls.add(row.rollNumber())) {
                        throw new DataImportException(lineNumber, "duplicate roll number " + row.rollNumber() + " in file");
                    }
                    rows.add(row);
                } catch (DataImportException e) {
                    errors.add(e.getMessage());          // remember and continue with the next line
                }
            }
        } catch (IOException e) {
            return Result.failure("Could not read the file: " + e.getMessage());
        }

        String summary = rows.size() + " valid row(s), " + errors.size() + " rejected";
        return errors.isEmpty() ? Result.success(rows, summary) : Result.partial(rows, errors, summary);
    }

    /** Parses one line; throws a checked exception describing exactly what is wrong. */
    StudentImportRow parseLine(int lineNumber, String line) throws DataImportException {
        String[] f = line.split(",", -1);
        if (f.length != 8) {
            throw new DataImportException(lineNumber, "expected 8 columns but found " + f.length);
        }
        for (int i = 0; i < f.length; i++) {
            f[i] = f[i].trim();
        }
        if (!RollNumber.isValid(f[0])) {
            throw new DataImportException(lineNumber, "invalid roll number '" + f[0] + "'");
        }
        if (f[1].isEmpty()) {
            throw new DataImportException(lineNumber, "first name is required");
        }
        if (!EMAIL.matcher(f[3]).matches()) {
            throw new DataImportException(lineNumber, "invalid email '" + f[3] + "'");
        }
        if (f[5].isEmpty()) {
            throw new DataImportException(lineNumber, "department code is required");
        }
        int semester;
        try {
            semester = Integer.parseInt(f[6]);
        } catch (NumberFormatException nfe) {
            // exception chaining: wrap the low-level exception as the cause
            throw new DataImportException(lineNumber, "semester '" + f[6] + "' is not a number", nfe);
        }
        if (semester < 1 || semester > 8) {
            throw new DataImportException(lineNumber, "semester must be between 1 and 8");
        }
        String section = f[7].isEmpty() ? "A" : f[7].toUpperCase();
        return new StudentImportRow(lineNumber, f[0].toUpperCase(), f[1], f[2], f[3].toLowerCase(), f[4],
                f[5].toUpperCase(), semester, section);
    }

    /** Sample file content offered as a template download. */
    public static String sampleCsv() {
        return HEADER + "\n"
                + "STU2026101,Rahul,Mehta,rahul.mehta@apex.edu.in,9876500101,CSE,5,A\n"
                + "STU2026102,Kavya,Rao,kavya.rao@apex.edu.in,9876500102,ECE,5,A\n";
    }
}
