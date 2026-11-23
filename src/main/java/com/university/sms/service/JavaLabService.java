package com.university.sms.service;

import com.university.sms.core.collections.AcademicAnalyticsEngine;
import com.university.sms.core.collections.AnalyticsReport;
import com.university.sms.core.concurrent.AttendanceAlertMonitor;
import com.university.sms.core.concurrent.ConcurrentReportEngine;
import com.university.sms.core.concurrent.PortalActivityMonitor;
import com.university.sms.core.concurrent.ThreadLifecycleTracer;
import com.university.sms.core.exception.SmsException;
import com.university.sms.core.generic.InMemoryRepository;
import com.university.sms.core.generic.Pair;
import com.university.sms.core.generic.Result;
import com.university.sms.core.grading.Grade;
import com.university.sms.core.grading.GradingPolicy;
import com.university.sms.core.grading.RelativeGradingPolicy;
import com.university.sms.core.io.AuditTrailLogger;
import com.university.sms.core.io.CsvStudentExporter;
import com.university.sms.core.io.CsvStudentImporter;
import com.university.sms.core.io.SerializationBackupService;
import com.university.sms.core.io.StudentImportRow;
import com.university.sms.core.jdbc.ArchiveSummary;
import com.university.sms.core.jdbc.ArchivedStudent;
import com.university.sms.core.jdbc.SqliteArchiveDao;
import com.university.sms.core.model.FacultyRecord;
import com.university.sms.core.model.StudentRecord;
import com.university.sms.dto.StudentPerformanceDto;
import com.university.sms.entity.Course;
import com.university.sms.entity.Department;
import com.university.sms.entity.Faculty;
import com.university.sms.entity.Mark;
import com.university.sms.entity.Student;
import com.university.sms.entity.Subject;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Integration layer (Prit Patel - architecture & integration lead).
 *
 * <p>Bridges the Spring/JPA web application and the plain-Java "core" modules written for
 * Units 1-5. It converts JPA entities into core {@link StudentRecord}/{@link FacultyRecord}
 * objects (the <b>Adapter</b> idea) and then calls each member's module:</p>
 * <ul>
 *   <li>Grading policies &amp; generics - Prit Patel</li>
 *   <li>CSV import/export, serialization backup, audit trail, exceptions - Anjay</li>
 *   <li>Thread pool reports, producer-consumer alerts, lifecycle trace, activity monitor - Vivekanand</li>
 *   <li>SQLite JDBC archive and Collections analytics - Arindam</li>
 * </ul>
 */
@Service
public class JavaLabService {

    private final UniversityErpService erpService;
    private final AuditTrailLogger auditLogger;
    private final SqliteArchiveDao archiveDao;
    private final PortalActivityMonitor activityMonitor;

    private final CsvStudentExporter csvExporter = new CsvStudentExporter();
    private final CsvStudentImporter csvImporter = new CsvStudentImporter();
    private final SerializationBackupService backupService = new SerializationBackupService();
    private final AcademicAnalyticsEngine analyticsEngine = new AcademicAnalyticsEngine();
    private final ConcurrentReportEngine reportEngine = new ConcurrentReportEngine();

    /** Generic repository used as an in-memory cache of the converted records. */
    private final InMemoryRepository<StudentRecord, Long> recordCache = new InMemoryRepository<>();

    public JavaLabService(UniversityErpService erpService, AuditTrailLogger auditLogger,
                          SqliteArchiveDao archiveDao, PortalActivityMonitor activityMonitor) {
        this.erpService = erpService;
        this.auditLogger = auditLogger;
        this.archiveDao = archiveDao;
        this.activityMonitor = activityMonitor;
    }

    // ================= Adapter: JPA entities -> plain Java core objects =================

    public List<StudentRecord> loadStudentRecords() {
        List<StudentRecord> records = new ArrayList<>();
        for (StudentPerformanceDto p : erpService.getAllStudentPerformances(null)) {
            Student s = p.getStudent();
            StudentRecord r = new StudentRecord(s.getId(), s.getRollNumber(), s.getFirstName(), s.getLastName(),
                    s.getEmail(), s.getDepartment().getCode(), s.getSemester())
                    .withPerformance(p.getAttendancePercentage(), p.getAveragePercentage(), p.getSgpa(), p.getBacklogs());
            for (Mark m : erpService.getMarksForStudent(s.getId())) {
                r.addSubjectScore(m.getPercentage());
            }
            records.add(r);
        }
        recordCache.saveAll(records);
        return records;
    }

    public List<FacultyRecord> loadFacultyRecords() {
        List<FacultyRecord> list = new ArrayList<>();
        for (Faculty f : erpService.getAllFaculty()) {
            FacultyRecord r = new FacultyRecord(f.getId(), f.getEmployeeId(), f.getFirstName(), f.getLastName(),
                    f.getEmail(), f.getDepartment() != null ? f.getDepartment().getCode() : "GEN", f.getDesignation());
            for (Subject sub : erpService.getSubjectsByFaculty(f.getId())) {
                r.teaches(sub.getCode());
            }
            list.add(r);
        }
        return list;
    }

    public int cachedRecordCount() {
        return recordCache.count();
    }

    // ================= Prit Patel: polymorphic grading simulator =================

    /** One row of the simulator table. */
    public record GradingRow(String roll, String name, String dept, double percentage,
                             Grade absolute, Grade simulated, boolean changed) {
    }

    public record GradingSimulation(String policyName, String policyDescription, List<GradingRow> rows,
                                    Double mean, Double stdDev, long changedCount) {
    }

    public GradingSimulation simulateGrading(String policyKey) {
        List<StudentRecord> students = loadStudentRecords();
        List<Double> percentages = students.stream().map(StudentRecord::getAveragePercentage).toList();

        GradingPolicy baseline = GradingPolicy.of("absolute");
        GradingPolicy chosen = GradingPolicy.of(policyKey);        // runtime polymorphism: type decided now
        List<Grade> before = baseline.assignAll(percentages);
        List<Grade> after = chosen.assignAll(percentages);

        List<GradingRow> rows = new ArrayList<>();
        for (int i = 0; i < students.size(); i++) {
            StudentRecord s = students.get(i);
            rows.add(new GradingRow(s.getRoll(), s.getFullName(), s.getDepartmentCode(), s.getAveragePercentage(),
                    before.get(i), after.get(i), before.get(i) != after.get(i)));
        }
        Double mean = null;
        Double sd = null;
        if (chosen instanceof RelativeGradingPolicy rel) {         // pattern matching for instanceof
            mean = Math.round(rel.getMean() * 100.0) / 100.0;
            sd = Math.round(rel.getStandardDeviation() * 100.0) / 100.0;
        }
        long changed = rows.stream().filter(GradingRow::changed).count();
        return new GradingSimulation(chosen.getName(), chosen.getDescription(), rows, mean, sd, changed);
    }

    // ================= Arindam: Collections analytics =================

    public AnalyticsReport analytics(String searchRoll) {
        return analyticsEngine.analyse(loadStudentRecords(), 5, searchRoll);
    }

    // ================= Anjay: I/O streams =================

    public int exportCsv(OutputStream out, String actor) throws IOException {
        int n = csvExporter.write(loadStudentRecords(), out);
        auditLogger.log(actor, "CSV_EXPORT", n + " student rows exported");
        return n;
    }

    /** Reads the uploaded CSV, then admits every valid row as a real student account. */
    public Result<Integer> importCsv(InputStream in, String actor) {
        Result<List<StudentImportRow>> parsed = csvImporter.read(in);
        if (parsed.getValue() == null) {
            auditLogger.log(actor, "CSV_IMPORT_FAIL", parsed.getSummary());
            return Result.failure(parsed.getSummary());
        }
        List<String> errors = new ArrayList<>(parsed.getErrors());
        int created = 0;
        for (StudentImportRow row : parsed.getValue()) {
            try {
                Department dept = erpService.getAllDepartments().stream()
                        .filter(d -> d.getCode().equalsIgnoreCase(row.departmentCode()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("unknown department " + row.departmentCode()));
                Optional<Course> course = erpService.getAllCourses().stream()
                        .filter(c -> c.getDepartment() != null && c.getDepartment().getId().equals(dept.getId()))
                        .findFirst();
                if (course.isEmpty()) {
                    throw new IllegalArgumentException("department " + dept.getCode() + " has no course");
                }
                boolean exists = erpService.getAllStudents().stream()
                        .anyMatch(s -> s.getRollNumber().equalsIgnoreCase(row.rollNumber())
                                || s.getEmail().equalsIgnoreCase(row.email()));
                if (exists) {
                    throw new IllegalArgumentException("roll number or email already registered");
                }
                erpService.createOrUpdateStudent(null, row.rollNumber(), row.firstName(), row.lastName(),
                        row.email(), row.phone(), null, null, null, null, null,
                        row.semester(), row.section(), 2026, dept.getId(), course.get().getId(), null);
                created++;
            } catch (RuntimeException e) {
                errors.add("Line " + row.lineNumber() + ": " + e.getMessage());
            }
        }
        String summary = created + " student(s) admitted, " + errors.size() + " row(s) rejected";
        auditLogger.log(actor, "CSV_IMPORT", summary);
        return errors.isEmpty() ? Result.success(created, summary) : Result.partial(created, errors, summary);
    }

    public long backup(OutputStream out, String actor) throws IOException {
        long n = backupService.backup(loadStudentRecords(), out);
        auditLogger.log(actor, "BACKUP_CREATE", n + " records serialized");
        return n;
    }

    public SerializationBackupService.BackupContents verifyBackup(InputStream in, String actor) throws SmsException {
        SerializationBackupService.BackupContents contents = backupService.restore(in);
        auditLogger.log(actor, "BACKUP_VERIFY", contents.students().size() + " records read back");
        return contents;
    }

    public List<String> auditTail(int n) {
        return auditLogger.tail(n);
    }

    public void audit(String actor, String action, String details) {
        auditLogger.log(actor, action, details);
    }

    // ================= Vivekanand: multithreading =================

    public Pair<ConcurrentReportEngine.ReportRun, ConcurrentReportEngine.ReportRun> compareReportGeneration(int threads)
            throws InterruptedException {
        List<StudentRecord> students = loadStudentRecords();
        ConcurrentReportEngine.ReportRun sequential = reportEngine.generateSequential(students);
        ConcurrentReportEngine.ReportRun parallel = reportEngine.generateParallel(students, threads);
        return Pair.of(sequential, parallel);
    }

    public List<String> runAlertPipeline() throws InterruptedException {
        return new AttendanceAlertMonitor().run(loadStudentRecords());
    }

    public List<String> traceThreadLifecycle() throws InterruptedException {
        return new ThreadLifecycleTracer().trace();
    }

    public Pair<Long, Long> raceConditionDemo() throws InterruptedException {
        long unsafe = PortalActivityMonitor.raceConditionDemo(8, 50_000, false);
        long safe = PortalActivityMonitor.raceConditionDemo(8, 50_000, true);
        return Pair.of(unsafe, safe);
    }

    public PortalActivityMonitor getActivityMonitor() {
        return activityMonitor;
    }

    // ================= Arindam: JDBC archive =================

    public int createArchiveSnapshot(String actor) throws SQLException {
        int n = archiveDao.exportSnapshot(loadStudentRecords(), loadFacultyRecords(), actor);
        auditLogger.log(actor, "ARCHIVE_SNAPSHOT", n + " students written to SQLite");
        return n;
    }

    public ArchiveSummary archiveSummary() throws SQLException {
        return archiveDao.summary();
    }

    public List<ArchivedStudent> archivedStudents(String search) throws SQLException {
        return (search == null || search.isBlank()) ? archiveDao.findAllStudents() : archiveDao.searchByName(search);
    }

    public Map<String, Double> archiveDepartmentAverages() throws SQLException {
        return archiveDao.departmentAverageSgpa();
    }

    public SqliteArchiveDao getArchiveDao() {
        return archiveDao;
    }
}
