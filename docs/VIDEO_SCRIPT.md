# Video Presentation Script (≈ 10–12 minutes)

**Slides:** `docs/Apex_ERP_Presentation.pptx` has 21 slides: 3 intro, 4 per member (a section slide + 3 content slides), and 2 closing. **The speaker notes under each slide are the exact lines to say.** Each member records their own section; the slides show their name in the top-right corner.

**Setup before recording:** run `./mvnw spring-boot:run`. Open http://localhost:8080 in a browser. Record with QuickTime (File → New Screen Recording) or OBS, with the microphone on. Each member speaks for their own part, about 2.5 minutes each. The silent file `SMS_demo_walkthrough.mp4` shows the same click path if you want a reference.

---

## 0. Opening – Prit (≈ 1 min)
**Screen:** login page.
> "Good morning sir. We are Prit Patel, Anjay, Vivekanand and Arindam. Our project is the **Apex University ERP**, a Student Management System built in Java 21 with Spring Boot. It has three secure portals: Admin, Faculty and Student. We divided the work by the Java syllabus: each of us implemented two or three units as real features of this system."

## 1. Prit Patel – Units 1, 2 (polymorphism), 5 (generics) (≈ 2.5 min)
1. Log in as **admin / admin123**. Show the dashboard (KPIs, charts, low-attendance watchlist).
   > "Login uses Spring Security with BCrypt passwords. Each role is sent to its own portal."
2. Open **Java Lab Console → Overview**.
   > "This table maps every unit to its owner, its classes and the feature it powers."
3. Open the **Polymorphic Grading** tab. Switch Absolute → Relative → Moderation.
   > "`GradingPolicy` is a sealed interface. `GradingPolicy.of()` chooses the object at run time, and `assignAll()` calls the subclass's `assign()`. That is dynamic method dispatch. `AbstractGradingPolicy` uses the Template Method pattern. The relative policy overrides the `prepare()` hook to compute the mean and standard deviation. `GradeCalculator` shows overloading."
4. Show `core/model/Person.java` in the IDE.
   > "Unit 1: an abstract class, constructor chaining with `this(...)`, a static counter, a final method. `StudentRecord` and `FacultyRecord` extend it. `RollNumber` is an immutable final class. `UniversityInfo` shows static final constants and a static block."
5. Show `core/generic/GenericRepository.java`.
   > "Unit 5 generics: a bounded type `T extends Identifiable<ID>`, wildcards `? extends` and `? super`, and generic `Pair` and `Result` classes."

## 2. Anjay – Unit 2 (exceptions), Unit 3 (I/O streams) (≈ 2.5 min)
1. Open the **I/O & Exceptions** tab. Click **Bulk admission template** and edit it to contain one bad email and one bad semester. Import it.
   > "The CSV is read with `BufferedReader` over `InputStreamReader`. Every bad line throws a checked `DataImportException` with the line number. I catch it per line, so the good rows are still admitted and the bad ones are reported."
2. Click **Export performance CSV**, then **Download serialized backup**, then upload that file to **Verify**.
   > "The backup chains byte streams: Buffered, then GZIP, then a `DataOutputStream` header and an `ObjectOutputStream` body. Restoring checks the magic number, so a wrong file throws `SmsException`."
3. Show the **audit trail** box.
   > "`FileOutputStream` in append mode, read back with `BufferedReader`."
4. Log out and log in as **student3 / student123**. Click **Exam Hall Ticket**.
   > "This student has 60% attendance, so the controller throws `AttendanceShortageException`. `GlobalExceptionHandler`, a `@ControllerAdvice`, catches it and shows this page with the number of classes needed. student1 gets a printable hall ticket."

## 3. Vivekanand – Unit 3 (multithreading, AWT) (≈ 2.5 min)
1. Log in as **faculty1 / faculty123**. Mark attendance, then enter marks. Type **35** for internal and save.
   > "The browser blocks it. If the request is forced, the server throws `InvalidMarksException` from `MarksValidator`, and the message is shown here."
2. Log in as admin. Open **Java Lab → Multithreading** and click **Run all four thread demos**.
   > "The report cards are generated sequentially and then with a fixed thread pool of four `Callable` tasks. You can see the worker thread names and about a 3.6× speed-up. The alert pipeline is a producer–consumer: the producer is a `Runnable`, the consumer extends `Thread`, and they share a buffer that uses `wait()` and `notifyAll()`. The life-cycle trace shows NEW, RUNNABLE, TIMED_WAITING, WAITING, BLOCKED and TERMINATED. The race demo shows `count++` losing updates, while the synchronized version gives exactly 400,000."
3. Point at the counters at the top.
   > "Each Tomcat request runs on its own thread. My interceptor updates these counters with `synchronized`, `AtomicLong` and `ConcurrentHashMap`."
4. Run the AWT panel (`exec:java … ControlPanelLauncher`), press **Start Server**, and type a roll number + Enter.
   > "AWT delegation event model: `ActionListener`, `ItemListener`, `KeyAdapter` and `WindowAdapter`. The server starts on a background thread so the window never freezes."

## 4. Arindam – Unit 4 (JDBC, Collections), Unit 5 (JavaFX) (≈ 2.5 min)
1. Open the **Collections Analytics** tab. Search `STU2026004`.
   > "A `PriorityQueue` gives the top 5, a `TreeMap` keeps departments sorted, an `EnumMap` counts grades, a `TreeSet` holds the shortage list, a `LinkedList` is the promotion queue, `Iterator.remove()` filters safely, and `Collections.binarySearch` finds the roll number."
2. Open the **JDBC Archive** tab and click **Take snapshot**. Search a name.
   > "Plain JDBC on SQLite: `DriverManager`, `PreparedStatement` with batch inserts inside one transaction, and a rollback if anything fails. `ResultSet` mapping, `DatabaseMetaData`, and a SQL `GROUP BY` for the averages."
3. Run the JavaFX viewer (`exec:java … DesktopLauncher`). Type in the search box and change the department.
   > "JavaFX reads the same SQLite file. It has a `MenuBar`, a `TableView` with a `FilteredList` for live search, a `BarChart`, and a `Task` that loads the data on a background thread."
4. Briefly show `entity/` and `repository/`.
   > "I also built the database layer: JPA entities, Spring Data repositories, and the automatic MySQL-to-H2 fallback."

## Novel features to demo live (each member, about 45 s inside their part)
- **Prit:** Admin → Smart Campus → *Academic Risk Radar*. Then student1 → *Goal Planner & Risk*: set the target SGPA to 9.5 and press Recalculate.
- **Anjay:** student1 → *Exam Results*: copy the verification code. Open `/verify` (logged out), paste it → VALID. Change one character → MISMATCH.
- **Vivekanand:** Admin → Smart Campus → *Automatic Alert Engine*: press **Run scan now**. Then student3 → *Notifications* shows the attendance alert.
- **Arindam:** Admin → Smart Campus → *Exam Seating Plan*: press **Generate** (0 same-department neighbours). Then student1 → *Exam Hall Ticket* shows the seat.

## 5. Closing – Prit (≈ 30 s)
> "To summarise: a working three-portal ERP in which every unit of the syllabus has a real job, 15 passing JUnit tests plus integration tests, and a complete report with the code of every class. Thank you, sir."
