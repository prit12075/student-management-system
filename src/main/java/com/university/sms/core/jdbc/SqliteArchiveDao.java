package com.university.sms.core.jdbc;

import com.university.sms.core.collections.Seat;
import com.university.sms.core.model.FacultyRecord;
import com.university.sms.core.model.StudentRecord;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * UNIT 4 - JDBC with SQLite (Arindam).
 *
 * <p>Data Access Object that keeps an <b>offline semester archive</b> in a single SQLite file
 * ({@code archive/sms_archive.db}). The live system uses MySQL/H2 through Hibernate; this class
 * deliberately uses the <b>plain JDBC API</b> so every JDBC step is visible:</p>
 * <ol>
 *   <li>Load/locate the driver - {@link DriverManager#getConnection(String)} with a {@code jdbc:sqlite:} URL</li>
 *   <li>{@link Connection} - opened in try-with-resources so it is always closed</li>
 *   <li>{@link Statement} for DDL ({@code CREATE TABLE}), {@link PreparedStatement} with {@code ?} placeholders for DML</li>
 *   <li><b>Transactions</b> - {@code setAutoCommit(false)}, {@code commit()}, {@code rollback()} on error</li>
 *   <li><b>Batch processing</b> - {@code addBatch()} / {@code executeBatch()}</li>
 *   <li>{@link ResultSet} navigation and {@link ResultSetMetaData} / {@link DatabaseMetaData}</li>
 * </ol>
 * The same file is opened by the JavaFX desktop client, so the archive can be browsed without the server.
 */
public class SqliteArchiveDao {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final Path dbFile;
    private final String url;

    public SqliteArchiveDao(Path dbFile) {
        this.dbFile = dbFile;
        this.url = "jdbc:sqlite:" + dbFile.toAbsolutePath();
    }

    private Connection connect() throws SQLException {
        try {
            if (dbFile.getParent() != null) {
                Files.createDirectories(dbFile.getParent());
            }
        } catch (IOException e) {
            throw new SQLException("Cannot create archive folder", e);
        }
        return DriverManager.getConnection(url);
    }

    /** DDL with a plain Statement. IF NOT EXISTS makes it safe to call many times. */
    public void createSchema() throws SQLException {
        try (Connection con = connect(); Statement st = con.createStatement()) {
            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS archived_students (
                        roll_number TEXT PRIMARY KEY,
                        full_name   TEXT NOT NULL,
                        email       TEXT,
                        department  TEXT NOT NULL,
                        semester    INTEGER NOT NULL,
                        attendance  REAL NOT NULL,
                        average     REAL NOT NULL,
                        sgpa        REAL NOT NULL,
                        backlogs    INTEGER NOT NULL,
                        status      TEXT NOT NULL
                    )""");
            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS archived_faculty (
                        employee_id  TEXT PRIMARY KEY,
                        full_name    TEXT NOT NULL,
                        department   TEXT NOT NULL,
                        designation  TEXT,
                        subjects     TEXT
                    )""");
            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS seating_plan (
                        roll_number  TEXT PRIMARY KEY,
                        exam_name    TEXT NOT NULL,
                        room         TEXT NOT NULL,
                        seat_row     INTEGER NOT NULL,
                        seat_col     INTEGER NOT NULL,
                        student_name TEXT,
                        department   TEXT
                    )""");
            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS snapshot_runs (
                        id          INTEGER PRIMARY KEY AUTOINCREMENT,
                        taken_at    TEXT NOT NULL,
                        taken_by    TEXT,
                        students    INTEGER NOT NULL,
                        faculty     INTEGER NOT NULL
                    )""");
        }
    }

    /**
     * Replaces the archive with a fresh snapshot inside ONE transaction: either every row is
     * written, or (on any error) nothing changes.
     *
     * @return number of student rows written
     */
    public int exportSnapshot(List<StudentRecord> students, List<FacultyRecord> faculty, String takenBy)
            throws SQLException {
        createSchema();
        try (Connection con = connect()) {
            con.setAutoCommit(false);                              // start transaction
            try (Statement clear = con.createStatement();
                 PreparedStatement insStudent = con.prepareStatement(
                         "INSERT INTO archived_students VALUES (?,?,?,?,?,?,?,?,?,?)");
                 PreparedStatement insFaculty = con.prepareStatement(
                         "INSERT INTO archived_faculty VALUES (?,?,?,?,?)");
                 PreparedStatement insRun = con.prepareStatement(
                         "INSERT INTO snapshot_runs(taken_at, taken_by, students, faculty) VALUES (?,?,?,?)")) {

                clear.executeUpdate("DELETE FROM archived_students");
                clear.executeUpdate("DELETE FROM archived_faculty");

                for (StudentRecord s : students) {
                    insStudent.setString(1, s.getRoll());
                    insStudent.setString(2, s.getFullName());
                    insStudent.setString(3, s.getEmail());
                    insStudent.setString(4, s.getDepartmentCode());
                    insStudent.setInt(5, s.getSemester());
                    insStudent.setDouble(6, s.getAttendancePercentage());
                    insStudent.setDouble(7, s.getAveragePercentage());
                    insStudent.setDouble(8, s.getSgpa());
                    insStudent.setInt(9, s.getBacklogs());
                    insStudent.setString(10, s.isAttendanceShort() ? "SHORTAGE" : "ELIGIBLE");
                    insStudent.addBatch();                         // queue the row
                }
                int[] studentCounts = insStudent.executeBatch();   // send all rows in one go

                for (FacultyRecord f : faculty) {
                    insFaculty.setString(1, f.getEmployeeId());
                    insFaculty.setString(2, f.getFullName());
                    insFaculty.setString(3, f.getDepartmentCode());
                    insFaculty.setString(4, f.getDesignation());
                    insFaculty.setString(5, String.join(", ", f.getSubjectCodes()));
                    insFaculty.addBatch();
                }
                insFaculty.executeBatch();

                insRun.setString(1, LocalDateTime.now().format(TS));
                insRun.setString(2, takenBy);
                insRun.setInt(3, students.size());
                insRun.setInt(4, faculty.size());
                insRun.executeUpdate();

                con.commit();                                      // make everything permanent
                return studentCounts.length;
            } catch (SQLException e) {
                con.rollback();                                    // undo the partial snapshot
                throw e;
            }
        }
    }

    /** SELECT all rows, ordered, mapped row-by-row from the ResultSet. */
    public List<ArchivedStudent> findAllStudents() throws SQLException {
        return query("SELECT * FROM archived_students ORDER BY department, roll_number");
    }

    /** Parameterised query - '?' prevents SQL injection. */
    public List<ArchivedStudent> findByDepartment(String department) throws SQLException {
        return query("SELECT * FROM archived_students WHERE department = ? ORDER BY sgpa DESC", department);
    }

    public List<ArchivedStudent> searchByName(String fragment) throws SQLException {
        return query("SELECT * FROM archived_students WHERE lower(full_name) LIKE ? OR lower(roll_number) LIKE ?",
                "%" + fragment.toLowerCase() + "%", "%" + fragment.toLowerCase() + "%");
    }

    /** Aggregation done by the database (GROUP BY) rather than in Java. */
    public Map<String, Double> departmentAverageSgpa() throws SQLException {
        Map<String, Double> result = new LinkedHashMap<>();
        createSchema();
        try (Connection con = connect();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT department, ROUND(AVG(sgpa), 2) AS avg_sgpa FROM archived_students GROUP BY department ORDER BY department");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.put(rs.getString("department"), rs.getDouble("avg_sgpa"));
            }
        }
        return result;
    }

    /** Reads facts about the database itself through DatabaseMetaData. */
    public ArchiveSummary summary() throws SQLException {
        createSchema();
        try (Connection con = connect()) {
            DatabaseMetaData meta = con.getMetaData();
            List<String> tables = new ArrayList<>();
            try (ResultSet rs = meta.getTables(null, null, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    String name = rs.getString("TABLE_NAME");
                    if (!name.startsWith("sqlite_")) {
                        tables.add(name);
                    }
                }
            }
            Map<String, Integer> counts = new LinkedHashMap<>();
            try (Statement st = con.createStatement()) {
                for (String t : tables) {
                    try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + t)) {
                        counts.put(t, rs.next() ? rs.getInt(1) : 0);
                    }
                }
            }
            String last = "never";
            try (Statement st = con.createStatement();
                 ResultSet rs = st.executeQuery("SELECT taken_at, taken_by FROM snapshot_runs ORDER BY id DESC LIMIT 1")) {
                if (rs.next()) {
                    last = rs.getString(1) + " by " + rs.getString(2);
                }
            }
            long size = Files.exists(dbFile) ? sizeOf() : 0;
            return new ArchiveSummary(meta.getDatabaseProductName(), meta.getDatabaseProductVersion(),
                    meta.getDriverName() + " " + meta.getDriverVersion(), url, tables, counts, last, size);
        }
    }

    /** Column names of a table using ResultSetMetaData (shown in the desktop client's "About"). */
    public List<String> columnsOf(String table) throws SQLException {
        if (!table.matches("[a-z_]+")) {
            throw new SQLException("Illegal table name");
        }
        List<String> cols = new ArrayList<>();
        try (Connection con = connect(); Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM " + table + " LIMIT 1")) {
            ResultSetMetaData md = rs.getMetaData();
            for (int i = 1; i <= md.getColumnCount(); i++) {
                cols.add(md.getColumnName(i) + " : " + md.getColumnTypeName(i));
            }
        }
        return cols;
    }

    // ------------------------------------------------------------------ exam seating (novel feature)

    /** Replaces the stored seating plan in one transaction with a batch insert. */
    public int saveSeatingPlan(String examName, java.util.Collection<Seat> seats) throws SQLException {
        createSchema();
        try (Connection con = connect()) {
            con.setAutoCommit(false);
            try (Statement clear = con.createStatement();
                 PreparedStatement ins = con.prepareStatement("INSERT INTO seating_plan VALUES (?,?,?,?,?,?,?)")) {
                clear.executeUpdate("DELETE FROM seating_plan");
                for (Seat s : seats) {
                    ins.setString(1, s.rollNumber());
                    ins.setString(2, examName);
                    ins.setString(3, s.room());
                    ins.setInt(4, s.row());
                    ins.setInt(5, s.column());
                    ins.setString(6, s.studentName());
                    ins.setString(7, s.department());
                    ins.addBatch();
                }
                int n = ins.executeBatch().length;
                con.commit();
                return n;
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        }
    }

    /** Looks up one student's seat (used on the hall ticket). Optional.empty() if not allocated. */
    public java.util.Optional<Seat> findSeat(String rollNumber) throws SQLException {
        createSchema();
        try (Connection con = connect();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT room, seat_row, seat_col, student_name, department FROM seating_plan WHERE roll_number = ?")) {
            ps.setString(1, rollNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return java.util.Optional.of(new Seat(rs.getString(1), rs.getInt(2), rs.getInt(3),
                            rollNumber, rs.getString(4), rs.getString(5)));
                }
            }
        }
        return java.util.Optional.empty();
    }

    public String seatingExamName() throws SQLException {
        createSchema();
        try (Connection con = connect(); Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT exam_name FROM seating_plan LIMIT 1")) {
            return rs.next() ? rs.getString(1) : null;
        }
    }

    private List<ArchivedStudent> query(String sql, String... params) throws SQLException {
        createSchema();
        List<ArchivedStudent> list = new ArrayList<>();
        try (Connection con = connect(); PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setString(i + 1, params[i]);                    // JDBC parameters are 1-based
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {                                // move the cursor row by row
                    list.add(new ArchivedStudent(
                            rs.getString("roll_number"), rs.getString("full_name"), rs.getString("email"),
                            rs.getString("department"), rs.getInt("semester"), rs.getDouble("attendance"),
                            rs.getDouble("average"), rs.getDouble("sgpa"), rs.getInt("backlogs"),
                            rs.getString("status")));
                }
            }
        }
        return list;
    }

    private long sizeOf() {
        try {
            return Files.size(dbFile);
        } catch (IOException e) {
            return 0;
        }
    }

    public Path getDbFile() {
        return dbFile;
    }

    public String getUrl() {
        return url;
    }
}
