# Individual Contributions

The work was divided **equally by the Java concepts of Units 1–5**. Each member owns two or three syllabus topics. Each member implemented them as a plain-Java module in `core/` or `desktop/` and connected them to a real feature of the web ERP. Each member also owns one part of the Spring Boot application.

| Member | Roll no. | Syllabus topics | Web / desktop features | Novel feature |
|---|---|---|---|---|
| **Prit Patel** | AP24110011578 | Unit 1 OOP • Unit 2 Polymorphism • Unit 5 Generics | Architecture & integration, Spring Security & login, Admin portal, Java Lab Console | Academic Risk Radar + SGPA Goal Planner |
| **Anjay** | AP24110010240 | Unit 2 Exception handling • Unit 3 I/O streams | Student portal, exam hall ticket, PDF reports, global exception handling | Tamper-proof marksheets + public /verify |
| **Vivekanand Yadav** | AP24110011109 | Unit 3 Multithreading • Unit 3 AWT event handling | Faculty portal, ERP business service, live request monitor, AWT control panel | Automatic Alert Engine + notification inbox |
| **Arindam** | AP24110010048 | Unit 4 JDBC • Unit 4 Collections • Unit 5 JavaFX | Database layer (entities, repositories, seed data, DB fallback), SQLite archive, JavaFX viewer | Anti-malpractice exam seating plan |

## Prit Patel – Unit 1, Unit 2 (polymorphism), Unit 5 (generics)
- `core/model`: `Person` (abstract class, constructor chaining, static counter, final method), `StudentRecord` and `FacultyRecord` (inheritance, overriding, overloading, varargs), `RollNumber` (immutable value object), `UniversityInfo` (static/final, static block, nested class), `Reportable` and `Identifiable` interfaces.
- `core/grading`: sealed `GradingPolicy` with `AbstractGradingPolicy` (Template Method) and the Absolute, Relative and Moderation policies (runtime polymorphism), overloaded `GradeCalculator`, and the `Grade` enum, which now grades every mark in `Mark.java`.
- `core/generic`: `Pair<A,B>`, `Result<T>`, `GenericRepository<T extends Identifiable<ID>, ID>`, `InMemoryRepository`, and `GenericUtils` (bounded types and wildcards).
- Integration: `JavaLabService`, `JavaLabController`, `JavaLabConfig`, `SecurityConfig`, `AdminController`, `AuthController`, `CustomUserDetailsService`, `CustomAuthenticationSuccessHandler`, and the `java-lab.html` page.

- **Novel:** `core/risk` – `RiskFactor` (functional interface, lambda factors), `AcademicRiskAssessor`, `RiskLevel`, `SgpaGoalPlanner`; `SmartCampusService`, `SmartCampusController`, `SmartCampusConfig`, and the `admin/smart.html` and `student/planner.html` pages.

## Anjay – Unit 2 (exceptions), Unit 3 (I/O streams)
- `core/exception`: checked `SmsException` hierarchy (`InvalidMarksException`, `DataImportException`, `AttendanceShortageException`), unchecked `RecordNotFoundException`, and `MarksValidator` (throw/throws).
- `GlobalExceptionHandler` (`@ControllerAdvice`): the hall-ticket refusal page, record-not-found and upload-size errors. Exception chaining in `UniversityErpService.saveOrUpdateMark`.
- `core/io`: `CsvStudentImporter` (BufferedReader, per-line exception handling), `CsvStudentExporter` (Writer/OutputStream/Path overloads), `SerializationBackupService` (Buffered, GZIP, Data and Object streams), `AuditTrailLogger` (FileOutputStream append and tail), `StreamUtils`.
- `StudentController` (including `/student/hall-ticket`), `PdfReportService`, and the student pages, hall ticket and error pages.

- **Novel:** `core/verify/MarksheetFingerprint` (HMAC-SHA256 over DataOutputStream bytes, constant-time compare), `VerificationResult`, `VerificationController` + the public `verify.html`, the code printed on the PDF and the marks page.

## Vivekanand Yadav – Unit 3 (multithreading, AWT)
- `core/concurrent`: `ConcurrentReportEngine` (ExecutorService, Callable, Future, custom ThreadFactory), `AlertQueue<T>` (synchronized + wait/notifyAll), `AttendanceAlertMonitor` (producer from a Runnable, consumer that extends Thread, priorities, join), `PortalActivityMonitor` (synchronized, AtomicLong, ConcurrentHashMap/LongAdder, race-condition demo), `ThreadLifecycleTracer` (all six thread states).
- `ActivityTrackingInterceptor`: ThreadLocal per Tomcat worker thread.
- `desktop/awt/ControlPanelLauncher`: Frame, Button, Choice, Checkbox, TextArea and TextField with ActionListener, ItemListener, KeyAdapter and WindowAdapter. It starts Spring Boot on a background thread.
- `FacultyController`, `UniversityErpService`, the DTOs, and the faculty pages.

- **Novel:** `core/concurrent/AlertScheduler` (ScheduledExecutorService, daemon, MIN_PRIORITY, state-change de-duplication), `NotificationCenter` (ConcurrentHashMap + CopyOnWriteArrayList + AtomicLong), `Notification`, the student inbox, and notifications on marks publishing.

## Arindam – Unit 4 (JDBC, Collections), Unit 5 (JavaFX)
- `core/jdbc/SqliteArchiveDao`: DriverManager, Statement DDL, PreparedStatement, batch insert, commit/rollback, ResultSet, ResultSetMetaData, DatabaseMetaData, and GROUP BY.
- `core/collections/AcademicAnalyticsEngine`: ArrayList + Comparator chain, PriorityQueue, TreeMap, EnumMap, TreeSet/HashSet, LinkedList queue, Iterator.remove(), and the Collections utilities.
- `desktop/fx/SmsDesktopApp`: JavaFX life cycle, layouts, MenuBar, TableView, FilteredList/SortedList, ComboBox, BarChart, and Task.
- Database layer: all JPA `entity` classes, Spring Data `repository` interfaces, `DataInitializer`, `DatabaseConnectionConfig` (MySQL → H2 fallback), and `database_setup.sql`.

- **Novel:** `core/collections/SeatingPlanGenerator` (round-robin hall dealing, TreeMap of ArrayDeque, greedy Comparator choice, zero same-department neighbours), `Seat`, `SeatingPlan`, `SqliteArchiveDao.saveSeatingPlan/findSeat`, and the seat on the hall ticket.

## Shared
- `CoreModulesTest`: each member wrote the tests for their own section (20 tests, all passing).
- Report, screenshots and video presentation: each member presents their own units (see `docs/VIDEO_SCRIPT.md`).
