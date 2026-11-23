# Apex University ERP – Student Management System

**CSE 309 – Advanced Java Programming • Course Project • SRM University AP**

This is a full-stack University ERP built with **Java 21 and Spring Boot 3.3**. Each topic of the Advanced Java syllabus (Units 1–5) is used as a real feature of the product:

| Unit | Java concepts | Owner | Where it lives |
|---|---|---|---|
| 1 | Classes, objects, constructors, `this`/`static`/`final`, inheritance, packages | **Prit Patel** | `core.model` |
| 2 | Polymorphism: overloading, overriding, abstract class, sealed interface, enum | **Prit Patel** | `core.grading` (the `Grade` enum grades every mark) |
| 2 | Exception handling: custom checked/unchecked exceptions, chaining, `@ControllerAdvice` | **Anjay** | `core.exception`, `GlobalExceptionHandler` |
| 3 | I/O streams: character, byte, buffered, data, object, GZIP and file streams | **Anjay** | `core.io` |
| 3 | Multithreading: thread pool, `Callable`/`Future`, `synchronized`, `wait`/`notifyAll`, life cycle | **Vivekanand** | `core.concurrent` |
| 3 | AWT event handling: `ActionListener`, `ItemListener`, `KeyAdapter`, `WindowAdapter` | **Vivekanand** | `desktop.awt.ControlPanelLauncher` |
| 4 | JDBC with SQLite: `PreparedStatement`, batch, transactions, metadata | **Arindam** | `core.jdbc` |
| 4 | Collections: `List`/`Set`/`Map`/`Queue`, `PriorityQueue`, `TreeMap`, `EnumMap`, `Iterator` | **Arindam** | `core.collections` |
| 5 | Generics: generic classes, interfaces and methods, bounded types, wildcards | **Prit Patel** | `core.generic` |
| 5 | JavaFX: `TableView`, `MenuBar`, `BarChart`, `FilteredList`, `Task` | **Arindam** | `desktop.fx.SmsDesktopApp` |

### Novelty – the "Smart Campus" layer (one novel feature per member)

| Gap in typical college systems | Our feature | Owner |
|---|---|---|
| Struggling students are noticed only after results | **Academic Risk Radar** (explainable 0–100 early-warning score from pluggable lambda risk factors) + **SGPA Goal Planner** (what-if marks, minimum % for a target SGPA) | Prit Patel |
| Printed marksheets can be edited and resubmitted | **Tamper-proof marksheets**: HMAC-SHA256 code on every PDF + public `/verify` page | Anjay |
| Students learn about an attendance shortage too late | **Automatic Alert Engine**: `ScheduledExecutorService` scans attendance; lock-free per-student **notification inbox** (also notified on published marks and seat allotment) | Vivekanand |
| Exam seating is made by hand; the same branch sits together | **Anti-malpractice seating plan**: no two neighbours from the same department, saved with JDBC, seat printed on the hall ticket | Arindam |

Admin → **Smart Campus** (`/admin/smart`); Student → **Goal Planner & Risk**, **Notifications**; public → **`/verify`**.

The full member-wise split is in [`CONTRIBUTIONS.md`](CONTRIBUTIONS.md).

## Submission contents

| Required item | Location |
|---|---|
| Project report (introduction, contributions, code of each class, screenshots), 39 pages | [`docs/Project_Report.pdf`](docs/Project_Report.pdf) (editable: `docs/Project_Report.docx`) |
| Working code | `src/` (110 Java classes, Thymeleaf templates, tests) |
| Video presentation | Slides with speaker notes: [`docs/Apex_ERP_Presentation.pptx`](docs/Apex_ERP_Presentation.pptx) • script: [`docs/VIDEO_SCRIPT.md`](docs/VIDEO_SCRIPT.md) • silent demo: [`docs/SMS_demo_walkthrough.mp4`](docs/SMS_demo_walkthrough.mp4) |
| Screenshots of the complete flow | [`docs/screenshots/`](docs/screenshots) |

## Features

- **Three secure portals**: Admin, Faculty and Student. They use Spring Security 6 with BCrypt passwords and role-based URL rules. A faculty member can only edit the subjects assigned to them.
- **Admin portal**: dashboard KPIs and charts, CRUD for students, faculty, departments, courses, subjects, timetable, exams and notices, analytics, and PDF reports.
- **Faculty portal**: bulk attendance marking and marks entry (internal 30, assignment 20, practical 20, external 30). Totals, grades, SGPA and PASS/FAIL are calculated automatically. Invalid marks are rejected by `InvalidMarksException`.
- **Student portal**: dashboard with a below-75% alert, subject-wise attendance with the number of classes still needed, results, timetable, PDF marksheet, and an **exam hall ticket**. The hall ticket is withheld through `AttendanceShortageException` when attendance is too low.
- **Java Lab Console** (`/admin/java-lab`): runs every team member's module live:
  - Grading policy simulator (absolute / relative bell curve / moderation)
  - Collections analytics (toppers, department averages, grade distribution)
  - CSV bulk admission with line-by-line error reports, performance CSV export, `.smsbak` serialized backup and verification, and the audit trail
  - Thread pool vs sequential report generation (about 3.6× faster), producer–consumer alert pipeline, thread life-cycle trace, and race condition vs `synchronized`
  - SQLite archive snapshot (one JDBC transaction with batch inserts), `DatabaseMetaData`, and parameterised queries
- **Desktop clients**: an AWT server control panel and a JavaFX archive viewer.
- **Runs anywhere**: MySQL is used when available, with an automatic fallback to embedded H2. Bootstrap, icons, fonts and Chart.js are bundled, so the app also works offline.

## How to run

Requirements: JDK 21. Maven is downloaded automatically by `./mvnw`. MySQL 8 is optional.

```bash
./mvnw spring-boot:run          # open http://localhost:8080
./mvnw test                     # JUnit 5 + Spring MockMvc tests
```

| Role | Username | Password |
|---|---|---|
| Admin | `admin` | `admin123` |
| Faculty | `faculty1` … `faculty4` | `faculty123` |
| Student | `student1` … `student9` | `student123` (`student3` shows the low-attendance flow) |

Desktop clients:

```bash
# AWT control panel (starts and stops the server, quick student lookup)
./mvnw -q compile exec:java -Dexec.mainClass=com.university.sms.desktop.awt.ControlPanelLauncher

# JavaFX archive viewer (take a snapshot first: Admin → Java Lab Console → JDBC Archive)
./mvnw -q compile exec:java -Dexec.mainClass=com.university.sms.desktop.fx.DesktopLauncher
```

To use MySQL, set `DB_URL`, `DB_USERNAME` and `DB_PASSWORD`, or edit `src/main/resources/application.properties`. `database_setup.sql` contains the schema.

## Project structure

```
src/main/java/com/university/sms
├── core/                  plain-Java modules for Units 1–5 (framework-independent)
│   ├── model/             Unit 1  – Person, StudentRecord, FacultyRecord, RollNumber, UniversityInfo
│   ├── risk/              NOVEL   – Academic Risk Radar, SGPA Goal Planner
│   ├── verify/            NOVEL   – HMAC tamper-proof marksheets
│   ├── grading/           Unit 2  – GradingPolicy hierarchy, Grade enum, GradeCalculator
│   ├── generic/           Unit 5  – Pair, Result, GenericRepository, InMemoryRepository, GenericUtils
│   ├── exception/         Unit 2  – SmsException hierarchy, MarksValidator
│   ├── io/                Unit 3  – CSV import/export, serialization backup, audit trail
│   ├── concurrent/        Unit 3  – thread pool, producer–consumer, monitors, life cycle, alert scheduler + inbox
│   ├── jdbc/              Unit 4  – SqliteArchiveDao
│   └── collections/       Unit 4  – AcademicAnalyticsEngine, SeatingPlanGenerator
├── desktop/awt/           Unit 3  – AWT control panel
├── desktop/fx/            Unit 5  – JavaFX archive viewer
├── config/  controller/  service/  dto/  entity/  repository/   Spring Boot web ERP
src/main/resources/templates   Thymeleaf pages for the three portals and the Java Lab Console
src/test/java                  CoreModulesTest (20 tests) + StudentManagementSystemTests
docs/                          report, screenshots, video script and demo recording
```

## Team

| Member | Roll number |
|---|---|
| Prit Patel | AP24110011578 |
| Anjay | AP24110010240 |
| Vivekanand Yadav | AP24110011109 |
| Arindam | AP24110010048 |
