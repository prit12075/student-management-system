package com.university.sms.controller;

import com.university.sms.dto.AttendanceSummaryDto;
import com.university.sms.dto.StudentPerformanceDto;
import com.university.sms.entity.*;
import com.university.sms.service.PdfReportService;
import com.university.sms.service.SmartCampusService;
import com.university.sms.service.UniversityErpService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.*;

@Controller
@RequestMapping("/faculty")
public class FacultyController {

    private final UniversityErpService erpService;
    private final PdfReportService pdfReportService;
    private final SmartCampusService smartCampusService;

    public FacultyController(UniversityErpService erpService, PdfReportService pdfReportService,
                             SmartCampusService smartCampusService) {
        this.erpService = erpService;
        this.pdfReportService = pdfReportService;
        this.smartCampusService = smartCampusService;
    }

    private Faculty getCurrentFaculty(Authentication authentication) {
        return erpService.getFacultyByUsername(authentication.getName());
    }

    @ModelAttribute
    public void addCommonAttributes(Authentication authentication, Model model) {
        if (authentication != null) {
            Faculty faculty = getCurrentFaculty(authentication);
            model.addAttribute("currentFaculty", faculty);
            model.addAttribute("currentUser", faculty.getUser());
        }
    }

    // ==================== 1. FACULTY DASHBOARD ====================

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        Faculty faculty = getCurrentFaculty(authentication);
        List<Subject> assignedSubjects = erpService.getSubjectsByFaculty(faculty.getId());

        Set<Long> uniqueStudentIds = new HashSet<>();
        List<AttendanceSummaryDto> lowAttendanceAlerts = new ArrayList<>();
        List<String> subjectLabels = new ArrayList<>();
        List<Double> subjectAvgAttendance = new ArrayList<>();
        List<Double> subjectAvgMarks = new ArrayList<>();

        for (Subject sub : assignedSubjects) {
            subjectLabels.add(sub.getCode());
            List<Student> students = erpService.getStudentsForSubject(sub);
            students.forEach(s -> uniqueStudentIds.add(s.getId()));

            List<AttendanceSummaryDto> attSummaries = erpService.getStudentAttendanceSummariesForSubject(sub.getId());
            for (AttendanceSummaryDto dto : attSummaries) {
                if (dto.isLowAttendance()) {
                    lowAttendanceAlerts.add(dto);
                }
            }
            double avgAtt = attSummaries.isEmpty() ? 100.0 :
                    Math.round(attSummaries.stream().mapToDouble(AttendanceSummaryDto::getAttendancePercentage).average().orElse(100.0) * 10.0) / 10.0;
            subjectAvgAttendance.add(avgAtt);

            List<Mark> subMarks = erpService.getMarksForSubject(sub.getId());
            double avgMark = subMarks.isEmpty() ? 0.0 :
                    Math.round(subMarks.stream().mapToDouble(Mark::getPercentage).average().orElse(0.0) * 10.0) / 10.0;
            subjectAvgMarks.add(avgMark);
        }

        model.addAttribute("activePage", "dashboard");
        model.addAttribute("assignedSubjects", assignedSubjects);
        model.addAttribute("totalAssignedSubjects", assignedSubjects.size());
        model.addAttribute("totalStudentsTaught", uniqueStudentIds.size());
        model.addAttribute("lowAttendanceAlerts", lowAttendanceAlerts);
        model.addAttribute("lowAttendanceCount", lowAttendanceAlerts.size());
        model.addAttribute("assignmentsCount", erpService.getAssignmentsForFaculty(faculty.getId()).size());
        model.addAttribute("timetableSlots", erpService.getTimetableForFaculty(faculty.getId()));
        model.addAttribute("notices", erpService.getNoticesForAudience("FACULTY").stream().limit(4).toList());

        model.addAttribute("subjectLabels", subjectLabels);
        model.addAttribute("subjectAvgAttendance", subjectAvgAttendance);
        model.addAttribute("subjectAvgMarks", subjectAvgMarks);

        return "faculty/dashboard";
    }

    // ==================== 2. MARK & UPDATE ATTENDANCE ====================

    @GetMapping("/attendance")
    public String attendance(@RequestParam(required = false) Long subjectId,
                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                             Authentication authentication,
                             Model model) {
        Faculty faculty = getCurrentFaculty(authentication);
        List<Subject> assignedSubjects = erpService.getSubjectsByFaculty(faculty.getId());
        LocalDate selectedDate = date != null ? date : LocalDate.now();

        Subject selectedSubject = null;
        if (!assignedSubjects.isEmpty()) {
            if (subjectId != null) {
                selectedSubject = assignedSubjects.stream()
                        .filter(s -> Objects.equals(s.getId(), subjectId))
                        .findFirst()
                        .orElse(assignedSubjects.get(0));
            } else {
                selectedSubject = assignedSubjects.get(0);
            }
        }

        List<Student> enrolledStudents = Collections.emptyList();
        Map<Long, Attendance> existingAttendanceMap = Collections.emptyMap();
        List<AttendanceSummaryDto> attendanceSummaries = Collections.emptyList();

        if (selectedSubject != null) {
            enrolledStudents = erpService.getStudentsForSubject(selectedSubject);
            existingAttendanceMap = erpService.getAttendanceMapForSubjectAndDate(selectedSubject.getId(), selectedDate);
            attendanceSummaries = erpService.getStudentAttendanceSummariesForSubject(selectedSubject.getId());
        }

        model.addAttribute("activePage", "attendance");
        model.addAttribute("assignedSubjects", assignedSubjects);
        model.addAttribute("selectedSubject", selectedSubject);
        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("enrolledStudents", enrolledStudents);
        model.addAttribute("existingAttendanceMap", existingAttendanceMap);
        model.addAttribute("attendanceSummaries", attendanceSummaries);
        return "faculty/attendance";
    }

    @PostMapping("/attendance/save")
    public String saveAttendance(@RequestParam Long subjectId,
                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate attendanceDate,
                                 HttpServletRequest request,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        try {
            Faculty faculty = getCurrentFaculty(authentication);
            Subject subject = erpService.getSubjectById(subjectId);
            erpService.verifyFacultyOwnsSubject(faculty, subject);

            List<Student> students = erpService.getStudentsForSubject(subject);
            Map<Long, AttendanceStatus> statusMap = new HashMap<>();
            Map<Long, String> remarksMap = new HashMap<>();

            for (Student s : students) {
                String statusParam = request.getParameter("status_" + s.getId());
                String remarkParam = request.getParameter("remarks_" + s.getId());
                AttendanceStatus status = AttendanceStatus.PRESENT;
                if (statusParam != null) {
                    try {
                        status = AttendanceStatus.valueOf(statusParam);
                    } catch (IllegalArgumentException ignored) {
                    }
                }
                statusMap.put(s.getId(), status);
                remarksMap.put(s.getId(), remarkParam != null ? remarkParam.trim() : "");
            }

            erpService.markBulkAttendance(subjectId, attendanceDate, statusMap, remarksMap, faculty);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Attendance for " + subject.getCode() + " on " + attendanceDate + " saved successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/faculty/attendance?subjectId=" + subjectId + "&date=" + attendanceDate;
    }

    @GetMapping("/reports/attendance/pdf")
    public ResponseEntity<byte[]> downloadSubjectAttendancePdf(@RequestParam Long subjectId,
                                                               Authentication authentication) {
        Faculty faculty = getCurrentFaculty(authentication);
        Subject subject = erpService.getSubjectById(subjectId);
        erpService.verifyFacultyOwnsSubject(faculty, subject);

        List<AttendanceSummaryDto> summaries = erpService.getStudentAttendanceSummariesForSubject(subjectId);
        byte[] pdf = pdfReportService.generateSubjectAttendancePdf(subject, summaries);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Attendance_" + subject.getCode() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    // ==================== 3. ENTER & UPDATE MARKS ====================

    @GetMapping("/marks")
    public String marks(@RequestParam(required = false) Long subjectId,
                        Authentication authentication,
                        Model model) {
        Faculty faculty = getCurrentFaculty(authentication);
        List<Subject> assignedSubjects = erpService.getSubjectsByFaculty(faculty.getId());

        Subject selectedSubject = null;
        if (!assignedSubjects.isEmpty()) {
            if (subjectId != null) {
                selectedSubject = assignedSubjects.stream()
                        .filter(s -> Objects.equals(s.getId(), subjectId))
                        .findFirst()
                        .orElse(assignedSubjects.get(0));
            } else {
                selectedSubject = assignedSubjects.get(0);
            }
        }

        List<Student> enrolledStudents = Collections.emptyList();
        Map<Long, Mark> marksMap = Collections.emptyMap();

        if (selectedSubject != null) {
            enrolledStudents = erpService.getStudentsForSubject(selectedSubject);
            marksMap = erpService.getMarksMapForSubject(selectedSubject.getId());
        }

        model.addAttribute("activePage", "marks");
        model.addAttribute("assignedSubjects", assignedSubjects);
        model.addAttribute("selectedSubject", selectedSubject);
        model.addAttribute("enrolledStudents", enrolledStudents);
        model.addAttribute("marksMap", marksMap);
        return "faculty/marks";
    }

    @PostMapping("/marks/save-bulk")
    public String saveBulkMarks(@RequestParam Long subjectId,
                                HttpServletRequest request,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        try {
            Faculty faculty = getCurrentFaculty(authentication);
            Subject subject = erpService.getSubjectById(subjectId);
            erpService.verifyFacultyOwnsSubject(faculty, subject);

            List<Student> students = erpService.getStudentsForSubject(subject);
            for (Student s : students) {
                Double internal = parseDoubleOrDefault(request.getParameter("internal_" + s.getId()), 0.0);
                Double assignment = parseDoubleOrDefault(request.getParameter("assignment_" + s.getId()), 0.0);
                Double practical = parseDoubleOrDefault(request.getParameter("practical_" + s.getId()), 0.0);
                Double external = parseDoubleOrDefault(request.getParameter("external_" + s.getId()), 0.0);
                String remarks = request.getParameter("remarks_" + s.getId());

                erpService.saveOrUpdateMark(s.getId(), subjectId, internal, assignment, practical, external, remarks, faculty);
            }
            // novel feature (Vivekanand): every student gets an inbox notification
            smartCampusService.notifyMarksPublished(students, subject.getCode());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Examination & assignment marks for " + subject.getCode() + " updated and grades calculated!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/faculty/marks?subjectId=" + subjectId;
    }

    private Double parseDoubleOrDefault(String val, double def) {
        if (val == null || val.isBlank()) return def;
        try {
            return Double.parseDouble(val.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    // ==================== 4. ASSIGNMENTS & TEACHING TIMETABLE ====================

    @GetMapping("/assignments")
    public String assignments(Authentication authentication, Model model) {
        Faculty faculty = getCurrentFaculty(authentication);
        model.addAttribute("activePage", "assignments");
        model.addAttribute("assignedSubjects", erpService.getSubjectsByFaculty(faculty.getId()));
        model.addAttribute("assignments", erpService.getAssignmentsForFaculty(faculty.getId()));
        model.addAttribute("timetableSlots", erpService.getTimetableForFaculty(faculty.getId()));
        return "faculty/assignments";
    }

    @PostMapping("/assignments/save")
    public String saveAssignment(@RequestParam String title,
                                 @RequestParam String description,
                                 @RequestParam Long subjectId,
                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDate,
                                 @RequestParam(defaultValue = "20") Integer maxMarks,
                                 @RequestParam(required = false) String referenceLink,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        try {
            Faculty faculty = getCurrentFaculty(authentication);
            erpService.saveAssignment(title, description, subjectId, faculty, dueDate, maxMarks, referenceLink);
            redirectAttributes.addFlashAttribute("successMessage", "Assignment published successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/faculty/assignments";
    }

    @PostMapping("/assignments/delete/{id}")
    public String deleteAssignment(@PathVariable Long id,
                                   Authentication authentication,
                                   RedirectAttributes redirectAttributes) {
        try {
            Faculty faculty = getCurrentFaculty(authentication);
            erpService.deleteAssignment(id, faculty);
            redirectAttributes.addFlashAttribute("successMessage", "Assignment deleted.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/faculty/assignments";
    }

    // ==================== 5. STUDENT PERFORMANCE ====================

    @GetMapping("/performance")
    public String performance(@RequestParam(required = false) Long subjectId,
                              Authentication authentication,
                              Model model) {
        Faculty faculty = getCurrentFaculty(authentication);
        List<Subject> assignedSubjects = erpService.getSubjectsByFaculty(faculty.getId());

        Subject selectedSubject = null;
        if (!assignedSubjects.isEmpty()) {
            if (subjectId != null) {
                selectedSubject = assignedSubjects.stream()
                        .filter(s -> Objects.equals(s.getId(), subjectId))
                        .findFirst()
                        .orElse(assignedSubjects.get(0));
            } else {
                selectedSubject = assignedSubjects.get(0);
            }
        }

        List<AttendanceSummaryDto> attSummaries = Collections.emptyList();
        Map<Long, Mark> marksMap = Collections.emptyMap();
        List<String> studentLabels = new ArrayList<>();
        List<Double> studentAttList = new ArrayList<>();
        List<Double> studentMarksList = new ArrayList<>();

        if (selectedSubject != null) {
            attSummaries = erpService.getStudentAttendanceSummariesForSubject(selectedSubject.getId());
            marksMap = erpService.getMarksMapForSubject(selectedSubject.getId());
            for (AttendanceSummaryDto dto : attSummaries) {
                studentLabels.add(dto.getStudent().getRollNumber());
                studentAttList.add(dto.getAttendancePercentage());
                Mark m = marksMap.get(dto.getStudent().getId());
                studentMarksList.add(m != null ? m.getPercentage() : 0.0);
            }
        }

        model.addAttribute("activePage", "performance");
        model.addAttribute("assignedSubjects", assignedSubjects);
        model.addAttribute("selectedSubject", selectedSubject);
        model.addAttribute("attSummaries", attSummaries);
        model.addAttribute("marksMap", marksMap);
        model.addAttribute("studentLabels", studentLabels);
        model.addAttribute("studentAttList", studentAttList);
        model.addAttribute("studentMarksList", studentMarksList);
        return "faculty/performance";
    }
}

