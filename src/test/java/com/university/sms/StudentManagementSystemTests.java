package com.university.sms;

import com.university.sms.dto.AttendanceSummaryDto;
import com.university.sms.dto.StudentPerformanceDto;
import com.university.sms.entity.*;
import com.university.sms.service.PdfReportService;
import com.university.sms.service.UniversityErpService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:test_sms_db;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa",
    "spring.datasource.password="
})
class StudentManagementSystemTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UniversityErpService erpService;

    @Autowired
    private PdfReportService pdfReportService;

    @Test
    void contextLoadsAndDemoDataIsSeeded() {
        assertThat(erpService.getAllDepartments()).hasSizeGreaterThanOrEqualTo(4);
        assertThat(erpService.getAllCourses()).hasSizeGreaterThanOrEqualTo(4);
        assertThat(erpService.getAllFaculty()).hasSizeGreaterThanOrEqualTo(4);
        assertThat(erpService.getAllStudents()).hasSizeGreaterThanOrEqualTo(9);
        assertThat(erpService.getAllSubjects()).hasSizeGreaterThanOrEqualTo(8);

        // Verify <75% Low Attendance Alerts are accurately detected
        List<StudentPerformanceDto> lowAttendanceStudents = erpService.getLowAttendanceStudents(null);
        assertThat(lowAttendanceStudents).isNotEmpty();
        assertThat(lowAttendanceStudents)
                .extracting(p -> p.getStudent().getRollNumber())
                .contains("STU2026003", "STU2026005");
    }

    @Test
    void loginPortalsRenderCleanly() throws Exception {
        mockMvc.perform(get("/login?portal=admin")).andExpect(status().isOk());
        mockMvc.perform(get("/login?portal=faculty")).andExpect(status().isOk());
        mockMvc.perform(get("/login?portal=student")).andExpect(status().isOk());
    }

    @Test
    void unauthenticatedUserIsRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void adminCanAccessAllAdminModulesAndPdfReports() throws Exception {
        Student firstStudent = erpService.getAllStudents().get(0);

        mockMvc.perform(get("/admin/dashboard")).andExpect(status().isOk());
        mockMvc.perform(get("/admin/students")).andExpect(status().isOk());
        mockMvc.perform(get("/admin/students/view/" + firstStudent.getId())).andExpect(status().isOk());
        mockMvc.perform(get("/admin/faculty")).andExpect(status().isOk());
        mockMvc.perform(get("/admin/academics")).andExpect(status().isOk());
        mockMvc.perform(get("/admin/schedule")).andExpect(status().isOk());
        mockMvc.perform(get("/admin/analytics")).andExpect(status().isOk());
        mockMvc.perform(get("/admin/notices")).andExpect(status().isOk());

        mockMvc.perform(get("/admin/reports/performance/pdf"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"));

        mockMvc.perform(get("/admin/reports/low-attendance/pdf"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"));
    }

    @Test
    @WithMockUser(username = "faculty1", roles = {"FACULTY"})
    void facultyCanAccessAllFacultyViewsAndMarkAttendanceAndMarks() throws Exception {
        Faculty fac1 = erpService.getFacultyByUsername("faculty1");
        Subject assignedSub = erpService.getSubjectsByFaculty(fac1.getId()).get(0);

        mockMvc.perform(get("/faculty/dashboard")).andExpect(status().isOk());
        mockMvc.perform(get("/faculty/attendance")).andExpect(status().isOk());
        mockMvc.perform(get("/faculty/marks")).andExpect(status().isOk());
        mockMvc.perform(get("/faculty/assignments")).andExpect(status().isOk());
        mockMvc.perform(get("/faculty/performance")).andExpect(status().isOk());

        mockMvc.perform(get("/faculty/reports/attendance/pdf").param("subjectId", String.valueOf(assignedSub.getId())))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"));

        // Verify faculty cannot access Admin portal
        mockMvc.perform(get("/admin/dashboard")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "student3", roles = {"STUDENT"})
    void studentCanAccessOwnRecordsAndLowAttendanceAlertAndPdfMarksheet() throws Exception {
        mockMvc.perform(get("/student/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("CRITICAL ACADEMIC ALERT: ATTENDANCE BELOW 75%")));
        mockMvc.perform(get("/student/attendance")).andExpect(status().isOk());
        mockMvc.perform(get("/student/marks")).andExpect(status().isOk());
        mockMvc.perform(get("/student/academics")).andExpect(status().isOk());
        mockMvc.perform(get("/student/profile")).andExpect(status().isOk());

        byte[] pdfBytes = mockMvc.perform(get("/student/reports/marksheet/pdf"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(pdfBytes, 0, 4)).isEqualTo("%PDF");

        byte[] attPdfBytes = mockMvc.perform(get("/student/reports/attendance/pdf"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(attPdfBytes, 0, 4)).isEqualTo("%PDF");

        // Role restriction check
        mockMvc.perform(get("/admin/dashboard")).andExpect(status().isForbidden());
        mockMvc.perform(get("/faculty/dashboard")).andExpect(status().isForbidden());
    }

    @Test
    void facultyCanOnlyModifyAssignedSubjects() {
        Faculty fac1 = erpService.getFacultyByUsername("faculty1");
        Faculty fac2 = erpService.getFacultyByUsername("faculty2");

        List<Subject> fac2Subjects = erpService.getSubjectsByFaculty(fac2.getId());
        assertThat(fac2Subjects).isNotEmpty();
        Subject fac2Subject = fac2Subjects.get(0);

        Student student = erpService.getStudentByUsername("student1");

        // Faculty 1 attempting to modify marks for Faculty 2's subject must throw AccessDeniedException
        assertThatThrownBy(() ->
                erpService.saveOrUpdateMark(student.getId(), fac2Subject.getId(), 25.0, 15.0, 15.0, 25.0, "Unauthorized", fac1)
        ).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void pdfReportsGenerateValidPdfDocuments() {
        Student student = erpService.getStudentByUsername("student3");
        StudentPerformanceDto perf = erpService.getStudentPerformance(student);
        List<Mark> marks = erpService.getMarksForStudent(student.getId());
        List<AttendanceSummaryDto> summaries = erpService.getSubjectWiseAttendanceForStudent(student.getId());

        byte[] marksheetPdf = pdfReportService.generateStudentMarksheetPdf(student, marks, perf);
        byte[] attendancePdf = pdfReportService.generateStudentAttendancePdf(student, summaries, perf);

        assertThat(marksheetPdf).isNotEmpty();
        assertThat(attendancePdf).isNotEmpty();
        assertThat(new String(marksheetPdf, 0, 4)).isEqualTo("%PDF");
        assertThat(new String(attendancePdf, 0, 4)).isEqualTo("%PDF");
    }
}
