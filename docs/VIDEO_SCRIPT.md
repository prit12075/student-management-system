# Video Presentation Script

Slides: `docs/Apex_ERP_Presentation.pptx` (21 slides). The same script is in each slide's **Speaker Notes**, so PowerPoint shows it while you record (Mac: Slide Show → Record Slide Show; Windows: Record → From Current Slide). A printable copy is in `docs/Presentation_Script.pdf`.

| Slide | Title | Speaker |
|---|---|---|
| 1 | Apex University ERP | Prit Patel |
| 2 | The Problem and Our Novelty | Prit Patel |
| 3 | System Overview and Team | Prit Patel |
| 4 | Prit Patel | Prit Patel |
| 5 | OOP Core Model and Generics | Prit Patel |
| 6 | Polymorphic Grading | Prit Patel |
| 7 | Novel Feature: Academic Risk Radar | Prit Patel |
| 8 | Anjay | Anjay |
| 9 | Custom Exception Handling | Anjay |
| 10 | I/O Streams in Action | Anjay |
| 11 | Novel Feature: Tamper-proof Marksheets | Anjay |
| 12 | Vivekanand Yadav | Vivekanand Yadav |
| 13 | Multithreading Performance | Vivekanand Yadav |
| 14 | Producer–Consumer and AWT | Vivekanand Yadav |
| 15 | Novel Feature: Automatic Alert Engine | Vivekanand Yadav |
| 16 | Arindam | Arindam |
| 17 | JDBC with SQLite | Arindam |
| 18 | Collections and JavaFX | Arindam |
| 19 | Novel Feature: Smart Exam Seating | Arindam |
| 20 | Results and Testing | Prit Patel |
| 21 | Thank You | All members |

---

## Slide 1 — Apex University ERP
**Speaker:** Prit Patel

Good morning, sir. We are Prit Patel, Anjay, Vivekanand Yadav and Arindam, from B.Tech CSE, Semester Five. Our project for CSE 309, Advanced Java Programming, is the Apex University ERP: a smart Student Management System built in Java 21 with Spring Boot. In this video, each of us will present the Java units we implemented and the novel feature we designed. I will start with the overview.

## Slide 2 — The Problem and Our Novelty
**Speaker:** Prit Patel

Most college management systems only store records. They notice a weak student only after the results come out. A printed marksheet can be edited, and nobody can easily check it. Students discover an attendance shortage only when the hall ticket is refused. And exam seating is still planned by hand. Our project adds a Smart Campus layer that fixes these four gaps, one feature per team member: a Risk Radar, verified marksheets, an automatic alert engine, and smart exam seating.

## Slide 3 — System Overview and Team
**Speaker:** Prit Patel

The system has three portals: Admin, Faculty and Student. All of them are secured with Spring Security and BCrypt passwords. The admin manages students, faculty, subjects, timetables and notices. Faculty mark attendance and enter marks, and the system calculates grades and SGPA automatically. Students see their results, attendance, hall ticket and PDF marksheet. We divided the five syllabus units equally among the four of us. In total, the project has 110 Java classes and 20 passing JUnit tests.

## Slide 4 — Prit Patel
**Speaker:** Prit Patel

My part covers Unit 1, object-oriented programming; Unit 2, polymorphism; and Unit 5, generics. I also built the overall architecture, Spring Security, the admin portal, and my novel feature, the Academic Risk Radar with an SGPA Goal Planner.

## Slide 5 — OOP Core Model and Generics
**Speaker:** Prit Patel

This is the core model. Person is an abstract class that implements three interfaces. It shows constructor chaining with this, a static object counter, a final method, and an abstract method, getRoleTitle. StudentRecord and FacultyRecord extend Person and override that method, which is inheritance with method overriding. For Unit 5, GenericRepository uses a bounded type parameter, T extends Identifiable, along with upper- and lower-bounded wildcards. So one repository works safely for any record type, with no casting.

## Slide 6 — Polymorphic Grading
**Speaker:** Prit Patel

Grading in our ERP is polymorphic. The admin chooses a grading policy at run time: absolute, relative bell-curve, or moderation with grace marks. GradingPolicy is a sealed interface, and the same method call, assignAll, runs the version of whichever subclass was chosen. That is dynamic method dispatch. The abstract class uses the Template Method pattern, and GradeCalculator shows compile-time polymorphism through overloaded methods. The Grade enum now grades every real mark in the system.

## Slide 7 — Novel Feature: Academic Risk Radar
**Speaker:** Prit Patel

My novel feature is an early-warning system. Each risk factor, such as attendance, marks, backlogs and borderline subjects, is a lambda that implements a functional interface. The assessor combines the factors with weights into a score from 0 to 100, explains the reasons, and ranks the students from highest risk down. Students also get a what-if SGPA planner: they enter a target SGPA and see the percentage they need in every subject.

## Slide 8 — Anjay
**Speaker:** Anjay

My part covers Unit 2, exception handling, and Unit 3, input and output streams. I also built the student portal, the PDF reports, and my novel feature: tamper-proof marksheets that anyone can verify online.

## Slide 9 — Custom Exception Handling
**Speaker:** Anjay

I created a checked exception hierarchy with SmsException as the root, and every exception carries an error code. When a faculty member enters thirty-five marks out of thirty, MarksValidator throws InvalidMarksException. When a student below 75 percent attendance asks for a hall ticket, the controller throws AttendanceShortageException. The exception propagates up to my global handler, which is a ControllerAdvice, and that handler shows this friendly page with the number of classes still needed. I also used throws, multi-catch, exception chaining and try-with-resources.

## Slide 10 — I/O Streams in Action
**Speaker:** Anjay

Streams power three features. First, bulk admission: the admin uploads a CSV file, and I read it with a BufferedReader over an InputStreamReader. Every bad line is reported with its line number, while the good rows are still admitted. Second, backups: I chain a buffered stream, a GZIP stream, a DataOutputStream for the header, and an ObjectOutputStream that serializes all the student objects. Third, every important action is appended to an audit file using a FileOutputStream in append mode.

## Slide 11 — Novel Feature: Tamper-proof Marksheets
**Speaker:** Anjay

My novel feature makes marksheets impossible to forge. The marks and SGPA are written as canonical bytes with a DataOutputStream and sealed with HMAC-SHA256 using a secret key. The resulting code is printed on every PDF marksheet. An employer opens the public verify page, enters the roll number and the code, and sees VALID along with the real SGPA. If even one mark or character is changed, the code no longer matches, and the page shows MISMATCH.

## Slide 12 — Vivekanand Yadav
**Speaker:** Vivekanand Yadav

My part covers Unit 3: multithreading, and event handling with AWT. I also built the faculty portal, the core business service, and my novel feature, the automatic alert engine with a notification inbox for every student.

## Slide 13 — Multithreading Performance
**Speaker:** Vivekanand Yadav

Here, report cards are generated twice. The first time uses one thread. The second time uses a fixed thread pool of four Callable tasks through an ExecutorService. The parallel version is about 3.6 times faster, and you can see the worker thread names. The race-condition demo shows that a plain count plus-plus loses updates when eight threads run together, while my synchronized method always gives exactly four lakh. I also trace all six thread states, from NEW to TERMINATED.

## Slide 14 — Producer–Consumer and AWT
**Speaker:** Vivekanand Yadav

For inter-thread communication, I wrote a bounded buffer with synchronized, wait and notifyAll. The producer thread waits when the buffer is full, and the consumer thread waits when it is empty. The AWT control panel uses the delegation event model with ActionListener, ItemListener, KeyAdapter and WindowAdapter. It starts the Spring Boot server on a background thread, so the window never freezes, and it updates the screen safely through the event dispatch thread.

## Slide 15 — Novel Feature: Automatic Alert Engine
**Speaker:** Vivekanand Yadav

My novel feature is an alert engine. A ScheduledExecutorService runs on a low-priority background thread and scans attendance automatically. When a student drops below 75 percent, they receive an alert in their inbox, and they get another message when they recover. There are no duplicate alerts. Published marks and exam seat allotments also send notifications. The inbox is built only from concurrent collections, ConcurrentHashMap and CopyOnWriteArrayList, so many threads can use it safely at the same time.

## Slide 16 — Arindam
**Speaker:** Arindam

My part covers Unit 4, JDBC and the Collections framework, and Unit 5, JavaFX. I also built the database layer, and my novel feature: an anti-malpractice exam seating plan generator.

## Slide 17 — JDBC with SQLite
**Speaker:** Arindam

The ERP uses Hibernate for its main database. My archive module uses plain JDBC so that every step is visible. It gets a connection from the DriverManager, creates the tables with a Statement, and inserts all students with a PreparedStatement in batch mode, inside a single transaction. If anything fails, it rolls back. It then reads data back through a ResultSet and uses DatabaseMetaData to show database details. PreparedStatement also protects us from SQL injection.

## Slide 18 — Collections and JavaFX
**Speaker:** Arindam

The analytics engine uses the right collection for each job. A PriorityQueue picks the toppers, and a TreeMap keeps the departments in sorted order. An EnumMap counts the grades, a TreeSet stores the shortage list, and Iterator.remove filters records safely. The JavaFX desktop viewer opens the same SQLite file. It has a menu bar, a table view with live search, a department filter and a bar chart. The data loads on a background Task, so the window stays responsive.

## Slide 19 — Novel Feature: Smart Exam Seating
**Speaker:** Arindam

My novel feature is exam seating that prevents malpractice. Students of the same department write the same paper, so they must not sit next to each other. The generator first distributes students across the halls. It then keeps department queues in a TreeMap of ArrayDeques, and for every seat it picks a department different from the neighbours on the left and in front. The result has zero conflicts. The plan is saved through JDBC and printed on each student's hall ticket.

## Slide 20 — Results and Testing
**Speaker:** Prit Patel

To summarise the results: 110 Java classes, all 20 core JUnit tests passing along with integration tests, four novel features, and all five units of the syllabus used in real features. We tested every page of the three portals end to end, including the public verification page. In future, the alerts could also go out by SMS and e-mail, the system could have a REST API for a mobile app, and it could be deployed with Docker.

## Slide 21 — Thank You
**Speaker:** All members

Thank you, sir, for watching our presentation. The complete source code, the project report and the screenshots are available in our GitHub repository.
