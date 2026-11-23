package com.university.sms.controller;

import com.university.sms.core.concurrent.ConcurrentReportEngine;
import com.university.sms.core.exception.SmsException;
import com.university.sms.core.generic.Pair;
import com.university.sms.core.generic.Result;
import com.university.sms.core.io.CsvStudentImporter;
import com.university.sms.core.io.SerializationBackupService;
import com.university.sms.core.io.StreamUtils;
import com.university.sms.entity.User;
import com.university.sms.service.JavaLabService;
import com.university.sms.service.UniversityErpService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * "Java Lab Console" - admin page that exposes every Unit 1-5 module as a working feature
 * (Prit Patel - integration; each section belongs to the member named in the UI).
 */
@Controller
@RequestMapping("/admin/java-lab")
public class JavaLabController {

    /** One row of the syllabus coverage map shown on the Overview tab. */
    public record ConceptRow(String unit, String concept, String member, String classes, String feature) {
    }

    static final List<ConceptRow> CONCEPT_MAP = List.of(
            new ConceptRow("Unit 1", "Classes, objects, constructors, this, static, final, packages, inheritance",
                    "Prit Patel", "Person, StudentRecord, FacultyRecord, RollNumber, UniversityInfo",
                    "Plain-Java domain model used by every module"),
            new ConceptRow("Unit 2", "Polymorphism (overloading, overriding, dynamic dispatch, abstract class, interface)",
                    "Prit Patel", "GradingPolicy, AbstractGradingPolicy, Absolute/Relative/ModerationGradingPolicy, GradeCalculator, Grade",
                    "Grading Policy Simulator + real grade calculation of every mark"),
            new ConceptRow("Unit 2", "Exception handling (checked/unchecked, custom exceptions, throw/throws, chaining, try-with-resources)",
                    "Anjay", "SmsException, InvalidMarksException, DataImportException, AttendanceShortageException, MarksValidator, GlobalExceptionHandler",
                    "Marks validation, CSV error report, hall-ticket eligibility"),
            new ConceptRow("Unit 3", "I/O streams (byte, character, buffered, data, object, GZIP, file streams)",
                    "Anjay", "CsvStudentImporter, CsvStudentExporter, SerializationBackupService, AuditTrailLogger, StreamUtils",
                    "Bulk admission CSV, performance CSV, .smsbak backups, audit trail"),
            new ConceptRow("Unit 3", "Multithreading (Thread, Runnable, Callable, thread pool, synchronization, wait/notify, life cycle, priorities)",
                    "Vivekanand", "ConcurrentReportEngine, AlertQueue, AttendanceAlertMonitor, PortalActivityMonitor, ThreadLifecycleTracer, ActivityTrackingInterceptor",
                    "Parallel report cards, alert pipeline, live request monitor"),
            new ConceptRow("Unit 3", "Event handling with AWT (ActionListener, ItemListener, WindowAdapter, KeyAdapter)",
                    "Vivekanand", "desktop.awt.ControlPanelLauncher",
                    "Desktop control panel that starts/stops the server"),
            new ConceptRow("Unit 4", "JDBC (DriverManager, Connection, Statement, PreparedStatement, ResultSet, metadata, transactions, batch)",
                    "Arindam", "SqliteArchiveDao, ArchivedStudent, ArchiveSummary",
                    "Offline SQLite semester archive"),
            new ConceptRow("Unit 4", "Collections (List, Set, Map, Queue, PriorityQueue, TreeMap, EnumMap, Iterator, Comparator, Collections)",
                    "Arindam", "AcademicAnalyticsEngine, AnalyticsReport",
                    "Rank list, toppers, department analytics"),
            new ConceptRow("Unit 5", "Generics (generic classes, interfaces, methods, bounded types, wildcards)",
                    "Prit Patel", "Pair, Result, GenericRepository, InMemoryRepository, GenericUtils, AlertQueue<T>",
                    "Type-safe results, repository cache, utilities"),
            new ConceptRow("Unit 5", "JavaFX (Application, Stage, Scene, layouts, TableView, MenuBar, charts, Task)",
                    "Arindam", "desktop.fx.SmsDesktopApp, desktop.fx.DesktopLauncher",
                    "Desktop archive viewer reading the SQLite file")
    );

    private final JavaLabService labService;
    private final UniversityErpService erpService;

    public JavaLabController(JavaLabService labService, UniversityErpService erpService) {
        this.labService = labService;
        this.erpService = erpService;
    }

    @ModelAttribute
    public void addCommonAttributes(Authentication authentication, Model model) {
        if (authentication != null) {
            User adminUser = erpService.getUserByUsername(authentication.getName());
            model.addAttribute("currentUser", adminUser);
        }
        model.addAttribute("activePage", "javalab");
    }

    @GetMapping
    public String console(@RequestParam(defaultValue = "overview") String tab,
                          @RequestParam(defaultValue = "absolute") String policy,
                          @RequestParam(required = false) String search,
                          @RequestParam(required = false) String archiveSearch,
                          Model model) {
        model.addAttribute("tab", tab);
        model.addAttribute("conceptMap", CONCEPT_MAP);
        model.addAttribute("policy", policy);
        model.addAttribute("grading", labService.simulateGrading(policy));
        model.addAttribute("analytics", labService.analytics(search));
        model.addAttribute("search", search);
        model.addAttribute("auditLines", labService.auditTail(15));
        model.addAttribute("monitor", labService.getActivityMonitor());
        model.addAttribute("cachedRecords", labService.cachedRecordCount());
        model.addAttribute("archiveSearch", archiveSearch);
        try {
            model.addAttribute("archive", labService.archiveSummary());
            model.addAttribute("archivedStudents", labService.archivedStudents(archiveSearch));
            model.addAttribute("archiveAverages", labService.archiveDepartmentAverages());
        } catch (SQLException e) {
            model.addAttribute("archiveError", "SQLite archive unavailable: " + e.getMessage());
        }
        return "admin/java-lab";
    }

    // ---------------- Vivekanand: multithreading ----------------

    @PostMapping("/threads/reports")
    public String runReports(@RequestParam(defaultValue = "4") int threads, RedirectAttributes ra) {
        try {
            int poolSize = Math.max(1, Math.min(threads, 16));
            Pair<ConcurrentReportEngine.ReportRun, ConcurrentReportEngine.ReportRun> runs =
                    labService.compareReportGeneration(poolSize);
            ra.addFlashAttribute("sequentialRun", runs.first());
            ra.addFlashAttribute("parallelRun", runs.second());
            double speedUp = runs.second().elapsedMillis() == 0 ? 1 :
                    (double) runs.first().elapsedMillis() / runs.second().elapsedMillis();
            ra.addFlashAttribute("speedUp", String.format("%.1fx", speedUp));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            ra.addFlashAttribute("errorMessage", "Report generation was interrupted");
        }
        return "redirect:/admin/java-lab?tab=threads";
    }

    /** Runs all four multithreading demos in one click (used in the video presentation). */
    @PostMapping("/threads/all")
    public String runAll(RedirectAttributes ra) throws InterruptedException {
        runReports(4, ra);
        runAlerts(ra);
        lifecycle(ra);
        race(ra);
        return "redirect:/admin/java-lab?tab=threads";
    }

    @PostMapping("/threads/alerts")
    public String runAlerts(RedirectAttributes ra) throws InterruptedException {
        ra.addFlashAttribute("alertLog", labService.runAlertPipeline());
        return "redirect:/admin/java-lab?tab=threads";
    }

    @PostMapping("/threads/lifecycle")
    public String lifecycle(RedirectAttributes ra) throws InterruptedException {
        ra.addFlashAttribute("lifecycle", labService.traceThreadLifecycle());
        return "redirect:/admin/java-lab?tab=threads";
    }

    @PostMapping("/threads/race")
    public String race(RedirectAttributes ra) throws InterruptedException {
        Pair<Long, Long> r = labService.raceConditionDemo();
        ra.addFlashAttribute("raceUnsafe", r.first());
        ra.addFlashAttribute("raceSafe", r.second());
        ra.addFlashAttribute("raceExpected", 8L * 50_000);
        return "redirect:/admin/java-lab?tab=threads";
    }

    // ---------------- Anjay: I/O streams ----------------

    @GetMapping("/io/export.csv")
    public void exportCsv(HttpServletResponse response, Authentication auth) throws IOException {
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=student_performance_" + LocalDate.now() + ".csv");
        labService.exportCsv(response.getOutputStream(), auth.getName());
    }

    @GetMapping("/io/template.csv")
    public void template(HttpServletResponse response) throws IOException {
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=bulk_admission_template.csv");
        response.getOutputStream().write(CsvStudentImporter.sampleCsv().getBytes(StandardCharsets.UTF_8));
    }

    @PostMapping("/io/import")
    public String importCsv(@RequestParam("file") MultipartFile file, Authentication auth, RedirectAttributes ra) {
        if (file.isEmpty()) {
            ra.addFlashAttribute("errorMessage", "Please choose a CSV file first.");
            return "redirect:/admin/java-lab?tab=io";
        }
        try (InputStream in = file.getInputStream()) {
            Result<Integer> result = labService.importCsv(in, auth.getName());
            ra.addFlashAttribute("importResult", result);
            if (result.getValue() != null && result.getValue() > 0) {
                ra.addFlashAttribute("successMessage", result.getSummary());
            } else {
                ra.addFlashAttribute("errorMessage", result.getSummary());
            }
        } catch (IOException e) {
            ra.addFlashAttribute("errorMessage", "Upload failed: " + e.getMessage());
        }
        return "redirect:/admin/java-lab?tab=io";
    }

    @GetMapping("/io/backup")
    public void backup(HttpServletResponse response, Authentication auth) throws IOException {
        response.setContentType("application/octet-stream");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=sms_backup_" + LocalDate.now() + ".smsbak");
        labService.backup(response.getOutputStream(), auth.getName());
    }

    @PostMapping("/io/verify-backup")
    public String verifyBackup(@RequestParam("file") MultipartFile file, Authentication auth, RedirectAttributes ra) {
        try (InputStream in = file.getInputStream()) {
            SerializationBackupService.BackupContents contents = labService.verifyBackup(in, auth.getName());
            ra.addFlashAttribute("backupContents", contents);
            ra.addFlashAttribute("successMessage", "Backup is valid: " + contents.students().size()
                    + " student objects deserialized.");
        } catch (SmsException e) {
            ra.addFlashAttribute("errorMessage", e.toString());
        } catch (IOException e) {
            ra.addFlashAttribute("errorMessage", "Upload failed: " + e.getMessage());
        }
        return "redirect:/admin/java-lab?tab=io";
    }

    // ---------------- Arindam: JDBC archive ----------------

    @PostMapping("/archive/snapshot")
    public String snapshot(Authentication auth, RedirectAttributes ra) {
        try {
            int n = labService.createArchiveSnapshot(auth.getName());
            ra.addFlashAttribute("successMessage", "SQLite archive refreshed in one transaction: " + n + " students written.");
        } catch (SQLException e) {
            ra.addFlashAttribute("errorMessage", "Archive failed and was rolled back: " + e.getMessage());
        }
        return "redirect:/admin/java-lab?tab=jdbc";
    }

    @GetMapping("/archive/download")
    public void downloadArchive(HttpServletResponse response) throws IOException {
        Path db = labService.getArchiveDao().getDbFile();
        if (!Files.exists(db)) {
            throw new com.university.sms.core.exception.RecordNotFoundException("Archive file", db.getFileName());
        }
        response.setContentType("application/x-sqlite3");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=sms_archive.db");
        StreamUtils.copyFile(db, response.getOutputStream());
    }
}
