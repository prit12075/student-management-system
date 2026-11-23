package com.university.sms.controller;

import com.university.sms.core.exception.AttendanceShortageException;
import com.university.sms.core.model.UniversityInfo;
import com.university.sms.dto.AttendanceSummaryDto;
import com.university.sms.dto.StudentPerformanceDto;
import com.university.sms.entity.*;
import com.university.sms.service.PdfReportService;
import com.university.sms.service.SmartCampusService;
import com.university.sms.core.risk.SgpaGoalPlanner;
import com.university.sms.service.UniversityErpService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/student")
public class StudentController {

    private final UniversityErpService erpService;
    private final PdfReportService pdfReportService;
    private final SmartCampusService smartCampusService;

    public StudentController(UniversityErpService erpService, PdfReportService pdfReportService,
                             SmartCampusService smartCampusService) {
        this.erpService = erpService;
        this.pdfReportService = pdfReportService;
        this.smartCampusService = smartCampusService;
    }

    private Student getCurrentStudent(Authentication authentication) {
        return erpService.getStudentByUsername(authentication.getName());
    }

    @ModelAttribute
    public void addCommonAttributes(Authentication authentication, Model model) {
        if (authentication != null) {
            Student student = getCurrentStudent(authentication);
            StudentPerformanceDto perf = erpService.getStudentPerformance(student);
            model.addAttribute("currentStudent", student);
            model.addAttribute("currentUser", student.getUser());
            model.addAttribute("performance", perf);
            model.addAttribute("unreadCount", smartCampusService.unread(student.getRollNumber()));
        }
    }

    // ==================== 1. STUDENT DASHBOARD ====================

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        Student student = getCurrentStudent(authentication);
        StudentPerformanceDto performance = erpService.getStudentPerformance(student);
        List<AttendanceSummaryDto> attendanceSummaries = erpService.getSubjectWiseAttendanceForStudent(student.getId());
        List<Mark> marks = erpService.getMarksForStudent(student.getId());
        List<Assignment> assignments = erpService.getAssignmentsForStudent(student);
        List<Examination> exams = erpService.getExaminationsForStudent(student);
        List<Notice> notices = erpService.getNoticesForAudience("STUDENTS");

        List<String> subjectLabels = new ArrayList<>();
        List<Double> subjectAttendanceList = new ArrayList<>();
        for (AttendanceSummaryDto dto : attendanceSummaries) {
            subjectLabels.add(dto.getSubject().getCode());
            subjectAttendanceList.add(dto.getAttendancePercentage());
        }

        List<String> markSubjectLabels = new ArrayList<>();
        List<Double> markTotals = new ArrayList<>();
        for (Mark m : marks) {
            markSubjectLabels.add(m.getSubject().getCode());
            markTotals.add(m.getTotalMarks());
        }

        List<AttendanceSummaryDto> lowAttendanceSubjects = attendanceSummaries.stream()
                .filter(AttendanceSummaryDto::isLowAttendance)
                .toList();

        model.addAttribute("activePage", "dashboard");
        model.addAttribute("attendanceSummaries", attendanceSummaries);
        model.addAttribute("lowAttendanceSubjects", lowAttendanceSubjects);
        model.addAttribute("marks", marks);
        model.addAttribute("assignments", assignments);
        model.addAttribute("exams", exams);
        model.addAttribute("notices", notices.stream().limit(4).toList());
        model.addAttribute("subjectLabels", subjectLabels);
        model.addAttribute("subjectAttendanceList", subjectAttendanceList);
        model.addAttribute("markSubjectLabels", markSubjectLabels);
        model.addAttribute("markTotals", markTotals);

        return "student/dashboard";
    }

    // ==================== 2. SUBJECT-WISE ATTENDANCE ====================

    @GetMapping("/attendance")
    public String attendance(@RequestParam(required = false) Long subjectId,
                             Authentication authentication,
                             Model model) {
        Student student = getCurrentStudent(authentication);
        List<AttendanceSummaryDto> summaries = erpService.getSubjectWiseAttendanceForStudent(student.getId());
        List<Attendance> history = erpService.getAttendanceHistoryForStudent(student.getId(), subjectId);
        List<Subject> enrolledSubjects = erpService.getSubjectsForStudent(student);

        model.addAttribute("activePage", "attendance");
        model.addAttribute("summaries", summaries);
        model.addAttribute("history", history);
        model.addAttribute("enrolledSubjects", enrolledSubjects);
        model.addAttribute("selectedSubjectId", subjectId);
        return "student/attendance";
    }

    // ==================== 3. EXAMINATION MARKS & RESULTS ====================

    @GetMapping("/marks")
    public String marks(Authentication authentication, Model model) {
        Student student = getCurrentStudent(authentication);
        List<Mark> marks = erpService.getMarksForStudent(student.getId());

        model.addAttribute("activePage", "marks");
        model.addAttribute("marks", marks);
        model.addAttribute("verificationCode", smartCampusService.verificationCode(getCurrentStudent(authentication)));
        return "student/marks";
    }

    // ==================== 4. TIMETABLE, EXAMS & ASSIGNMENTS ====================

    @GetMapping("/academics")
    public String academics(Authentication authentication, Model model) {
        Student student = getCurrentStudent(authentication);
        List<Timetable> timetable = erpService.getTimetableForDepartmentAndSemester(
                student.getDepartment().getId(), student.getSemester());
        List<Examination> exams = erpService.getExaminationsForStudent(student);
        List<Assignment> assignments = erpService.getAssignmentsForStudent(student);

        model.addAttribute("activePage", "academics");
        model.addAttribute("timetable", timetable);
        model.addAttribute("exams", exams);
        model.addAttribute("assignments", assignments);
        return "student/academics";
    }

    // ==================== 5. PROFILE & NOTICE BOARD ====================

    @GetMapping("/profile")
    public String profile(Authentication authentication, Model model) {
        model.addAttribute("activePage", "profile");
        model.addAttribute("notices", erpService.getNoticesForAudience("STUDENTS"));
        return "student/profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@RequestParam(required = false) String phone,
                                @RequestParam(required = false) String address,
                                @RequestParam(required = false) String newPassword,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        Student student = getCurrentStudent(authentication);
        erpService.updateStudentContactAndPassword(student, phone, address, newPassword);
        redirectAttributes.addFlashAttribute("successMessage", "Profile and security settings updated successfully!");
        return "redirect:/student/profile";
    }

    // ==================== 6. DOWNLOADABLE PDF MARKSHEET & REPORTS ====================

    @GetMapping("/reports/marksheet/pdf")
    public ResponseEntity<byte[]> downloadMarksheetPdf(Authentication authentication) {
        Student student = getCurrentStudent(authentication);
        List<Mark> marks = erpService.getMarksForStudent(student.getId());
        StudentPerformanceDto performance = erpService.getStudentPerformance(student);
        byte[] pdfBytes = pdfReportService.generateStudentMarksheetPdf(student, marks, performance);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Official_Marksheet_" + student.getRollNumber() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @GetMapping("/reports/attendance/pdf")
    public ResponseEntity<byte[]> downloadAttendancePdf(Authentication authentication) {
        Student student = getCurrentStudent(authentication);
        List<AttendanceSummaryDto> summaries = erpService.getSubjectWiseAttendanceForStudent(student.getId());
        StudentPerformanceDto performance = erpService.getStudentPerformance(student);
        byte[] pdfBytes = pdfReportService.generateStudentAttendancePdf(student, summaries, performance);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Attendance_Report_" + student.getRollNumber() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    // ==================== EXAM HALL TICKET (Unit 2 - Anjay) ====================

    /**
     * Issues the end-semester hall ticket. If the student is below 75% overall or in any subject,
     * a checked AttendanceShortageException is thrown and GlobalExceptionHandler renders the
     * "not eligible" page - no try/catch needed here (exception propagation).
     */
    @GetMapping("/hall-ticket")
    public String hallTicket(Authentication authentication, Model model) throws AttendanceShortageException {
        Student student = getCurrentStudent(authentication);
        StudentPerformanceDto perf = erpService.getStudentPerformance(student);
        List<AttendanceSummaryDto> subjects = erpService.getSubjectWiseAttendanceForStudent(student.getId());

        if (perf.isLowAttendanceAlert()) {
            int needed = UniversityInfo.classesNeededFor75(perf.getPresentClasses(), perf.getTotalClasses());
            for (AttendanceSummaryDto dto : subjects) {
                needed = Math.max(needed, dto.getClassesNeededFor75());
            }
            throw new AttendanceShortageException(student.getRollNumber(), perf.getAttendancePercentage(), needed);
        }

        model.addAttribute("activePage", "hallticket");
        model.addAttribute("subjects", subjects);
        model.addAttribute("exams", erpService.getExaminationsForStudent(student));
        model.addAttribute("issuedOn", java.time.LocalDate.now());
        model.addAttribute("seat", smartCampusService.seatFor(student.getRollNumber()).orElse(null));
        model.addAttribute("seatExam", smartCampusService.seatingExamName());
        model.addAttribute("verificationCode", smartCampusService.verificationCode(student));
        return "student/hall-ticket";
    }

    // ==================== SMART CAMPUS: planner + risk (Prit) and inbox (Vivekanand) ====================

    /** What-if SGPA planner + personal risk card. Expected percentages arrive as exp_0, exp_1, ... */
    @GetMapping("/planner")
    public String planner(Authentication authentication,
                          @RequestParam(required = false) Double target,
                          jakarta.servlet.http.HttpServletRequest request, Model model) {
        Student student = getCurrentStudent(authentication);
        List<SmartCampusService.PlannerRow> rows = smartCampusService.plannerRows(student);
        List<Double> expected = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            String v = request.getParameter("exp_" + i);
            double value = rows.get(i).current();
            try {
                if (v != null && !v.isBlank()) value = Math.max(0, Math.min(100, Double.parseDouble(v)));
            } catch (NumberFormatException ignored) {
                // keep current percentage when the input is not a number
            }
            expected.add(value);
        }
        SgpaGoalPlanner.Projection projection = smartCampusService.project(rows, expected);
        double goal = target == null ? 9.0 : Math.max(0, Math.min(10, target));
        model.addAttribute("activePage", "planner");
        model.addAttribute("rows", rows);
        model.addAttribute("expected", expected);
        model.addAttribute("projection", projection);
        model.addAttribute("target", goal);
        model.addAttribute("requiredPct", smartCampusService.minimumUniformPercentage(rows, goal));
        model.addAttribute("risk", smartCampusService.riskFor(student));
        return "student/planner";
    }

    @GetMapping("/notifications")
    public String notifications(Authentication authentication, Model model) {
        Student student = getCurrentStudent(authentication);
        model.addAttribute("activePage", "notifications");
        model.addAttribute("items", smartCampusService.inbox(student.getRollNumber()));
        model.addAttribute("smart", smartCampusService);
        return "student/notifications";
    }

    @PostMapping("/notifications/read")
    public String markRead(Authentication authentication) {
        smartCampusService.markAllRead(getCurrentStudent(authentication).getRollNumber());
        return "redirect:/student/notifications";
    }
}
