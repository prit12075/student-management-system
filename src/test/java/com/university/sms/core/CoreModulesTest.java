package com.university.sms.core;

import com.university.sms.core.collections.AcademicAnalyticsEngine;
import com.university.sms.core.collections.AnalyticsReport;
import com.university.sms.core.concurrent.AlertQueue;
import com.university.sms.core.concurrent.AttendanceAlertMonitor;
import com.university.sms.core.concurrent.ConcurrentReportEngine;
import com.university.sms.core.concurrent.PortalActivityMonitor;
import com.university.sms.core.concurrent.ThreadLifecycleTracer;
import com.university.sms.core.exception.InvalidMarksException;
import com.university.sms.core.exception.MarksValidator;
import com.university.sms.core.exception.SmsException;
import com.university.sms.core.generic.GenericUtils;
import com.university.sms.core.generic.InMemoryRepository;
import com.university.sms.core.generic.Pair;
import com.university.sms.core.generic.Result;
import com.university.sms.core.grading.Grade;
import com.university.sms.core.grading.GradeCalculator;
import com.university.sms.core.grading.GradingPolicy;
import com.university.sms.core.grading.ModerationGradingPolicy;
import com.university.sms.core.grading.RelativeGradingPolicy;
import com.university.sms.core.io.CsvStudentExporter;
import com.university.sms.core.io.CsvStudentImporter;
import com.university.sms.core.io.SerializationBackupService;
import com.university.sms.core.io.StudentImportRow;
import com.university.sms.core.jdbc.ArchivedStudent;
import com.university.sms.core.jdbc.SqliteArchiveDao;
import com.university.sms.core.model.FacultyRecord;
import com.university.sms.core.model.Person;
import com.university.sms.core.model.RollNumber;
import com.university.sms.core.model.StudentRecord;
import com.university.sms.core.model.UniversityInfo;
import com.university.sms.core.collections.SeatingPlan;
import com.university.sms.core.collections.SeatingPlanGenerator;
import com.university.sms.core.concurrent.AlertScheduler;
import com.university.sms.core.concurrent.NotificationCenter;
import com.university.sms.core.risk.AcademicRiskAssessor;
import com.university.sms.core.risk.RiskLevel;
import com.university.sms.core.risk.RiskReport;
import com.university.sms.core.risk.SgpaGoalPlanner;
import com.university.sms.core.verify.MarksheetFingerprint;
import com.university.sms.core.verify.MarksheetLine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Plain JUnit 5 tests for the Unit 1-5 core modules (no Spring context needed, so they run fast).
 */
class CoreModulesTest {

    private static List<StudentRecord> sampleStudents() {
        List<StudentRecord> list = new ArrayList<>();
        list.add(new StudentRecord(1L, "STU2026001", "Aarav", "Sharma", "a@x.in", "CSE", 5).withPerformance(90, 86, 9.0, 0));
        list.add(new StudentRecord(2L, "STU2026003", "Rohan", "Gupta", "r@x.in", "CSE", 5).withPerformance(60, 55, 6.4, 1));
        list.add(new StudentRecord(3L, "STU2026004", "Ananya", "Iyer", "an@x.in", "CSE", 5).withPerformance(100, 94, 9.8, 0));
        list.add(new StudentRecord(4L, "STU2026007", "Karthik", "Nair", "k@x.in", "ECE", 5).withPerformance(90, 78, 8.2, 0));
        list.add(new StudentRecord(5L, "STU2026008", "Neha", "Joshi", "n@x.in", "ECE", 5).withPerformance(60, 38, 4.0, 2));
        list.get(0).addSubjectScore(86);
        list.get(1).addSubjectScore(27, 30);
        list.get(2).addSubjectScore(28, 19, 19, 28);
        list.get(4).addSubjectScore(35);
        return list;
    }

    // ===================== Prit Patel: Unit 1, 2, 5 =====================

    @Test
    void inheritanceAndDynamicDispatch() {
        Person student = new StudentRecord(10L, "AP24110011578", "Prit", "Patel", "p@srmap.edu.in", "CSE", 5);
        Person faculty = new FacultyRecord(20L, "FAC-1", "Rajesh", "Verma", "r@x.in", "CSE", "Professor").teaches("CS501");
        assertEquals("Student", student.getRoleTitle());       // same call, different object -> different method
        assertEquals("Professor", faculty.getRoleTitle());
        assertTrue(student.describe().contains("AP24110011578"));
        assertEquals(2024, ((StudentRecord) student).getRollNumber().admissionYear());
        assertTrue(Person.getObjectsCreated() >= 2);
        assertThrows(IllegalArgumentException.class, () -> RollNumber.of("bad roll!"));
    }

    @Test
    void staticFinalUtilityAndClassesNeeded() {
        assertEquals(75.0, UniversityInfo.MIN_ATTENDANCE_PERCENT);
        assertEquals(0, UniversityInfo.classesNeededFor75(9, 10));
        assertEquals(6, UniversityInfo.classesNeededFor75(6, 10));   // (6+6)/(10+6) = 75%
    }

    @Test
    void gradeEnumAndOverloadedCalculator() {
        assertEquals(Grade.O, Grade.fromPercentage(93));
        assertEquals(Grade.A_PLUS, Grade.fromLabel("A+"));
        assertFalse(Grade.F.isPass());
        assertEquals(85.0, GradeCalculator.percentage(25, 18, 17, 25));
        assertEquals(50.0, GradeCalculator.percentage(15, 30));
        assertEquals(60.0, GradeCalculator.percentage(new int[]{20, 10, 30}, 100));
        assertEquals(9.5, GradeCalculator.sgpa(List.of(Grade.O, Grade.A_PLUS), List.of(4, 4)));
    }

    @Test
    void gradingPoliciesArePolymorphic() {
        List<Double> scores = List.of(95.0, 80.0, 65.0, 50.0, 37.0);
        GradingPolicy absolute = GradingPolicy.of("absolute");
        GradingPolicy relative = GradingPolicy.of("relative");
        GradingPolicy moderation = GradingPolicy.of("moderation");
        assertInstanceOf(RelativeGradingPolicy.class, relative);
        assertInstanceOf(ModerationGradingPolicy.class, moderation);

        assertEquals(Grade.F, absolute.assignAll(scores).get(4));
        assertEquals(Grade.C, moderation.assignAll(scores).get(4));   // 37 + grace -> 40 -> C
        List<Grade> curved = relative.assignAll(scores);
        assertEquals(Grade.A_PLUS, curved.get(0));                     // z = 1.44 -> A+ (absolute gives O)
        assertEquals(Grade.F, curved.get(4));                          // below 40% always fails
    }

    @Test
    void genericsWork() {
        InMemoryRepository<StudentRecord, Long> repo = new InMemoryRepository<>();
        repo.saveAll(sampleStudents());
        assertEquals(5, repo.count());
        assertEquals(2, repo.findWhere(StudentRecord::isAttendanceShort).size());
        assertEquals(9.8, GenericUtils.max(sampleStudents().stream().map(StudentRecord::getSgpa).toList()));
        assertEquals(2.0, GenericUtils.average(List.of(1, 2, 3)));
        List<Number> numbers = new ArrayList<>();
        GenericUtils.fillRange(numbers, 0, 3);
        assertEquals(3, numbers.size());
        Pair<String, Double> p = Pair.of("CSE", 8.4);
        assertEquals(8.4, p.swap().first());
        Result<Integer> r = Result.success("12", "ok").map(Integer::parseInt);
        assertEquals(12, r.getValue());
    }

    // ===================== Anjay: Unit 2 exceptions + Unit 3 I/O =====================

    @Test
    void marksValidatorThrowsCheckedException() {
        InvalidMarksException ex = assertThrows(InvalidMarksException.class,
                () -> MarksValidator.validate(35.0, 10.0, 10.0, 10.0));
        assertEquals("Internal exam", ex.getComponent());
        assertEquals("SMS-MARKS-001", ex.getErrorCode());
    }

    @Test
    void csvImportKeepsGoodRowsAndReportsBadOnes() {
        String csv = CsvStudentImporter.HEADER + "\n"
                + "STU2026101,Rahul,Mehta,rahul@apex.edu.in,99,CSE,5,A\n"
                + "STU2026102,Kavya,Rao,not-an-email,99,ECE,5,A\n"
                + "STU2026103,Om,Das,om@apex.edu.in,99,ECE,five,A\n"
                + "STU2026101,Dup,Row,dup@apex.edu.in,99,CSE,5,A\n";
        Result<List<StudentImportRow>> result = new CsvStudentImporter()
                .read(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));
        assertEquals(1, result.getValue().size());
        assertEquals(3, result.getErrors().size());
        assertTrue(result.getErrors().get(1).contains("Line 4"));
    }

    @Test
    void csvExportAndSerializationRoundTrip() throws Exception {
        StringWriter out = new StringWriter();
        int rows = new CsvStudentExporter().write(sampleStudents(), out);
        assertEquals(5, rows);
        assertTrue(out.toString().startsWith(CsvStudentExporter.HEADER));

        SerializationBackupService backup = new SerializationBackupService();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        backup.backup(sampleStudents(), bytes);
        SerializationBackupService.BackupContents back = backup.restore(new ByteArrayInputStream(bytes.toByteArray()));
        assertEquals(5, back.students().size());
        assertEquals("STU2026004", back.students().get(2).getRoll());

        assertThrows(SmsException.class, () -> backup.restore(new ByteArrayInputStream("garbage".getBytes())));
    }

    // ===================== Vivekanand: Unit 3 multithreading =====================

    @Test
    void parallelReportsUseManyThreadsAndAreFaster() throws Exception {
        List<StudentRecord> students = new ArrayList<>(sampleStudents());
        students.addAll(sampleStudents());
        ConcurrentReportEngine engine = new ConcurrentReportEngine();
        ConcurrentReportEngine.ReportRun seq = engine.generateSequential(students);
        ConcurrentReportEngine.ReportRun par = engine.generateParallel(students, 5);
        assertEquals(10, par.cards().size());
        assertTrue(par.threadsUsed().size() > 1);
        assertTrue(par.elapsedMillis() < seq.elapsedMillis());
    }

    @Test
    void producerConsumerDispatchesEveryShortage() throws Exception {
        List<String> log = new AttendanceAlertMonitor().run(sampleStudents());
        long dispatched = log.stream().filter(l -> l.contains("DISPATCHED")).count();
        assertEquals(2, dispatched);
        assertTrue(log.get(log.size() - 1).contains("TERMINATED"));
    }

    @Test
    void alertQueueBlocksAndHandsOver() throws Exception {
        AlertQueue<Integer> q = new AlertQueue<>(1);
        Thread producer = new Thread(() -> {
            try {
                for (int i = 0; i < 5; i++) q.put(i);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        });
        producer.start();
        int sum = 0;
        for (int i = 0; i < 5; i++) sum += q.take();
        producer.join();
        assertEquals(10, sum);
    }

    @Test
    void synchronizedCounterHasNoLostUpdates() throws Exception {
        assertEquals(400_000, PortalActivityMonitor.raceConditionDemo(8, 50_000, true));
    }

    @Test
    void lifecycleTraceVisitsAllStates() throws Exception {
        List<String> states = new ThreadLifecycleTracer().trace();
        String all = String.join(" ", states);
        for (String s : List.of("NEW", "RUNNABLE", "TIMED_WAITING", "WAITING", "BLOCKED", "TERMINATED")) {
            assertTrue(all.contains(s), "missing state " + s + " in " + states);
        }
    }

    // ===================== Arindam: Unit 4 Collections + JDBC =====================

    @Test
    void analyticsEngineUsesCollectionsCorrectly() {
        AnalyticsReport report = new AcademicAnalyticsEngine().analyse(sampleStudents(), 3, "STU2026004");
        assertEquals("STU2026004", report.toppers().get(0).getRoll());
        assertEquals(3, report.toppers().size());
        assertEquals(List.of("CSE", "ECE"), new ArrayList<>(report.byDepartment().keySet()));
        assertEquals(2, report.shortageRollNumbers().size());
        assertEquals(List.of("STU2026003", "STU2026008"), report.detainedAfterIteratorFilter());
        assertEquals(2, report.distinctDepartments());
        assertTrue(report.rankOfSearched() >= 0);
        assertEquals(1, report.gradeDistribution().get(Grade.F));
    }

    @Test
    void sqliteArchiveRoundTrip(@TempDir Path dir) throws Exception {
        SqliteArchiveDao dao = new SqliteArchiveDao(dir.resolve("test_archive.db"));
        List<FacultyRecord> faculty = List.of(
                new FacultyRecord(1L, "FAC-1", "Rajesh", "Verma", "r@x.in", "CSE", "Professor").teaches("CS501", "CS502"));
        assertEquals(5, dao.exportSnapshot(sampleStudents(), faculty, "junit"));
        assertEquals(5, dao.exportSnapshot(sampleStudents(), faculty, "junit"));   // re-run replaces, no duplicates

        List<ArchivedStudent> all = dao.findAllStudents();
        assertEquals(5, all.size());
        assertEquals("STU2026004", dao.findByDepartment("CSE").get(0).rollNumber());
        assertEquals(1, dao.searchByName("neha").size());
        Map<String, Double> avg = dao.departmentAverageSgpa();
        assertEquals(6.1, avg.get("ECE"));
        assertEquals("SQLite", dao.summary().databaseProduct());
        assertEquals(2, dao.summary().rowCounts().get("snapshot_runs"));
    }

    // ===================== Novel features =====================

    @Test
    void riskRadarRanksStrugglingStudentsFirst() {
        List<RiskReport> ranked = new AcademicRiskAssessor().rank(sampleStudents());
        assertEquals("STU2026008", ranked.get(0).rollNumber());          // 60% attendance, 2 backlogs, 38% avg
        assertTrue(ranked.get(0).level().compareTo(RiskLevel.HIGH) >= 0);
        assertEquals(RiskLevel.LOW, ranked.get(ranked.size() - 1).level());
        assertEquals(RiskLevel.CRITICAL, RiskLevel.of(80));
    }

    @Test
    void sgpaPlannerProjectsAndFindsRequiredPercentage() {
        SgpaGoalPlanner.Projection p = SgpaGoalPlanner.project(List.of(92.0, 81.0), List.of(4, 4));
        assertEquals(9.5, p.sgpa());
        assertEquals(80.0, SgpaGoalPlanner.minimumUniformPercentage(List.of(4, 4, 2), 9.0));
        assertEquals(-1.0, SgpaGoalPlanner.minimumUniformPercentage(List.of(4), 10.5));
    }

    @Test
    void marksheetCodeDetectsTampering() {
        MarksheetFingerprint f = new MarksheetFingerprint("unit-test-secret");
        List<MarksheetLine> lines = List.of(new MarksheetLine("CS501", 93.0, "O"), new MarksheetLine("CS502", 89.0, "A+"));
        String code = f.codeFor("STU2026001", lines, 9.56);
        assertTrue(code.matches("[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}"));
        assertEquals(code, f.codeFor("stu2026001", List.of(lines.get(1), lines.get(0)), 9.56));   // order-independent
        String tampered = f.codeFor("STU2026001", List.of(new MarksheetLine("CS501", 98.0, "O"), lines.get(1)), 9.56);
        assertFalse(f.matches(code, tampered));
        assertTrue(f.matches(code, code.toLowerCase()));
        assertFalse(new MarksheetFingerprint("another-secret").codeFor("STU2026001", lines, 9.56).equals(code));
    }

    @Test
    void alertSchedulerNotifiesOnlyOnStateChange() {
        NotificationCenter center = new NotificationCenter();
        AlertScheduler scheduler = new AlertScheduler(CoreModulesTest::sampleStudents, center, 60);
        assertEquals(2, scheduler.scanNow());          // two students below 75%
        assertEquals(0, scheduler.scanNow());          // no repeated alerts
        assertEquals(1, center.inboxOf("STU2026003").size());
        assertEquals(1, center.unreadCount("STU2026003"));
        center.markAllRead("STU2026003");
        assertEquals(0, center.unreadCount("STU2026003"));
    }

    @Test
    void seatingPlanNeverSeatsSameDepartmentTogether() {
        List<StudentRecord> students = new ArrayList<>(sampleStudents());
        students.add(new StudentRecord(9L, "STU2026009", "Aditya", "K", "ad@x.in", "MECH", 5));
        SeatingPlan plan = new SeatingPlanGenerator().generate("Test exam", students,
                List.of(new SeatingPlanGenerator.ExamRoom("HALL-1", 3, 4)));
        assertEquals(6, plan.seated());
        assertEquals(0, plan.sameDepartmentNeighbours());
        assertTrue(plan.unseated().isEmpty());
        assertTrue(plan.seatByRoll().get("STU2026004").label().startsWith("HALL-1 "));
    }
}
