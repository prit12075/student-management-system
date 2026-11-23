package com.university.sms.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.university.sms.dto.AttendanceSummaryDto;
import com.university.sms.dto.StudentPerformanceDto;
import com.university.sms.entity.Mark;
import com.university.sms.entity.Student;
import com.university.sms.entity.Subject;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import com.university.sms.core.verify.MarksheetFingerprint;

@Service
public class PdfReportService {

    private final MarksheetFingerprint fingerprint;

    public PdfReportService(MarksheetFingerprint fingerprint) {
        this.fingerprint = fingerprint;
    }

    private static final Color PRIMARY_BLUE = new Color(15, 52, 96);
    private static final Color ACCENT_BLUE = new Color(29, 93, 155);
    private static final Color LIGHT_BG = new Color(241, 245, 249);
    private static final Color DANGER_RED = new Color(185, 28, 28);
    private static final Color SUCCESS_GREEN = new Color(21, 128, 61);

    public byte[] generateStudentMarksheetPdf(Student student, List<Mark> marks, StudentPerformanceDto performance) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(doc, out);
            doc.open();

            addUniversityHeader(doc, "OFFICIAL STATEMENT OF MARKS / TRANSCRIPT");

            // Student Details Box
            PdfPTable infoTable = new PdfPTable(4);
            infoTable.setWidthPercentage(100);
            infoTable.setSpacingBefore(12f);
            infoTable.setSpacingAfter(14f);
            infoTable.setWidths(new float[]{1.3f, 2.2f, 1.3f, 2.2f});

            addInfoRow(infoTable, "Student Name:", student.getFullName(), "Roll Number:", student.getRollNumber());
            addInfoRow(infoTable, "Programme / Course:", student.getCourse().getName(), "Department:", student.getDepartment().getName());
            addInfoRow(infoTable, "Semester / Section:", "Semester " + student.getSemester() + " (" + student.getSection() + ")", "Academic Session:", "2026 - 2027");
            addInfoRow(infoTable, "Overall Attendance:", performance.getAttendancePercentage() + "%", "Issue Date:", LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
            doc.add(infoTable);

            // Marks Table
            PdfPTable table = new PdfPTable(9);
            table.setWidthPercentage(100);
            table.setSpacingBefore(6f);
            table.setSpacingAfter(16f);
            table.setWidths(new float[]{1.1f, 2.8f, 0.8f, 1.0f, 1.0f, 1.0f, 1.0f, 1.1f, 1.0f});

            addTableHeader(table, new String[]{
                "Code", "Subject Title", "Cr", "Internal (30)", "Assign (20)", "Pract (20)", "Extern (30)", "Total (100)", "Grade"
            });

            Font rowFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, PRIMARY_BLUE);

            boolean alternate = false;
            for (Mark m : marks) {
                Color bg = alternate ? LIGHT_BG : Color.WHITE;
                addCell(table, m.getSubject().getCode(), boldFont, bg, Element.ALIGN_CENTER);
                addCell(table, m.getSubject().getName(), rowFont, bg, Element.ALIGN_LEFT);
                addCell(table, String.valueOf(m.getSubject().getCredits()), rowFont, bg, Element.ALIGN_CENTER);
                addCell(table, String.format("%.1f", m.getInternalMarks()), rowFont, bg, Element.ALIGN_CENTER);
                addCell(table, String.format("%.1f", m.getAssignmentMarks()), rowFont, bg, Element.ALIGN_CENTER);
                addCell(table, String.format("%.1f", m.getPracticalMarks()), rowFont, bg, Element.ALIGN_CENTER);
                addCell(table, String.format("%.1f", m.getExternalMarks()), rowFont, bg, Element.ALIGN_CENTER);
                addCell(table, String.format("%.1f", m.getTotalMarks()), boldFont, bg, Element.ALIGN_CENTER);

                Font gradeFont = FontFactory.getFont(
                        FontFactory.HELVETICA_BOLD, 9,
                        "PASS".equalsIgnoreCase(m.getResultStatus()) ? SUCCESS_GREEN : DANGER_RED
                );
                addCell(table, m.getGrade() + " (" + m.getResultStatus() + ")", gradeFont, bg, Element.ALIGN_CENTER);
                alternate = !alternate;
            }
            doc.add(table);

            // Summary Box
            PdfPTable summaryTable = new PdfPTable(4);
            summaryTable.setWidthPercentage(100);
            summaryTable.setSpacingAfter(25f);
            String resultText = performance.getBacklogs() == 0 && performance.getSubjectsEvaluated() > 0 ? "PASSED" : "PROMOTED / PENDING";
            addInfoRow(summaryTable, "SGPA:", String.format("%.2f / 10.0", performance.getSgpa()),
                    "Aggregate Percentage:", String.format("%.1f%%", performance.getAveragePercentage()));
            addInfoRow(summaryTable, "Overall Grade:", performance.getOverallGrade(),
                    "Final Result Status:", resultText);
            doc.add(summaryTable);

            if (performance.isLowAttendanceAlert()) {
                Paragraph alert = new Paragraph(
                        "ACADEMIC NOTICE: Student attendance (" + performance.getAttendancePercentage() + "%) is below the mandatory 75% university requirement in one or more subjects.",
                        FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, DANGER_RED)
                );
                alert.setSpacingAfter(20f);
                doc.add(alert);
            }

            // Novel feature (Anjay): tamper-proof verification code sealed with HMAC-SHA256
            String code = fingerprint.codeFor(student.getRollNumber(), SmartCampusService.linesOf(marks), performance.getSgpa());
            Paragraph seal = new Paragraph("VERIFICATION CODE: " + code
                    + "   |   Verify this marksheet online at /verify with the roll number and this code.",
                    FontFactory.getFont(FontFactory.COURIER_BOLD, 9, ACCENT_BLUE));
            seal.setSpacingAfter(15f);
            doc.add(seal);

            addSignatureFooter(doc);
            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate student marksheet PDF", e);
        }
    }

    public byte[] generateStudentAttendancePdf(Student student, List<AttendanceSummaryDto> summaries, StudentPerformanceDto performance) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(doc, out);
            doc.open();

            addUniversityHeader(doc, "STUDENT SUBJECT-WISE ATTENDANCE REPORT");

            PdfPTable infoTable = new PdfPTable(4);
            infoTable.setWidthPercentage(100);
            infoTable.setSpacingBefore(12f);
            infoTable.setSpacingAfter(14f);
            addInfoRow(infoTable, "Student Name:", student.getFullName(), "Roll Number:", student.getRollNumber());
            addInfoRow(infoTable, "Department:", student.getDepartment().getName(), "Overall Attendance:", performance.getAttendancePercentage() + "%");
            doc.add(infoTable);

            PdfPTable table = new PdfPTable(7);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1.2f, 2.8f, 1.0f, 1.0f, 1.0f, 1.2f, 1.5f});
            addTableHeader(table, new String[]{
                "Code", "Subject Name", "Total", "Present", "Absent", "Percentage", "Status"
            });

            Font rowFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);
            boolean alt = false;
            for (AttendanceSummaryDto dto : summaries) {
                Color bg = alt ? LIGHT_BG : Color.WHITE;
                addCell(table, dto.getSubject().getCode(), rowFont, bg, Element.ALIGN_CENTER);
                addCell(table, dto.getSubject().getName(), rowFont, bg, Element.ALIGN_LEFT);
                addCell(table, String.valueOf(dto.getTotalClasses()), rowFont, bg, Element.ALIGN_CENTER);
                addCell(table, String.valueOf(dto.getPresentCount()), rowFont, bg, Element.ALIGN_CENTER);
                addCell(table, String.valueOf(dto.getAbsentCount()), rowFont, bg, Element.ALIGN_CENTER);
                addCell(table, dto.getAttendancePercentage() + "%", rowFont, bg, Element.ALIGN_CENTER);

                Font statusFont = FontFactory.getFont(
                        FontFactory.HELVETICA_BOLD, 9,
                        dto.isLowAttendance() ? DANGER_RED : SUCCESS_GREEN
                );
                addCell(table, dto.isLowAttendance() ? "ALERT (<75%)" : "ELIGIBLE (>=75%)", statusFont, bg, Element.ALIGN_CENTER);
                alt = !alt;
            }
            doc.add(table);

            addSignatureFooter(doc);
            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate attendance PDF", e);
        }
    }

    public byte[] generateSubjectAttendancePdf(Subject subject, List<AttendanceSummaryDto> summaries) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(doc, out);
            doc.open();

            addUniversityHeader(doc, "FACULTY SUBJECT ATTENDANCE REGISTER REPORT");

            PdfPTable infoTable = new PdfPTable(4);
            infoTable.setWidthPercentage(100);
            infoTable.setSpacingBefore(12f);
            infoTable.setSpacingAfter(14f);
            addInfoRow(infoTable, "Subject Code:", subject.getCode(), "Subject Title:", subject.getName());
            addInfoRow(infoTable, "Department:", subject.getDepartment().getName(),
                    "Faculty In-Charge:", subject.getAssignedFaculty() != null ? subject.getAssignedFaculty().getFullName() : "Unassigned");
            doc.add(infoTable);

            PdfPTable table = new PdfPTable(7);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1.4f, 2.5f, 1.0f, 1.0f, 1.0f, 1.2f, 1.5f});
            addTableHeader(table, new String[]{
                "Roll No", "Student Name", "Classes", "Present", "Absent", "Attendance %", "Eligibility"
            });

            Font rowFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);
            boolean alt = false;
            for (AttendanceSummaryDto dto : summaries) {
                Color bg = alt ? LIGHT_BG : Color.WHITE;
                addCell(table, dto.getStudent().getRollNumber(), rowFont, bg, Element.ALIGN_CENTER);
                addCell(table, dto.getStudent().getFullName(), rowFont, bg, Element.ALIGN_LEFT);
                addCell(table, String.valueOf(dto.getTotalClasses()), rowFont, bg, Element.ALIGN_CENTER);
                addCell(table, String.valueOf(dto.getPresentCount()), rowFont, bg, Element.ALIGN_CENTER);
                addCell(table, String.valueOf(dto.getAbsentCount()), rowFont, bg, Element.ALIGN_CENTER);
                addCell(table, dto.getAttendancePercentage() + "%", rowFont, bg, Element.ALIGN_CENTER);

                Font statusFont = FontFactory.getFont(
                        FontFactory.HELVETICA_BOLD, 9,
                        dto.isLowAttendance() ? DANGER_RED : SUCCESS_GREEN
                );
                addCell(table, dto.isLowAttendance() ? "LOW (<75%)" : "REGULAR", statusFont, bg, Element.ALIGN_CENTER);
                alt = !alt;
            }
            doc.add(table);

            addSignatureFooter(doc);
            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate subject attendance PDF", e);
        }
    }

    public byte[] generateUniversityPerformancePdf(String reportTitle, List<StudentPerformanceDto> performances) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);
            PdfWriter.getInstance(doc, out);
            doc.open();

            addUniversityHeader(doc, reportTitle);

            PdfPTable table = new PdfPTable(8);
            table.setWidthPercentage(100);
            table.setSpacingBefore(14f);
            table.setWidths(new float[]{1.3f, 2.3f, 1.5f, 1.0f, 1.2f, 1.1f, 1.0f, 1.5f});
            addTableHeader(table, new String[]{
                "Roll No", "Student Name", "Department", "Sem", "Attendance %", "Avg Marks %", "SGPA", "Attendance Status"
            });

            Font rowFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);
            boolean alt = false;
            for (StudentPerformanceDto p : performances) {
                Color bg = alt ? LIGHT_BG : Color.WHITE;
                addCell(table, p.getStudent().getRollNumber(), rowFont, bg, Element.ALIGN_CENTER);
                addCell(table, p.getStudent().getFullName(), rowFont, bg, Element.ALIGN_LEFT);
                addCell(table, p.getStudent().getDepartment().getCode(), rowFont, bg, Element.ALIGN_CENTER);
                addCell(table, "Sem " + p.getStudent().getSemester(), rowFont, bg, Element.ALIGN_CENTER);
                addCell(table, p.getAttendancePercentage() + "%", rowFont, bg, Element.ALIGN_CENTER);
                addCell(table, p.getAveragePercentage() + "%", rowFont, bg, Element.ALIGN_CENTER);
                addCell(table, String.format("%.2f", p.getSgpa()), rowFont, bg, Element.ALIGN_CENTER);

                Font statusFont = FontFactory.getFont(
                        FontFactory.HELVETICA_BOLD, 9,
                        p.isLowAttendanceAlert() ? DANGER_RED : SUCCESS_GREEN
                );
                addCell(table, p.isLowAttendanceAlert() ? "CRITICAL (<75%)" : "GOOD STANDING", statusFont, bg, Element.ALIGN_CENTER);
                alt = !alt;
            }
            doc.add(table);

            addSignatureFooter(doc);
            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate university performance PDF", e);
        }
    }

    private void addUniversityHeader(Document doc, String subtitle) throws DocumentException {
        Font uniFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, PRIMARY_BLUE);
        Font subUniFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.GRAY);
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, ACCENT_BLUE);

        Paragraph uniName = new Paragraph("APEX NATIONAL INSTITUTE OF TECHNOLOGY & MANAGEMENT", uniFont);
        uniName.setAlignment(Element.ALIGN_CENTER);
        doc.add(uniName);

        Paragraph uniSub = new Paragraph("University ERP & Academic Examination Division • Accredited Grade 'A++'", subUniFont);
        uniSub.setAlignment(Element.ALIGN_CENTER);
        uniSub.setSpacingAfter(8f);
        doc.add(uniSub);

        Paragraph reportTitle = new Paragraph(subtitle, titleFont);
        reportTitle.setAlignment(Element.ALIGN_CENTER);
        reportTitle.setSpacingAfter(8f);
        doc.add(reportTitle);
    }

    private void addInfoRow(PdfPTable table, String label1, String val1, String label2, String val2) {
        Font lblFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, PRIMARY_BLUE);
        Font valFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);

        addCell(table, label1, lblFont, LIGHT_BG, Element.ALIGN_LEFT);
        addCell(table, val1, valFont, Color.WHITE, Element.ALIGN_LEFT);
        addCell(table, label2, lblFont, LIGHT_BG, Element.ALIGN_LEFT);
        addCell(table, val2, valFont, Color.WHITE, Element.ALIGN_LEFT);
    }

    private void addTableHeader(PdfPTable table, String[] headers) {
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
            cell.setBackgroundColor(PRIMARY_BLUE);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cell.setPadding(6f);
            cell.setBorderColor(new Color(203, 213, 225));
            table.addCell(cell);
        }
    }

    private void addCell(PdfPTable table, String text, Font font, Color bg, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "-", font));
        cell.setBackgroundColor(bg);
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(5f);
        cell.setBorderColor(new Color(203, 213, 225));
        table.addCell(cell);
    }

    private void addSignatureFooter(Document doc) throws DocumentException {
        PdfPTable sigTable = new PdfPTable(2);
        sigTable.setWidthPercentage(100);
        sigTable.setSpacingBefore(30f);

        Font sigFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, PRIMARY_BLUE);
        PdfPCell left = new PdfPCell(new Phrase("Generated by: University ERP Portal\nDate: " + LocalDate.now(), sigFont));
        left.setBorder(Rectangle.NO_BORDER);
        left.setHorizontalAlignment(Element.ALIGN_LEFT);

        PdfPCell right = new PdfPCell(new Phrase("Controller of Examinations & Registrar\nApex National Institute of Technology", sigFont));
        right.setBorder(Rectangle.NO_BORDER);
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);

        sigTable.addCell(left);
        sigTable.addCell(right);
        doc.add(sigTable);
    }
}

