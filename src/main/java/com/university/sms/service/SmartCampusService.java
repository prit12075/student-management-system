package com.university.sms.service;

import com.university.sms.core.collections.Seat;
import com.university.sms.core.collections.SeatingPlan;
import com.university.sms.core.collections.SeatingPlanGenerator;
import com.university.sms.core.concurrent.AlertScheduler;
import com.university.sms.core.concurrent.Notification;
import com.university.sms.core.concurrent.NotificationCenter;
import com.university.sms.core.exception.RecordNotFoundException;
import com.university.sms.core.jdbc.SqliteArchiveDao;
import com.university.sms.core.model.StudentRecord;
import com.university.sms.core.risk.AcademicRiskAssessor;
import com.university.sms.core.risk.RiskLevel;
import com.university.sms.core.risk.RiskReport;
import com.university.sms.core.risk.SgpaGoalPlanner;
import com.university.sms.core.verify.MarksheetFingerprint;
import com.university.sms.core.verify.MarksheetLine;
import com.university.sms.core.verify.VerificationResult;
import com.university.sms.dto.StudentPerformanceDto;
import com.university.sms.entity.Mark;
import com.university.sms.entity.Student;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * "Smart Campus" novel features - one service facade over the four modules
 * (integration by Prit Patel; module owners noted per section).
 */
@Service
public class SmartCampusService {

    private final UniversityErpService erpService;
    private final JavaLabService labService;
    private final AcademicRiskAssessor riskAssessor;
    private final MarksheetFingerprint fingerprint;
    private final NotificationCenter notifications;
    private final AlertScheduler alertScheduler;
    private final SeatingPlanGenerator seatingGenerator;
    private final SqliteArchiveDao archiveDao;

    private volatile SeatingPlan lastPlan;          // latest generated plan, shared by all request threads

    public SmartCampusService(UniversityErpService erpService, JavaLabService labService,
                              AcademicRiskAssessor riskAssessor, MarksheetFingerprint fingerprint,
                              NotificationCenter notifications, AlertScheduler alertScheduler,
                              SeatingPlanGenerator seatingGenerator, SqliteArchiveDao archiveDao) {
        this.erpService = erpService;
        this.labService = labService;
        this.riskAssessor = riskAssessor;
        this.fingerprint = fingerprint;
        this.notifications = notifications;
        this.alertScheduler = alertScheduler;
        this.seatingGenerator = seatingGenerator;
        this.archiveDao = archiveDao;
    }

    // ===================== Prit Patel: Academic Risk Radar + SGPA planner =====================

    public List<RiskReport> riskRadar() {
        return riskAssessor.rank(labService.loadStudentRecords());
    }

    public Map<RiskLevel, Long> riskSummary(List<RiskReport> reports) {
        Map<RiskLevel, Long> counts = new EnumMap<>(RiskLevel.class);
        for (RiskLevel l : RiskLevel.values()) {
            counts.put(l, reports.stream().filter(r -> r.level() == l).count());
        }
        return counts;
    }

    public RiskReport riskFor(Student student) {
        return labService.loadStudentRecords().stream()
                .filter(r -> r.getRoll().equals(student.getRollNumber()))
                .findFirst()
                .map(riskAssessor::assess)
                .orElseThrow(() -> new RecordNotFoundException("Student", student.getRollNumber()));
    }

    public List<String> riskFactorNames() {
        return riskAssessor.factorNames();
    }

    /** One planner row: subject, credits, current percentage. */
    public record PlannerRow(String code, String name, int credits, double current) {
    }

    public List<PlannerRow> plannerRows(Student student) {
        List<PlannerRow> rows = new ArrayList<>();
        for (Mark m : erpService.getMarksForStudent(student.getId())) {
            int credits = m.getSubject().getCredits() == null ? 4 : m.getSubject().getCredits();
            rows.add(new PlannerRow(m.getSubject().getCode(), m.getSubject().getName(), credits, m.getPercentage()));
        }
        return rows;
    }

    public SgpaGoalPlanner.Projection project(List<PlannerRow> rows, List<Double> expected) {
        return SgpaGoalPlanner.project(expected, rows.stream().map(PlannerRow::credits).toList());
    }

    public double minimumUniformPercentage(List<PlannerRow> rows, double target) {
        return SgpaGoalPlanner.minimumUniformPercentage(rows.stream().map(PlannerRow::credits).toList(), target);
    }

    // ===================== Anjay: tamper-proof marksheet verification =====================

    /** Shared with PdfReportService so the PDF and the website compute the same code. */
    public static List<MarksheetLine> linesOf(List<Mark> marks) {
        return marks.stream().map(m -> new MarksheetLine(m.getSubject().getCode(), m.getTotalMarks(), m.getGrade())).toList();
    }

    public String verificationCode(Student student) {
        StudentPerformanceDto perf = erpService.getStudentPerformance(student);
        return fingerprint.codeFor(student.getRollNumber(), linesOf(erpService.getMarksForStudent(student.getId())), perf.getSgpa());
    }

    public VerificationResult verify(String roll, String code) {
        String cleanRoll = roll == null ? "" : roll.trim().toUpperCase();
        Optional<Student> student = erpService.getAllStudents().stream()
                .filter(s -> s.getRollNumber().equalsIgnoreCase(cleanRoll)).findFirst();
        if (student.isEmpty()) {
            return new VerificationResult(VerificationResult.Status.UNKNOWN_ROLL, cleanRoll, null, null, 0, 0,
                    "No student with roll number " + cleanRoll + " exists in the university records.");
        }
        Student s = student.get();
        StudentPerformanceDto perf = erpService.getStudentPerformance(s);
        String expected = verificationCode(s);
        if (fingerprint.matches(expected, code)) {
            labService.audit("public", "VERIFY_OK", cleanRoll);
            return new VerificationResult(VerificationResult.Status.VALID, s.getRollNumber(), s.getFullName(),
                    s.getDepartment().getName(), perf.getSgpa(), perf.getSubjectsEvaluated(),
                    "Genuine marksheet: the code matches the current official record.");
        }
        labService.audit("public", "VERIFY_FAIL", cleanRoll);
        return new VerificationResult(VerificationResult.Status.MISMATCH, s.getRollNumber(), null, null, 0, 0,
                "The code does not match the official record. The document may be edited/forged, or the marks were revised after it was issued.");
    }

    // ===================== Vivekanand: alert scheduler + inbox =====================

    public AlertScheduler getAlertScheduler() {
        return alertScheduler;
    }

    public int runAlertScanNow() {
        return alertScheduler.scanNow();
    }

    public List<Notification> inbox(String roll) {
        return notifications.inboxOf(roll);
    }

    public long unread(String roll) {
        return notifications.unreadCount(roll);
    }

    public boolean isRead(long id) {
        return notifications.isRead(id);
    }

    public void markAllRead(String roll) {
        notifications.markAllRead(roll);
    }

    public List<String> recentNotifications() {
        return notifications.recent(20);
    }

    public int notificationsSent() {
        return notifications.totalSent();
    }

    public void notifyMarksPublished(List<Student> students, String subjectCode) {
        for (Student s : students) {
            notifications.send(s.getRollNumber(), "MARKS", "Marks published: " + subjectCode,
                    "Your internal, assignment, practical and external marks for " + subjectCode
                            + " are now available under Exam Results & Grades.");
        }
    }

    // ===================== Arindam: exam seating plan =====================

    public SeatingPlan generateSeating(String examName, int rooms, int rows, int cols, String actor) throws SQLException {
        List<StudentRecord> students = labService.loadStudentRecords();
        List<SeatingPlanGenerator.ExamRoom> halls = new ArrayList<>();
        for (int i = 1; i <= rooms; i++) {
            halls.add(new SeatingPlanGenerator.ExamRoom("HALL-" + i, rows, cols));
        }
        SeatingPlan plan = seatingGenerator.generate(examName, students, halls);
        archiveDao.saveSeatingPlan(examName, plan.seatByRoll().values());
        for (Seat seat : plan.seatByRoll().values()) {
            notifications.send(seat.rollNumber(), "INFO", "Exam seat allotted",
                    examName + ": your seat is " + seat.label() + ". It is also printed on your hall ticket.");
        }
        labService.audit(actor, "SEATING_PLAN", plan.seated() + " seats in " + rooms + " hall(s)");
        lastPlan = plan;
        return plan;
    }

    public SeatingPlan getLastPlan() {
        return lastPlan;
    }

    public Optional<Seat> seatFor(String roll) {
        try {
            return archiveDao.findSeat(roll);
        } catch (SQLException e) {
            return Optional.empty();
        }
    }

    public String seatingExamName() {
        try {
            return archiveDao.seatingExamName();
        } catch (SQLException e) {
            return null;
        }
    }
}
