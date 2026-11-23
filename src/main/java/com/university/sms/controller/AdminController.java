package com.university.sms.controller;

import com.university.sms.dto.AttendanceSummaryDto;
import com.university.sms.dto.StudentPerformanceDto;
import com.university.sms.entity.*;
import com.university.sms.service.PdfReportService;
import com.university.sms.service.UniversityErpService;
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
import java.time.LocalTime;
import java.util.*;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UniversityErpService erpService;
    private final PdfReportService pdfReportService;

    public AdminController(UniversityErpService erpService, PdfReportService pdfReportService) {
        this.erpService = erpService;
        this.pdfReportService = pdfReportService;
    }

    @ModelAttribute
    public void addCommonAttributes(Authentication authentication, Model model) {
        if (authentication != null) {
            User adminUser = erpService.getUserByUsername(authentication.getName());
            model.addAttribute("currentUser", adminUser);
        }
    }

    // ==================== 1. ADMIN DASHBOARD ====================

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        List<Student> students = erpService.getAllStudents();
        List<Faculty> facultyList = erpService.getAllFaculty();
        List<Department> departments = erpService.getAllDepartments();
        List<Subject> subjects = erpService.getAllSubjects();
        List<StudentPerformanceDto> allPerf = erpService.getAllStudentPerformances(null);
        List<StudentPerformanceDto> lowAttendanceList = erpService.getLowAttendanceStudents(null);

        double avgUniversityAttendance = allPerf.isEmpty() ? 100.0 :
                Math.round(allPerf.stream().mapToDouble(StudentPerformanceDto::getAttendancePercentage).average().orElse(100.0) * 10.0) / 10.0;
        double avgUniversityMarks = allPerf.isEmpty() ? 0.0 :
                Math.round(allPerf.stream().mapToDouble(StudentPerformanceDto::getAveragePercentage).average().orElse(0.0) * 10.0) / 10.0;

        // Chart data: Department labels, student counts, average attendance, average marks
        List<String> deptNames = new ArrayList<>();
        List<Integer> deptStudentCounts = new ArrayList<>();
        List<Double> deptAvgMarks = new ArrayList<>();
        List<Double> deptAvgAttendance = new ArrayList<>();

        for (Department dept : departments) {
            deptNames.add(dept.getCode());
            List<StudentPerformanceDto> deptPerfs = allPerf.stream()
                    .filter(p -> Objects.equals(p.getStudent().getDepartment().getId(), dept.getId()))
                    .toList();
            deptStudentCounts.add(deptPerfs.size());
            deptAvgMarks.add(Math.round(deptPerfs.stream().mapToDouble(StudentPerformanceDto::getAveragePercentage).average().orElse(0.0) * 10.0) / 10.0);
            deptAvgAttendance.add(Math.round(deptPerfs.stream().mapToDouble(StudentPerformanceDto::getAttendancePercentage).average().orElse(100.0) * 10.0) / 10.0);
        }

        // Attendance brackets for Doughnut Chart
        long highAttCount = allPerf.stream().filter(p -> p.getAttendancePercentage() >= 85.0).count();
        long moderateAttCount = allPerf.stream().filter(p -> p.getAttendancePercentage() >= 75.0 && p.getAttendancePercentage() < 85.0).count();
        long criticalAttCount = allPerf.stream().filter(p -> p.getAttendancePercentage() < 75.0).count();

        model.addAttribute("activePage", "dashboard");
        model.addAttribute("totalStudents", students.size());
        model.addAttribute("totalFaculty", facultyList.size());
        model.addAttribute("totalDepartments", departments.size());
        model.addAttribute("totalSubjects", subjects.size());
        model.addAttribute("avgUniversityAttendance", avgUniversityAttendance);
        model.addAttribute("avgUniversityMarks", avgUniversityMarks);
        model.addAttribute("lowAttendanceCount", lowAttendanceList.size());
        model.addAttribute("lowAttendanceList", lowAttendanceList);
        model.addAttribute("studentPerformances", allPerf);
        model.addAttribute("recentNotices", erpService.getAllNotices().stream().limit(4).toList());
        model.addAttribute("upcomingExams", erpService.getAllExaminations().stream().limit(5).toList());

        model.addAttribute("deptNames", deptNames);
        model.addAttribute("deptStudentCounts", deptStudentCounts);
        model.addAttribute("deptAvgMarks", deptAvgMarks);
        model.addAttribute("deptAvgAttendance", deptAvgAttendance);
        model.addAttribute("highAttCount", highAttCount);
        model.addAttribute("moderateAttCount", moderateAttCount);
        model.addAttribute("criticalAttCount", criticalAttCount);

        return "admin/dashboard";
    }

    // ==================== 2. STUDENT MANAGEMENT ====================

    @GetMapping("/students")
    public String students(@RequestParam(required = false) String keyword,
                           @RequestParam(required = false) Long departmentId,
                           @RequestParam(required = false) Integer semester,
                           @RequestParam(required = false) Long editId,
                           Model model) {
        List<Student> students = erpService.searchStudents(keyword, departmentId, semester);
        Map<Long, StudentPerformanceDto> perfMap = new HashMap<>();
        for (Student s : students) {
            perfMap.put(s.getId(), erpService.getStudentPerformance(s));
        }

        Student editStudent = (editId != null && editId > 0) ? erpService.getStudentById(editId) : new Student();

        model.addAttribute("activePage", "students");
        model.addAttribute("students", students);
        model.addAttribute("perfMap", perfMap);
        model.addAttribute("editStudent", editStudent);
        model.addAttribute("departments", erpService.getAllDepartments());
        model.addAttribute("courses", erpService.getAllCourses());
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedDepartmentId", departmentId);
        model.addAttribute("selectedSemester", semester);
        return "admin/students";
    }

    @PostMapping("/students/save")
    public String saveStudent(@RequestParam(required = false) Long id,
                              @RequestParam String rollNumber,
                              @RequestParam String firstName,
                              @RequestParam String lastName,
                              @RequestParam String email,
                              @RequestParam(required = false) String phone,
                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateOfBirth,
                              @RequestParam(required = false) String gender,
                              @RequestParam(required = false) String address,
                              @RequestParam(required = false) String guardianName,
                              @RequestParam(required = false) String guardianPhone,
                              @RequestParam Integer semester,
                              @RequestParam(defaultValue = "A") String section,
                              @RequestParam(defaultValue = "2024") Integer enrollmentYear,
                              @RequestParam Long departmentId,
                              @RequestParam Long courseId,
                              @RequestParam(required = false) String password,
                              RedirectAttributes redirectAttributes) {
        try {
            Student saved = erpService.createOrUpdateStudent(
                    id, rollNumber, firstName, lastName, email, phone, dateOfBirth,
                    gender, address, guardianName, guardianPhone, semester, section,
                    enrollmentYear, departmentId, courseId, password
            );
            redirectAttributes.addFlashAttribute("successMessage",
                    "Student record for " + saved.getFullName() + " (" + saved.getRollNumber() + ") saved successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error saving student: " + e.getMessage());
        }
        return "redirect:/admin/students";
    }

    @PostMapping("/students/delete/{id}")
    public String deleteStudent(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Student s = erpService.getStudentById(id);
            erpService.deleteStudent(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Student " + s.getFullName() + " (" + s.getRollNumber() + ") deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Could not delete student: " + e.getMessage());
        }
        return "redirect:/admin/students";
    }

    @GetMapping("/students/view/{id}")
    public String viewStudentDetail(@PathVariable Long id, Model model) {
        Student student = erpService.getStudentById(id);
        StudentPerformanceDto performance = erpService.getStudentPerformance(student);
        List<AttendanceSummaryDto> attendanceSummaries = erpService.getSubjectWiseAttendanceForStudent(id);
        List<Mark> marks = erpService.getMarksForStudent(id);

        model.addAttribute("activePage", "students");
        model.addAttribute("student", student);
        model.addAttribute("performance", performance);
        model.addAttribute("attendanceSummaries", attendanceSummaries);
        model.addAttribute("marks", marks);
        return "admin/student-detail";
    }

    @GetMapping("/students/{id}/marksheet/pdf")
    public ResponseEntity<byte[]> downloadStudentMarksheet(@PathVariable Long id) {
        Student student = erpService.getStudentById(id);
        List<Mark> marks = erpService.getMarksForStudent(id);
        StudentPerformanceDto performance = erpService.getStudentPerformance(student);
        byte[] pdfBytes = pdfReportService.generateStudentMarksheetPdf(student, marks, performance);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Marksheet_" + student.getRollNumber() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    // ==================== 3. FACULTY MANAGEMENT ====================

    @GetMapping("/faculty")
    public String faculty(@RequestParam(required = false) String keyword,
                          @RequestParam(required = false) Long departmentId,
                          @RequestParam(required = false) Long editId,
                          Model model) {
        List<Faculty> facultyList = erpService.searchFaculty(keyword, departmentId);
        Map<Long, List<Subject>> assignedSubjectsMap = new HashMap<>();
        for (Faculty f : facultyList) {
            assignedSubjectsMap.put(f.getId(), erpService.getSubjectsByFaculty(f.getId()));
        }

        Faculty editFaculty = (editId != null && editId > 0) ? erpService.getFacultyById(editId) : new Faculty();

        model.addAttribute("activePage", "faculty");
        model.addAttribute("facultyList", facultyList);
        model.addAttribute("assignedSubjectsMap", assignedSubjectsMap);
        model.addAttribute("editFaculty", editFaculty);
        model.addAttribute("departments", erpService.getAllDepartments());
        model.addAttribute("allSubjects", erpService.getAllSubjects());
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedDepartmentId", departmentId);
        return "admin/faculty";
    }

    @PostMapping("/faculty/save")
    public String saveFaculty(@RequestParam(required = false) Long id,
                              @RequestParam String employeeId,
                              @RequestParam String firstName,
                              @RequestParam String lastName,
                              @RequestParam String email,
                              @RequestParam(required = false) String phone,
                              @RequestParam String designation,
                              @RequestParam(required = false) String qualification,
                              @RequestParam(required = false) String specialization,
                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate joiningDate,
                              @RequestParam Long departmentId,
                              @RequestParam(required = false) String password,
                              RedirectAttributes redirectAttributes) {
        try {
            Faculty saved = erpService.createOrUpdateFaculty(
                    id, employeeId, firstName, lastName, email, phone,
                    designation, qualification, specialization, joiningDate,
                    departmentId, password
            );
            redirectAttributes.addFlashAttribute("successMessage",
                    "Faculty member " + saved.getFullName() + " (" + saved.getEmployeeId() + ") saved successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error saving faculty: " + e.getMessage());
        }
        return "redirect:/admin/faculty";
    }

    @PostMapping("/faculty/delete/{id}")
    public String deleteFaculty(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Faculty f = erpService.getFacultyById(id);
            erpService.deleteFaculty(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Faculty member " + f.getFullName() + " removed successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Could not remove faculty: " + e.getMessage());
        }
        return "redirect:/admin/faculty";
    }

    // ==================== 4. DEPARTMENTS, COURSES & SUBJECTS ====================

    @GetMapping("/academics")
    public String academics(Model model) {
        model.addAttribute("activePage", "academics");
        model.addAttribute("departments", erpService.getAllDepartments());
        model.addAttribute("courses", erpService.getAllCourses());
        model.addAttribute("subjects", erpService.getAllSubjects());
        model.addAttribute("facultyList", erpService.getAllFaculty());
        return "admin/academics";
    }

    @PostMapping("/departments/save")
    public String saveDepartment(@RequestParam String code,
                                 @RequestParam String name,
                                 @RequestParam(required = false) String headOfDepartment,
                                 @RequestParam(required = false) String description,
                                 @RequestParam(required = false) String coursesOffered,
                                 @RequestParam(defaultValue = "8") Integer totalSemesters,
                                 RedirectAttributes redirectAttributes) {
        try {
            erpService.saveDepartment(code, name, headOfDepartment, description, coursesOffered, totalSemesters);
            redirectAttributes.addFlashAttribute("successMessage", "Department " + code.toUpperCase() + " saved successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error saving department: " + e.getMessage());
        }
        return "redirect:/admin/academics";
    }

    @PostMapping("/departments/delete/{id}")
    public String deleteDepartment(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            erpService.deleteDepartment(id);
            redirectAttributes.addFlashAttribute("successMessage", "Department deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Cannot delete department with linked students/courses.");
        }
        return "redirect:/admin/academics";
    }

    @PostMapping("/courses/save")
    public String saveCourse(@RequestParam String code,
                             @RequestParam String name,
                             @RequestParam(defaultValue = "4") Integer durationYears,
                             @RequestParam(defaultValue = "8") Integer totalSemesters,
                             @RequestParam Long departmentId,
                             RedirectAttributes redirectAttributes) {
        try {
            erpService.saveCourse(code, name, durationYears, totalSemesters, departmentId);
            redirectAttributes.addFlashAttribute("successMessage", "Course " + code.toUpperCase() + " saved successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error saving course: " + e.getMessage());
        }
        return "redirect:/admin/academics";
    }

    @PostMapping("/courses/delete/{id}")
    public String deleteCourse(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            erpService.deleteCourse(id);
            redirectAttributes.addFlashAttribute("successMessage", "Course deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Cannot delete course with enrolled students.");
        }
        return "redirect:/admin/academics";
    }

    @PostMapping("/subjects/save")
    public String saveSubject(@RequestParam String code,
                              @RequestParam String name,
                              @RequestParam(defaultValue = "4") Integer credits,
                              @RequestParam(defaultValue = "5") Integer semester,
                              @RequestParam(defaultValue = "A") String section,
                              @RequestParam(defaultValue = "THEORY") String subjectType,
                              @RequestParam Long departmentId,
                              @RequestParam Long courseId,
                              @RequestParam(required = false) Long facultyId,
                              RedirectAttributes redirectAttributes) {
        try {
            erpService.saveSubject(code, name, credits, semester, section, subjectType, departmentId, courseId, facultyId);
            redirectAttributes.addFlashAttribute("successMessage", "Subject " + code.toUpperCase() + " saved successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error saving subject: " + e.getMessage());
        }
        return "redirect:/admin/academics";
    }

    @PostMapping("/subjects/assign-faculty")
    public String assignFacultyToSubject(@RequestParam Long subjectId,
                                         @RequestParam(required = false) Long facultyId,
                                         @RequestParam(defaultValue = "A") String section,
                                         @RequestParam(defaultValue = "academics") String returnPage,
                                         RedirectAttributes redirectAttributes) {
        try {
            Subject updated = erpService.assignFacultyToSubject(subjectId, facultyId, section);
            String facName = updated.getAssignedFaculty() != null ? updated.getAssignedFaculty().getFullName() : "Unassigned";
            redirectAttributes.addFlashAttribute("successMessage",
                    "Assigned " + facName + " to " + updated.getCode() + " (Section " + updated.getSection() + ")");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error assigning faculty: " + e.getMessage());
        }
        return "faculty".equalsIgnoreCase(returnPage) ? "redirect:/admin/faculty" : "redirect:/admin/academics";
    }

    @PostMapping("/subjects/delete/{id}")
    public String deleteSubject(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            erpService.deleteSubject(id);
            redirectAttributes.addFlashAttribute("successMessage", "Subject deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting subject: " + e.getMessage());
        }
        return "redirect:/admin/academics";
    }

    // ==================== 5. TIMETABLE & EXAMINATIONS ====================

    @GetMapping("/schedule")
    public String schedule(@RequestParam(required = false) Long departmentId,
                           @RequestParam(required = false) Integer semester,
                           Model model) {
        model.addAttribute("activePage", "schedule");
        model.addAttribute("timetables", erpService.getTimetableForDepartmentAndSemester(departmentId, semester));
        model.addAttribute("examinations", erpService.getAllExaminations());
        model.addAttribute("departments", erpService.getAllDepartments());
        model.addAttribute("subjects", erpService.getAllSubjects());
        model.addAttribute("selectedDepartmentId", departmentId);
        model.addAttribute("selectedSemester", semester);
        return "admin/schedule";
    }

    @PostMapping("/timetable/save")
    public String saveTimetable(@RequestParam String dayOfWeek,
                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime,
                                @RequestParam Long subjectId,
                                @RequestParam(defaultValue = "A") String section,
                                @RequestParam String roomNumber,
                                @RequestParam(defaultValue = "LECTURE") String slotType,
                                RedirectAttributes redirectAttributes) {
        try {
            erpService.saveTimetableSlot(dayOfWeek, startTime, endTime, subjectId, section, roomNumber, slotType);
            redirectAttributes.addFlashAttribute("successMessage", "Timetable slot added successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error adding timetable slot: " + e.getMessage());
        }
        return "redirect:/admin/schedule";
    }

    @PostMapping("/timetable/delete/{id}")
    public String deleteTimetable(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        erpService.deleteTimetableSlot(id);
        redirectAttributes.addFlashAttribute("successMessage", "Timetable slot deleted.");
        return "redirect:/admin/schedule";
    }

    @PostMapping("/examinations/save")
    public String saveExamination(@RequestParam String name,
                                  @RequestParam String examType,
                                  @RequestParam Long subjectId,
                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate examDate,
                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime,
                                  @RequestParam String roomNumber,
                                  @RequestParam(defaultValue = "100") Integer maxMarks,
                                  @RequestParam(defaultValue = "UPCOMING") String status,
                                  RedirectAttributes redirectAttributes) {
        try {
            erpService.saveExamination(name, examType, subjectId, examDate, startTime, endTime, roomNumber, maxMarks, status);
            redirectAttributes.addFlashAttribute("successMessage", "Examination scheduled successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error scheduling examination: " + e.getMessage());
        }
        return "redirect:/admin/schedule";
    }

    @PostMapping("/examinations/delete/{id}")
    public String deleteExamination(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        erpService.deleteExamination(id);
        redirectAttributes.addFlashAttribute("successMessage", "Examination removed.");
        return "redirect:/admin/schedule";
    }

    // ==================== 6. ATTENDANCE, MARKS & PERFORMANCE ANALYTICS ====================

    @GetMapping("/analytics")
    public String analytics(@RequestParam(required = false) Long departmentId, Model model) {
        List<StudentPerformanceDto> performances = erpService.getAllStudentPerformances(departmentId);
        List<StudentPerformanceDto> lowAttendance = erpService.getLowAttendanceStudents(departmentId);

        List<String> studentLabels = performances.stream().map(p -> p.getStudent().getRollNumber()).toList();
        List<Double> attendanceSeries = performances.stream().map(StudentPerformanceDto::getAttendancePercentage).toList();
        List<Double> marksSeries = performances.stream().map(StudentPerformanceDto::getAveragePercentage).toList();

        model.addAttribute("activePage", "analytics");
        model.addAttribute("performances", performances);
        model.addAttribute("lowAttendance", lowAttendance);
        model.addAttribute("departments", erpService.getAllDepartments());
        model.addAttribute("selectedDepartmentId", departmentId);
        model.addAttribute("studentLabels", studentLabels);
        model.addAttribute("attendanceSeries", attendanceSeries);
        model.addAttribute("marksSeries", marksSeries);
        return "admin/analytics";
    }

    @GetMapping("/reports/performance/pdf")
    public ResponseEntity<byte[]> downloadPerformanceReport(@RequestParam(required = false) Long departmentId) {
        List<StudentPerformanceDto> performances = erpService.getAllStudentPerformances(departmentId);
        byte[] pdf = pdfReportService.generateUniversityPerformancePdf(
                "UNIVERSITY STUDENT ATTENDANCE & ACADEMIC PERFORMANCE REPORT", performances);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=University_Performance_Report.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/reports/low-attendance/pdf")
    public ResponseEntity<byte[]> downloadLowAttendanceReport(@RequestParam(required = false) Long departmentId) {
        List<StudentPerformanceDto> lowList = erpService.getLowAttendanceStudents(departmentId);
        byte[] pdf = pdfReportService.generateUniversityPerformancePdf(
                "CRITICAL ALERT REPORT: STUDENTS WITH ATTENDANCE BELOW 75%", lowList);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Low_Attendance_Alert_Report.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    // ==================== 7. NOTICE BOARD MANAGEMENT ====================

    @GetMapping("/notices")
    public String notices(Model model) {
        model.addAttribute("activePage", "notices");
        model.addAttribute("notices", erpService.getAllNotices());
        model.addAttribute("departments", erpService.getAllDepartments());
        return "admin/notices";
    }

    @PostMapping("/notices/save")
    public String saveNotice(@RequestParam String title,
                             @RequestParam String content,
                             @RequestParam String category,
                             @RequestParam String targetAudience,
                             @RequestParam(required = false) Long departmentId,
                             @RequestParam(defaultValue = "false") boolean pinned,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        User admin = erpService.getUserByUsername(authentication.getName());
        erpService.saveNotice(title, content, category, targetAudience, departmentId, admin, pinned);
        redirectAttributes.addFlashAttribute("successMessage", "Notice published to university notice board!");
        return "redirect:/admin/notices";
    }

    @PostMapping("/notices/pin/{id}")
    public String togglePinNotice(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        erpService.togglePinNotice(id);
        redirectAttributes.addFlashAttribute("successMessage", "Notice pin status updated.");
        return "redirect:/admin/notices";
    }

    @PostMapping("/notices/delete/{id}")
    public String deleteNotice(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        erpService.deleteNotice(id);
        redirectAttributes.addFlashAttribute("successMessage", "Notice removed from board.");
        return "redirect:/admin/notices";
    }
}

