package com.university.sms.config;

import com.university.sms.entity.*;
import com.university.sms.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final CourseRepository courseRepository;
    private final FacultyRepository facultyRepository;
    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;
    private final AttendanceRepository attendanceRepository;
    private final ExaminationRepository examinationRepository;
    private final MarkRepository markRepository;
    private final AssignmentRepository assignmentRepository;
    private final TimetableRepository timetableRepository;
    private final NoticeRepository noticeRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           DepartmentRepository departmentRepository,
                           CourseRepository courseRepository,
                           FacultyRepository facultyRepository,
                           StudentRepository studentRepository,
                           SubjectRepository subjectRepository,
                           AttendanceRepository attendanceRepository,
                           ExaminationRepository examinationRepository,
                           MarkRepository markRepository,
                           AssignmentRepository assignmentRepository,
                           TimetableRepository timetableRepository,
                           NoticeRepository noticeRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.courseRepository = courseRepository;
        this.facultyRepository = facultyRepository;
        this.studentRepository = studentRepository;
        this.subjectRepository = subjectRepository;
        this.attendanceRepository = attendanceRepository;
        this.examinationRepository = examinationRepository;
        this.markRepository = markRepository;
        this.assignmentRepository = assignmentRepository;
        this.timetableRepository = timetableRepository;
        this.noticeRepository = noticeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("University ERP database already contains sample records. Skipping initial seed.");
            return;
        }

        log.info("Seeding University ERP demo data (Admin, Faculty, Students, Departments, Subjects, Attendance, Marks, Timetable, Notices)...");

        // 1. Admin User
        User adminUser = userRepository.save(new User(
                "admin",
                passwordEncoder.encode("admin123"),
                "admin@apexuniversity.edu",
                "Dr. Vikramaditya Sen (Registrar & Admin)",
                Role.ROLE_ADMIN
        ));

        // 2. Departments
        Department cseDept = departmentRepository.save(new Department(
                "CSE",
                "Computer Science & Engineering",
                "Dr. Rajesh Verma",
                "Department of Computer Science, Software Engineering, AI & Data Science.",
                "B.Tech CSE, M.Tech CSE, Ph.D.",
                8
        ));
        Department eceDept = departmentRepository.save(new Department(
                "ECE",
                "Electronics & Communication Engineering",
                "Prof. Arvind Nair",
                "Department of VLSI, Embedded Systems, IoT and Signal Processing.",
                "B.Tech ECE, M.Tech VLSI",
                8
        ));
        Department mechDept = departmentRepository.save(new Department(
                "MECH",
                "Mechanical & Automation Engineering",
                "Dr. Sunita Deshmukh",
                "Department of Robotics, CAD/CAM, Thermal and Manufacturing Engineering.",
                "B.Tech Mechanical, M.Tech Robotics",
                8
        ));
        Department mbaDept = departmentRepository.save(new Department(
                "MBA",
                "School of Business & Management",
                "Dr. Nikhil Bannerjee",
                "Postgraduate Department of Finance, Operations, Marketing and Business Analytics.",
                "MBA, BBA",
                4
        ));

        // 3. Courses
        Course btechCse = courseRepository.save(new Course("BTECH-CSE", "B.Tech in Computer Science & Engineering", 4, 8, cseDept));
        Course btechEce = courseRepository.save(new Course("BTECH-ECE", "B.Tech in Electronics & Communication", 4, 8, eceDept));
        Course btechMech = courseRepository.save(new Course("BTECH-ME", "B.Tech in Mechanical Engineering", 4, 8, mechDept));
        Course mbaGen = courseRepository.save(new Course("MBA-FIN", "Master of Business Administration", 2, 4, mbaDept));

        // 4. Faculty Members
        Faculty fac1 = createFaculty("faculty1", "faculty123", "FAC-2026-001", "Rajesh", "Verma",
                "rajesh.verma@apexuniversity.edu", "+91-9810112233", "Professor & HOD",
                "Ph.D. (IIT Delhi)", "Distributed Systems, Java Enterprise & Databases",
                LocalDate.of(2016, 7, 15), cseDept);

        Faculty fac2 = createFaculty("faculty2", "faculty123", "FAC-2026-002", "Meera", "Krishnan",
                "meera.krishnan@apexuniversity.edu", "+91-9810223344", "Associate Professor",
                "Ph.D. (IISc Bangalore)", "Artificial Intelligence, Machine Learning & OS",
                LocalDate.of(2018, 8, 1), cseDept);

        Faculty fac3 = createFaculty("faculty3", "faculty123", "FAC-2026-003", "Arvind", "Nair",
                "arvind.nair@apexuniversity.edu", "+91-9810334455", "Professor & HOD",
                "Ph.D. (IIT Madras)", "Digital Signal Processing & VLSI Architecture",
                LocalDate.of(2015, 6, 10), eceDept);

        Faculty fac4 = createFaculty("faculty4", "faculty123", "FAC-2026-004", "Sunita", "Deshmukh",
                "sunita.deshmukh@apexuniversity.edu", "+91-9810445566", "Associate Professor",
                "Ph.D. (NIT Trichy)", "Thermodynamics, Robotics & Industrial Automation",
                LocalDate.of(2019, 1, 20), mechDept);

        // 5. Subjects
        Subject cs501 = createSubject("CS501", "Design & Analysis of Algorithms", 4, 5, "A", "THEORY", cseDept, btechCse, fac1);
        Subject cs502 = createSubject("CS502", "Relational Database Management Systems", 4, 5, "A", "THEORY", cseDept, btechCse, fac1);
        Subject cs503 = createSubject("CS503", "Operating Systems & Kernel Design", 4, 5, "A", "THEORY", cseDept, btechCse, fac2);
        Subject cs504 = createSubject("CS504", "Artificial Intelligence & Machine Learning", 4, 5, "A", "ELECTIVE", cseDept, btechCse, fac2);
        Subject cs505 = createSubject("CS505L", "Full-Stack Java & Spring Boot Lab", 2, 5, "A", "LAB", cseDept, btechCse, fac1);

        Subject ec501 = createSubject("EC501", "Digital Signal Processing", 4, 5, "A", "THEORY", eceDept, btechEce, fac3);
        Subject ec502 = createSubject("EC502", "CMOS VLSI Design", 4, 5, "A", "THEORY", eceDept, btechEce, fac3);

        Subject me501 = createSubject("ME501", "Applied Thermodynamics & Heat Transfer", 4, 5, "A", "THEORY", mechDept, btechMech, fac4);

        // 6. Students
        Student stu1 = createStudent("student1", "student123", "STU2026001", "Aarav", "Sharma",
                "aarav.sharma@student.apex.edu", "+91-9876500001", LocalDate.of(2005, 3, 14), "Male",
                "42 Residency Road, Bengaluru", "Suresh Sharma", "+91-9876599001", 5, "A", 2024, cseDept, btechCse);

        Student stu2 = createStudent("student2", "student123", "STU2026002", "Priya", "Patel",
                "priya.patel@student.apex.edu", "+91-9876500002", LocalDate.of(2005, 7, 22), "Female",
                "18SG Highway, Ahmedabad", "Mahesh Patel", "+91-9876599002", 5, "A", 2024, cseDept, btechCse);

        // Intentionally < 75% attendance student in CSE for alert demonstration!
        Student stu3 = createStudent("student3", "student123", "STU2026003", "Rohan", "Gupta",
                "rohan.gupta@student.apex.edu", "+91-9876500003", LocalDate.of(2005, 11, 5), "Male",
                "105 Connaught Place, New Delhi", "Rajeev Gupta", "+91-9876599003", 5, "A", 2024, cseDept, btechCse);

        Student stu4 = createStudent("student4", "student123", "STU2026004", "Ananya", "Iyer",
                "ananya.iyer@student.apex.edu", "+91-9876500004", LocalDate.of(2005, 1, 30), "Female",
                "77 Adyar Main Road, Chennai", "Venkatesh Iyer", "+91-9876599004", 5, "A", 2024, cseDept, btechCse);

        // Second < 75% attendance student in CSE
        Student stu5 = createStudent("student5", "student123", "STU2026005", "Vikram", "Singh",
                "vikram.singh@student.apex.edu", "+91-9876500005", LocalDate.of(2005, 5, 18), "Male",
                "29 Civil Lines, Jaipur", "Randhir Singh", "+91-9876599005", 5, "A", 2024, cseDept, btechCse);

        Student stu6 = createStudent("student6", "student123", "STU2026006", "Sneha", "Reddy",
                "sneha.reddy@student.apex.edu", "+91-9876500006", LocalDate.of(2005, 9, 12), "Female",
                "14 Jubilee Hills, Hyderabad", "Prakash Reddy", "+91-9876599006", 5, "A", 2024, cseDept, btechCse);

        Student stu7 = createStudent("student7", "student123", "STU2026007", "Karthik", "Nair",
                "karthik.nair@student.apex.edu", "+91-9876500007", LocalDate.of(2005, 4, 9), "Male",
                "8 Marine Drive, Kochi", "Gopalakrishnan Nair", "+91-9876599007", 5, "A", 2024, eceDept, btechEce);

        // < 75% attendance student in ECE
        Student stu8 = createStudent("student8", "student123", "STU2026008", "Neha", "Joshi",
                "neha.joshi@student.apex.edu", "+91-9876500008", LocalDate.of(2005, 12, 3), "Female",
                "55 FC Road, Pune", "Milind Joshi", "+91-9876599008", 5, "A", 2024, eceDept, btechEce);

        Student stu9 = createStudent("student9", "student123", "STU2026009", "Aditya", "Kulkarni",
                "aditya.kulkarni@student.apex.edu", "+91-9876500009", LocalDate.of(2005, 6, 25), "Male",
                "19 Shivaji Nagar, Nagpur", "Sanjay Kulkarni", "+91-9876599009", 5, "A", 2024, mechDept, btechMech);

        // 7. Seed 10 days of Attendance for each subject
        List<Subject> cseSubjects = List.of(cs501, cs502, cs503, cs504, cs505);
        seedStudentAttendance(stu1, cseSubjects, 10, 9, 0); // 90%
        seedStudentAttendance(stu2, cseSubjects, 10, 8, 1); // 80%
        seedStudentAttendance(stu3, cseSubjects, 10, 6, 1); // 60% -> LOW ATTENDANCE ALERT (<75%)
        seedStudentAttendance(stu4, cseSubjects, 10, 10, 0); // 100%
        seedStudentAttendance(stu5, cseSubjects, 10, 6, 0); // 60% -> LOW ATTENDANCE ALERT (<75%)
        seedStudentAttendance(stu6, cseSubjects, 10, 8, 0); // 80%

        List<Subject> eceSubjects = List.of(ec501, ec502);
        seedStudentAttendance(stu7, eceSubjects, 10, 9, 0); // 90%
        seedStudentAttendance(stu8, eceSubjects, 10, 6, 1); // 60% -> LOW ATTENDANCE ALERT (<75%)

        seedStudentAttendance(stu9, List.of(me501), 10, 9, 0); // 90%

        // 8. Seed Marks for Students
        seedStudentMarks(stu1, cs501, 28, 19, 19, 27, "Excellent algorithmic thinking", fac1);
        seedStudentMarks(stu1, cs502, 27, 18, 18, 26, "Strong SQL & normalization skills", fac1);
        seedStudentMarks(stu1, cs503, 26, 19, 18, 25, "Very good kernel concepts", fac2);
        seedStudentMarks(stu1, cs504, 29, 19, 19, 28, "Top project in ML", fac2);
        seedStudentMarks(stu1, cs505, 29, 20, 20, 28, "Outstanding Spring Boot project", fac1);

        seedStudentMarks(stu2, cs501, 25, 18, 17, 24, "Consistent performance", fac1);
        seedStudentMarks(stu2, cs502, 26, 18, 18, 25, "Well structured DB schema", fac1);
        seedStudentMarks(stu2, cs503, 24, 17, 16, 23, "Good grasp of concurrency", fac2);
        seedStudentMarks(stu2, cs504, 27, 18, 18, 25, "Great model evaluation", fac2);
        seedStudentMarks(stu2, cs505, 27, 19, 18, 26, "Clean MVC code", fac1);

        seedStudentMarks(stu3, cs501, 19, 14, 14, 18, "Needs regular class attendance", fac1);
        seedStudentMarks(stu3, cs502, 21, 15, 15, 19, "Improve indexing concepts", fac1);
        seedStudentMarks(stu3, cs503, 18, 13, 14, 17, "Low attendance impacted lab work", fac2);
        seedStudentMarks(stu3, cs504, 20, 15, 15, 18, "Fair attempt", fac2);
        seedStudentMarks(stu3, cs505, 22, 16, 15, 20, "Satisfactory lab work", fac1);

        seedStudentMarks(stu4, cs501, 29, 20, 19, 28, "Class topper", fac1);
        seedStudentMarks(stu4, cs502, 28, 19, 19, 27, "Exemplary work", fac1);
        seedStudentMarks(stu4, cs503, 28, 19, 19, 26, "Excellent", fac2);
        seedStudentMarks(stu4, cs504, 29, 20, 20, 28, "Research grade project", fac2);
        seedStudentMarks(stu4, cs505, 30, 20, 20, 29, "Perfect full-stack implementation", fac1);

        seedStudentMarks(stu5, cs501, 18, 13, 13, 16, "Warning: Improve attendance", fac1);
        seedStudentMarks(stu5, cs502, 19, 14, 13, 17, "Needs improvement", fac1);
        seedStudentMarks(stu5, cs503, 17, 12, 13, 16, "Attended remedial session", fac2);
        seedStudentMarks(stu5, cs504, 19, 14, 14, 17, "Satisfactory", fac2);
        seedStudentMarks(stu5, cs505, 20, 15, 14, 18, "Completed lab modules", fac1);

        seedStudentMarks(stu6, cs501, 24, 17, 16, 22, "Good analytical skills", fac1);
        seedStudentMarks(stu6, cs502, 23, 17, 17, 22, "Good", fac1);
        seedStudentMarks(stu6, cs503, 25, 16, 16, 21, "Active in class", fac2);
        seedStudentMarks(stu6, cs504, 24, 18, 17, 23, "Well done", fac2);
        seedStudentMarks(stu6, cs505, 26, 18, 18, 24, "Good UI & DB integration", fac1);

        seedStudentMarks(stu7, ec501, 26, 18, 18, 25, "Strong DSP fundamentals", fac3);
        seedStudentMarks(stu7, ec502, 25, 17, 18, 24, "Good Verilog simulation", fac3);

        seedStudentMarks(stu8, ec501, 19, 14, 14, 18, "Attendance below 75% threshold", fac3);
        seedStudentMarks(stu8, ec502, 20, 14, 15, 17, "Needs regular lab attendance", fac3);

        seedStudentMarks(stu9, me501, 25, 18, 17, 24, "Good thermal calculations", fac4);

        // 9. Timetable Slots
        timetableRepository.save(new Timetable("MONDAY", LocalTime.of(9, 0), LocalTime.of(10, 0), cs501, cseDept, 5, "A", "LH-301", "LECTURE"));
        timetableRepository.save(new Timetable("MONDAY", LocalTime.of(10, 15), LocalTime.of(11, 15), cs502, cseDept, 5, "A", "LH-301", "LECTURE"));
        timetableRepository.save(new Timetable("MONDAY", LocalTime.of(11, 30), LocalTime.of(12, 30), cs503, cseDept, 5, "A", "LH-302", "LECTURE"));
        timetableRepository.save(new Timetable("TUESDAY", LocalTime.of(9, 0), LocalTime.of(10, 0), cs504, cseDept, 5, "A", "LH-301", "LECTURE"));
        timetableRepository.save(new Timetable("TUESDAY", LocalTime.of(10, 15), LocalTime.of(12, 15), cs505, cseDept, 5, "A", "COMP-LAB-2", "LAB"));
        timetableRepository.save(new Timetable("WEDNESDAY", LocalTime.of(9, 0), LocalTime.of(10, 0), cs501, cseDept, 5, "A", "LH-301", "LECTURE"));
        timetableRepository.save(new Timetable("WEDNESDAY", LocalTime.of(10, 15), LocalTime.of(11, 15), cs503, cseDept, 5, "A", "LH-302", "LECTURE"));
        timetableRepository.save(new Timetable("THURSDAY", LocalTime.of(9, 0), LocalTime.of(10, 0), cs502, cseDept, 5, "A", "LH-301", "LECTURE"));
        timetableRepository.save(new Timetable("THURSDAY", LocalTime.of(10, 15), LocalTime.of(11, 15), cs504, cseDept, 5, "A", "AI-LAB-1", "TUTORIAL"));
        timetableRepository.save(new Timetable("FRIDAY", LocalTime.of(9, 0), LocalTime.of(11, 0), cs505, cseDept, 5, "A", "COMP-LAB-2", "LAB"));
        timetableRepository.save(new Timetable("FRIDAY", LocalTime.of(11, 15), LocalTime.of(12, 15), ec501, eceDept, 5, "A", "ECE-201", "LECTURE"));
        timetableRepository.save(new Timetable("SATURDAY", LocalTime.of(10, 0), LocalTime.of(11, 0), me501, mechDept, 5, "A", "ME-104", "LECTURE"));

        // 10. Examinations
        examinationRepository.save(new Examination("End-Semester Theory Exam - Fall 2026", "END_SEM", 5, cs501,
                LocalDate.now().plusDays(10), LocalTime.of(9, 30), LocalTime.of(12, 30), "EXAM-HALL-A", 100, "UPCOMING"));
        examinationRepository.save(new Examination("End-Semester Theory Exam - Fall 2026", "END_SEM", 5, cs502,
                LocalDate.now().plusDays(12), LocalTime.of(9, 30), LocalTime.of(12, 30), "EXAM-HALL-A", 100, "UPCOMING"));
        examinationRepository.save(new Examination("End-Semester Theory Exam - Fall 2026", "END_SEM", 5, cs503,
                LocalDate.now().plusDays(14), LocalTime.of(9, 30), LocalTime.of(12, 30), "EXAM-HALL-B", 100, "UPCOMING"));
        examinationRepository.save(new Examination("Practical & Viva Voce Assessment", "PRACTICAL", 5, cs505,
                LocalDate.now().plusDays(5), LocalTime.of(10, 0), LocalTime.of(13, 0), "COMP-LAB-2", 50, "UPCOMING"));
        examinationRepository.save(new Examination("Mid-Semester Internal Assessment II", "INTERNAL", 5, ec501,
                LocalDate.now().minusDays(15), LocalTime.of(10, 0), LocalTime.of(11, 30), "ECE-201", 30, "COMPLETED"));

        // 11. Assignments
        assignmentRepository.save(new Assignment(
                "Dynamic Programming & Graph Shortest Path Implementation",
                "Implement Dijkstra's and Bellman-Ford algorithms in Java 21 and compare time complexity on sparse vs dense graphs.",
                cs501, fac1, LocalDate.now().plusDays(6), 20, "https://docs.oracle.com/en/java/javase/21/"
        ));
        assignmentRepository.save(new Assignment(
                "University ERP Normalized MySQL Schema & Indexing Report",
                "Design 3NF/BCNF relational tables with foreign keys, composite indexes, and query execution plan analysis.",
                cs502, fac1, LocalDate.now().plusDays(9), 20, "https://dev.mysql.com/doc/"
        ));
        assignmentRepository.save(new Assignment(
                "Process Synchronization & Deadlock Avoidance Simulator",
                "Implement Banker's Algorithm for multi-resource deadlock avoidance with thread-safe Java locks.",
                cs503, fac2, LocalDate.now().plusDays(11), 20, "https://pages.cs.wisc.edu/~remzi/OSTEP/"
        ));
        assignmentRepository.save(new Assignment(
                "Neural Network Classification on Student Performance Dataset",
                "Build and evaluate a multi-layer perceptron classifier predicting student grade outcomes.",
                cs504, fac2, LocalDate.now().plusDays(14), 20, "https://scikit-learn.org/"
        ));

        // 12. Notices
        noticeRepository.save(new Notice(
                "Mandatory 75% Attendance Policy for End-Semester Examinations",
                "All students are hereby notified that a minimum of 75% attendance is strictly mandatory to appear in the upcoming End-Semester Examinations. Students flagged with Low Attendance (<75%) on their ERP dashboard must meet their respective HOD and Faculty Advisor immediately.",
                "URGENT", "ALL", null, adminUser, true
        ));
        noticeRepository.save(new Notice(
                "End-Semester Examination Schedule & Admit Card Release",
                "The End-Semester Theory and Practical Examination timetable for Semester 5 has been published under the Examination Schedule module. Students can download their provisional marksheets and attendance certificates directly from the Student Portal.",
                "EXAMINATION", "STUDENTS", null, adminUser, true
        ));
        noticeRepository.save(new Notice(
                "Faculty Deadline: Internal Assessment & Practical Marks Upload",
                "All faculty members are requested to finalize and verify Internal Exam (30), Assignment (20), and Practical (20) marks on the Faculty Portal before Friday 5:00 PM.",
                "ACADEMIC", "FACULTY", null, adminUser, false
        ));
        noticeRepository.save(new Notice(
                "Annual National Smart Campus Hackathon 2026 Registration Open",
                "The Department of Computer Science & Engineering invites interdisciplinary student teams to register for the 36-hour Smart India Campus Hackathon.",
                "EVENT", "ALL", cseDept, adminUser, false
        ));

        log.info("University ERP demo data initialization complete!");
    }

    private Faculty createFaculty(String username, String rawPwd, String empId, String first, String last,
                                  String email, String phone, String desig, String qual, String spec,
                                  LocalDate joinDate, Department dept) {
        User u = userRepository.save(new User(username, passwordEncoder.encode(rawPwd), email, "Dr. " + first + " " + last, Role.ROLE_FACULTY));
        Faculty f = new Faculty();
        f.setEmployeeId(empId);
        f.setFirstName(first);
        f.setLastName(last);
        f.setEmail(email);
        f.setPhone(phone);
        f.setDesignation(desig);
        f.setQualification(qual);
        f.setSpecialization(spec);
        f.setJoiningDate(joinDate);
        f.setDepartment(dept);
        f.setUser(u);
        return facultyRepository.save(f);
    }

    private Subject createSubject(String code, String name, int credits, int sem, String sec,
                                  String type, Department dept, Course course, Faculty faculty) {
        Subject s = new Subject();
        s.setCode(code);
        s.setName(name);
        s.setCredits(credits);
        s.setSemester(sem);
        s.setSection(sec);
        s.setSubjectType(type);
        s.setDepartment(dept);
        s.setCourse(course);
        s.setAssignedFaculty(faculty);
        return subjectRepository.save(s);
    }

    private Student createStudent(String username, String rawPwd, String rollNo, String first, String last,
                                  String email, String phone, LocalDate dob, String gender, String address,
                                  String guardian, String guardianPhone, int sem, String sec, int year,
                                  Department dept, Course course) {
        User u = userRepository.save(new User(username, passwordEncoder.encode(rawPwd), email, first + " " + last, Role.ROLE_STUDENT));
        Student s = new Student();
        s.setRollNumber(rollNo);
        s.setFirstName(first);
        s.setLastName(last);
        s.setEmail(email);
        s.setPhone(phone);
        s.setDateOfBirth(dob);
        s.setGender(gender);
        s.setAddress(address);
        s.setGuardianName(guardian);
        s.setGuardianPhone(guardianPhone);
        s.setSemester(sem);
        s.setSection(sec);
        s.setEnrollmentYear(year);
        s.setDepartment(dept);
        s.setCourse(course);
        s.setUser(u);
        return studentRepository.save(s);
    }

    private void seedStudentAttendance(Student student, List<Subject> subjects, int totalDays, int presentDays, int leaveDays) {
        for (Subject subject : subjects) {
            for (int d = 1; d <= totalDays; d++) {
                LocalDate date = LocalDate.now().minusDays(totalDays - d + 1);
                AttendanceStatus status;
                String remark = "";
                if (d <= presentDays) {
                    status = AttendanceStatus.PRESENT;
                } else if (d <= presentDays + leaveDays) {
                    status = AttendanceStatus.LEAVE;
                    remark = "Approved Medical / Academic Leave";
                } else {
                    status = AttendanceStatus.ABSENT;
                    remark = "Unexcused absence";
                }
                attendanceRepository.save(new Attendance(student, subject, date, status, remark, subject.getAssignedFaculty()));
            }
        }
    }

    private void seedStudentMarks(Student student, Subject subject, double internal, double assignment,
                                  double practical, double external, String remarks, Faculty faculty) {
        Mark m = new Mark();
        m.setStudent(student);
        m.setSubject(subject);
        m.setSemester(subject.getSemester());
        m.setInternalMarks(internal);
        m.setAssignmentMarks(assignment);
        m.setPracticalMarks(practical);
        m.setExternalMarks(external);
        m.setRemarks(remarks);
        m.setEnteredBy(faculty);
        m.calculateGradeAndTotal();
        markRepository.save(m);
    }
}

