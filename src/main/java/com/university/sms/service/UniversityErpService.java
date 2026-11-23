package com.university.sms.service;

import com.university.sms.core.exception.InvalidMarksException;
import com.university.sms.core.exception.MarksValidator;
import com.university.sms.dto.AttendanceSummaryDto;
import com.university.sms.dto.StudentPerformanceDto;
import com.university.sms.entity.*;
import com.university.sms.repository.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class UniversityErpService {

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

    public UniversityErpService(UserRepository userRepository,
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

    // ==================== USER & AUTH HELPERS ====================

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
    }

    public Faculty getFacultyByUsername(String username) {
        return facultyRepository.findByUserUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Faculty profile not found for user: " + username));
    }

    public Student getStudentByUsername(String username) {
        return studentRepository.findByUserUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Student profile not found for user: " + username));
    }

    // ==================== DEPARTMENTS & COURSES ====================

    public List<Department> getAllDepartments() {
        return departmentRepository.findAll();
    }

    public Department getDepartmentById(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Department not found: " + id));
    }

    public Department saveDepartment(String code, String name, String headOfDepartment, String description, String coursesOffered, Integer totalSemesters) {
        Department dept = departmentRepository.findByCode(code.trim().toUpperCase()).orElse(new Department());
        dept.setCode(code.trim().toUpperCase());
        dept.setName(name.trim());
        dept.setHeadOfDepartment(headOfDepartment);
        dept.setDescription(description);
        dept.setCoursesOffered(coursesOffered);
        dept.setTotalSemesters(totalSemesters != null ? totalSemesters : 8);
        return departmentRepository.save(dept);
    }

    public void deleteDepartment(Long id) {
        departmentRepository.deleteById(id);
    }

    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    public Course getCourseById(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Course not found: " + id));
    }

    public Course saveCourse(String code, String name, Integer durationYears, Integer totalSemesters, Long departmentId) {
        Department dept = getDepartmentById(departmentId);
        Course course = courseRepository.findByCode(code.trim().toUpperCase()).orElse(new Course());
        course.setCode(code.trim().toUpperCase());
        course.setName(name.trim());
        course.setDurationYears(durationYears != null ? durationYears : 4);
        course.setTotalSemesters(totalSemesters != null ? totalSemesters : 8);
        course.setDepartment(dept);
        return courseRepository.save(course);
    }

    public void deleteCourse(Long id) {
        courseRepository.deleteById(id);
    }

    // ==================== SUBJECTS & FACULTY ASSIGNMENT ====================

    public List<Subject> getAllSubjects() {
        return subjectRepository.findAll();
    }

    public Subject getSubjectById(Long id) {
        return subjectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Subject not found: " + id));
    }

    public List<Subject> getSubjectsForStudent(Student student) {
        List<Subject> byCourse = subjectRepository.findByCourseIdAndSemesterOrderByCodeAsc(
                student.getCourse().getId(), student.getSemester());
        if (!byCourse.isEmpty()) {
            return byCourse;
        }
        return subjectRepository.findByDepartmentIdAndSemesterOrderByCodeAsc(
                student.getDepartment().getId(), student.getSemester());
    }

    public List<Subject> getSubjectsByFaculty(Long facultyId) {
        return subjectRepository.findByAssignedFacultyIdOrderByCodeAsc(facultyId);
    }

    public Subject saveSubject(String code, String name, Integer credits, Integer semester, String section,
                               String subjectType, Long departmentId, Long courseId, Long facultyId) {
        Department dept = getDepartmentById(departmentId);
        Course course = getCourseById(courseId);
        Subject subject = subjectRepository.findByCode(code.trim().toUpperCase()).orElse(new Subject());
        subject.setCode(code.trim().toUpperCase());
        subject.setName(name.trim());
        subject.setCredits(credits != null ? credits : 4);
        subject.setSemester(semester != null ? semester : 5);
        subject.setSection(section != null && !section.isBlank() ? section.trim().toUpperCase() : "A");
        subject.setSubjectType(subjectType != null ? subjectType : "THEORY");
        subject.setDepartment(dept);
        subject.setCourse(course);
        if (facultyId != null && facultyId > 0) {
            subject.setAssignedFaculty(getFacultyById(facultyId));
        } else {
            subject.setAssignedFaculty(null);
        }
        return subjectRepository.save(subject);
    }

    public Subject assignFacultyToSubject(Long subjectId, Long facultyId, String section) {
        Subject subject = getSubjectById(subjectId);
        if (facultyId != null && facultyId > 0) {
            subject.setAssignedFaculty(getFacultyById(facultyId));
        } else {
            subject.setAssignedFaculty(null);
        }
        if (section != null && !section.isBlank()) {
            subject.setSection(section.trim().toUpperCase());
        }
        return subjectRepository.save(subject);
    }

    public void deleteSubject(Long subjectId) {
        attendanceRepository.deleteBySubjectId(subjectId);
        markRepository.deleteBySubjectId(subjectId);
        assignmentRepository.deleteBySubjectId(subjectId);
        timetableRepository.deleteBySubjectId(subjectId);
        List<Examination> exams = examinationRepository.findBySubjectIdOrderByExamDateAsc(subjectId);
        examinationRepository.deleteAll(exams);
        subjectRepository.deleteById(subjectId);
    }

    // ==================== STUDENTS MANAGEMENT ====================

    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    public List<Student> searchStudents(String keyword, Long departmentId, Integer semester) {
        return studentRepository.searchStudents(keyword, departmentId, semester);
    }

    public Student getStudentById(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Student not found with ID: " + id));
    }

    public List<Student> getStudentsForSubject(Subject subject) {
        List<Student> students = studentRepository.findByCourseIdAndSemesterOrderByRollNumberAsc(
                subject.getCourse().getId(), subject.getSemester());
        if (students.isEmpty()) {
            students = studentRepository.findByDepartmentIdAndSemesterOrderByRollNumberAsc(
                    subject.getDepartment().getId(), subject.getSemester());
        }
        return students;
    }

    public Student createOrUpdateStudent(Long id, String rollNumber, String firstName, String lastName,
                                         String email, String phone, LocalDate dateOfBirth, String gender,
                                         String address, String guardianName, String guardianPhone,
                                         Integer semester, String section, Integer enrollmentYear,
                                         Long departmentId, Long courseId, String rawPassword) {
        Department department = getDepartmentById(departmentId);
        Course course = getCourseById(courseId);

        Student student;
        if (id != null && id > 0) {
            student = getStudentById(id);
            student.setRollNumber(rollNumber.trim().toUpperCase());
            student.setFirstName(firstName.trim());
            student.setLastName(lastName.trim());
            student.setEmail(email.trim().toLowerCase());
            student.setPhone(phone);
            student.setDateOfBirth(dateOfBirth);
            student.setGender(gender);
            student.setAddress(address);
            student.setGuardianName(guardianName);
            student.setGuardianPhone(guardianPhone);
            student.setSemester(semester != null ? semester : 5);
            student.setSection(section != null && !section.isBlank() ? section.trim().toUpperCase() : "A");
            student.setEnrollmentYear(enrollmentYear != null ? enrollmentYear : 2024);
            student.setDepartment(department);
            student.setCourse(course);

            User user = student.getUser();
            user.setFullName(student.getFullName());
            user.setEmail(student.getEmail());
            if (rawPassword != null && !rawPassword.isBlank()) {
                user.setPassword(passwordEncoder.encode(rawPassword.trim()));
            }
            userRepository.save(user);
        } else {
            student = new Student();
            String cleanRoll = rollNumber.trim().toUpperCase();
            String username = cleanRoll.toLowerCase();
            if (userRepository.existsByUsername(username)) {
                username = username + "_" + (System.currentTimeMillis() % 1000);
            }
            String pwd = (rawPassword != null && !rawPassword.isBlank()) ? rawPassword.trim() : "student123";
            User user = new User(
                    username,
                    passwordEncoder.encode(pwd),
                    email.trim().toLowerCase(),
                    firstName.trim() + " " + lastName.trim(),
                    Role.ROLE_STUDENT
            );
            user = userRepository.save(user);

            student.setRollNumber(cleanRoll);
            student.setFirstName(firstName.trim());
            student.setLastName(lastName.trim());
            student.setEmail(email.trim().toLowerCase());
            student.setPhone(phone);
            student.setDateOfBirth(dateOfBirth != null ? dateOfBirth : LocalDate.of(2005, 5, 15));
            student.setGender(gender != null ? gender : "Male");
            student.setAddress(address);
            student.setGuardianName(guardianName);
            student.setGuardianPhone(guardianPhone);
            student.setSemester(semester != null ? semester : 5);
            student.setSection(section != null && !section.isBlank() ? section.trim().toUpperCase() : "A");
            student.setEnrollmentYear(enrollmentYear != null ? enrollmentYear : 2024);
            student.setDepartment(department);
            student.setCourse(course);
            student.setUser(user);
        }

        return studentRepository.save(student);
    }

    public void deleteStudent(Long studentId) {
        attendanceRepository.deleteByStudentId(studentId);
        markRepository.deleteByStudentId(studentId);
        studentRepository.deleteById(studentId);
    }

    // ==================== FACULTY MANAGEMENT ====================

    public List<Faculty> getAllFaculty() {
        return facultyRepository.findAll();
    }

    public List<Faculty> searchFaculty(String keyword, Long departmentId) {
        return facultyRepository.searchFaculty(keyword, departmentId);
    }

    public Faculty getFacultyById(Long id) {
        return facultyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Faculty not found with ID: " + id));
    }

    public Faculty createOrUpdateFaculty(Long id, String employeeId, String firstName, String lastName,
                                         String email, String phone, String designation, String qualification,
                                         String specialization, LocalDate joiningDate, Long departmentId,
                                         String rawPassword) {
        Department department = getDepartmentById(departmentId);
        Faculty faculty;
        if (id != null && id > 0) {
            faculty = getFacultyById(id);
            faculty.setEmployeeId(employeeId.trim().toUpperCase());
            faculty.setFirstName(firstName.trim());
            faculty.setLastName(lastName.trim());
            faculty.setEmail(email.trim().toLowerCase());
            faculty.setPhone(phone);
            faculty.setDesignation(designation);
            faculty.setQualification(qualification);
            faculty.setSpecialization(specialization);
            faculty.setJoiningDate(joiningDate);
            faculty.setDepartment(department);

            User user = faculty.getUser();
            user.setFullName(faculty.getFullName());
            user.setEmail(faculty.getEmail());
            if (rawPassword != null && !rawPassword.isBlank()) {
                user.setPassword(passwordEncoder.encode(rawPassword.trim()));
            }
            userRepository.save(user);
        } else {
            faculty = new Faculty();
            String cleanEmpId = employeeId.trim().toUpperCase();
            String username = cleanEmpId.toLowerCase().replace("-", "");
            if (userRepository.existsByUsername(username)) {
                username = username + "_" + (System.currentTimeMillis() % 1000);
            }
            String pwd = (rawPassword != null && !rawPassword.isBlank()) ? rawPassword.trim() : "faculty123";
            User user = new User(
                    username,
                    passwordEncoder.encode(pwd),
                    email.trim().toLowerCase(),
                    firstName.trim() + " " + lastName.trim(),
                    Role.ROLE_FACULTY
            );
            user = userRepository.save(user);

            faculty.setEmployeeId(cleanEmpId);
            faculty.setFirstName(firstName.trim());
            faculty.setLastName(lastName.trim());
            faculty.setEmail(email.trim().toLowerCase());
            faculty.setPhone(phone);
            faculty.setDesignation(designation != null ? designation : "Assistant Professor");
            faculty.setQualification(qualification != null ? qualification : "Ph.D.");
            faculty.setSpecialization(specialization);
            faculty.setJoiningDate(joiningDate != null ? joiningDate : LocalDate.now());
            faculty.setDepartment(department);
            faculty.setUser(user);
        }

        return facultyRepository.save(faculty);
    }

    public void deleteFaculty(Long facultyId) {
        List<Subject> assigned = subjectRepository.findByAssignedFacultyIdOrderByCodeAsc(facultyId);
        for (Subject s : assigned) {
            s.setAssignedFaculty(null);
            subjectRepository.save(s);
        }
        List<Assignment> assignments = assignmentRepository.findByCreatedByIdOrderByDueDateAsc(facultyId);
        assignmentRepository.deleteAll(assignments);

        List<Attendance> markedAttendance = attendanceRepository.findAll().stream()
                .filter(a -> a.getMarkedBy() != null && Objects.equals(a.getMarkedBy().getId(), facultyId))
                .toList();
        for (Attendance a : markedAttendance) {
            a.setMarkedBy(null);
            attendanceRepository.save(a);
        }

        List<Mark> enteredMarks = markRepository.findAll().stream()
                .filter(m -> m.getEnteredBy() != null && Objects.equals(m.getEnteredBy().getId(), facultyId))
                .toList();
        for (Mark m : enteredMarks) {
            m.setEnteredBy(null);
            markRepository.save(m);
        }

        facultyRepository.deleteById(facultyId);
    }

    // ==================== ATTENDANCE MANAGEMENT & <75% ALERTS ====================

    public void verifyFacultyOwnsSubject(Faculty faculty, Subject subject) {
        if (subject.getAssignedFaculty() == null || !Objects.equals(subject.getAssignedFaculty().getId(), faculty.getId())) {
            throw new AccessDeniedException("Security Violation: Faculty member " + faculty.getFullName()
                    + " is not assigned to subject " + subject.getCode());
        }
    }

    public void markBulkAttendance(Long subjectId, LocalDate date, Map<Long, AttendanceStatus> statusByStudentId,
                                   Map<Long, String> remarksByStudentId, Faculty faculty) {
        Subject subject = getSubjectById(subjectId);
        if (faculty != null) {
            verifyFacultyOwnsSubject(faculty, subject);
        }
        LocalDate targetDate = date != null ? date : LocalDate.now();

        for (Map.Entry<Long, AttendanceStatus> entry : statusByStudentId.entrySet()) {
            Long studentId = entry.getKey();
            AttendanceStatus status = entry.getValue() != null ? entry.getValue() : AttendanceStatus.PRESENT;
            String remarks = remarksByStudentId != null ? remarksByStudentId.getOrDefault(studentId, "") : "";

            Student student = getStudentById(studentId);
            Attendance attendance = attendanceRepository
                    .findByStudentIdAndSubjectIdAndAttendanceDate(studentId, subjectId, targetDate)
                    .orElse(new Attendance());

            attendance.setStudent(student);
            attendance.setSubject(subject);
            attendance.setAttendanceDate(targetDate);
            attendance.setStatus(status);
            attendance.setRemarks(remarks);
            attendance.setMarkedBy(faculty);
            attendanceRepository.save(attendance);
        }
    }

    public Map<Long, Attendance> getAttendanceMapForSubjectAndDate(Long subjectId, LocalDate date) {
        List<Attendance> records = attendanceRepository.findBySubjectIdAndAttendanceDate(subjectId, date);
        Map<Long, Attendance> map = new HashMap<>();
        for (Attendance a : records) {
            map.put(a.getStudent().getId(), a);
        }
        return map;
    }

    public List<AttendanceSummaryDto> getSubjectWiseAttendanceForStudent(Long studentId) {
        Student student = getStudentById(studentId);
        List<Subject> subjects = getSubjectsForStudent(student);
        List<Attendance> allRecords = attendanceRepository.findByStudentIdOrderByAttendanceDateDesc(studentId);

        List<AttendanceSummaryDto> summaries = new ArrayList<>();
        for (Subject subject : subjects) {
            List<Attendance> subRecords = allRecords.stream()
                    .filter(a -> Objects.equals(a.getSubject().getId(), subject.getId()))
                    .toList();
            int total = subRecords.size();
            int present = (int) subRecords.stream().filter(a -> a.getStatus() == AttendanceStatus.PRESENT).count();
            int absent = (int) subRecords.stream().filter(a -> a.getStatus() == AttendanceStatus.ABSENT).count();
            int leave = (int) subRecords.stream().filter(a -> a.getStatus() == AttendanceStatus.LEAVE).count();
            summaries.add(new AttendanceSummaryDto(student, subject, total, present, absent, leave));
        }
        return summaries;
    }

    public List<AttendanceSummaryDto> getStudentAttendanceSummariesForSubject(Long subjectId) {
        Subject subject = getSubjectById(subjectId);
        List<Student> students = getStudentsForSubject(subject);
        List<Attendance> subRecords = attendanceRepository.findBySubjectIdOrderByAttendanceDateDesc(subjectId);

        List<AttendanceSummaryDto> summaries = new ArrayList<>();
        for (Student student : students) {
            List<Attendance> stuRecords = subRecords.stream()
                    .filter(a -> Objects.equals(a.getStudent().getId(), student.getId()))
                    .toList();
            int total = stuRecords.size();
            int present = (int) stuRecords.stream().filter(a -> a.getStatus() == AttendanceStatus.PRESENT).count();
            int absent = (int) stuRecords.stream().filter(a -> a.getStatus() == AttendanceStatus.ABSENT).count();
            int leave = (int) stuRecords.stream().filter(a -> a.getStatus() == AttendanceStatus.LEAVE).count();
            summaries.add(new AttendanceSummaryDto(student, subject, total, present, absent, leave));
        }
        return summaries;
    }

    public List<Attendance> getAttendanceHistoryForStudent(Long studentId, Long subjectId) {
        if (subjectId != null && subjectId > 0) {
            return attendanceRepository.findByStudentIdAndSubjectIdOrderByAttendanceDateDesc(studentId, subjectId);
        }
        return attendanceRepository.findByStudentIdOrderByAttendanceDateDesc(studentId);
    }

    // ==================== MARKS & EVALUATION MANAGEMENT ====================

    public Mark saveOrUpdateMark(Long studentId, Long subjectId, Double internalMarks, Double assignmentMarks,
                                 Double practicalMarks, Double externalMarks, String remarks, Faculty faculty) {
        Subject subject = getSubjectById(subjectId);
        if (faculty != null) {
            verifyFacultyOwnsSubject(faculty, subject);
        }
        Student student = getStudentById(studentId);

        // Unit 2 (Anjay): validate with a checked exception, then wrap it (exception chaining)
        // so callers keep their simple unchecked contract.
        try {
            MarksValidator.validate(internalMarks, assignmentMarks, practicalMarks, externalMarks);
        } catch (InvalidMarksException e) {
            throw new IllegalArgumentException(student.getRollNumber() + ": " + e.getMessage(), e);
        }

        Mark mark = markRepository.findByStudentIdAndSubjectId(studentId, subjectId)
                .orElse(new Mark());
        mark.setStudent(student);
        mark.setSubject(subject);
        mark.setSemester(subject.getSemester());
        mark.setInternalMarks(internalMarks != null ? internalMarks : 0.0);
        mark.setAssignmentMarks(assignmentMarks != null ? assignmentMarks : 0.0);
        mark.setPracticalMarks(practicalMarks != null ? practicalMarks : 0.0);
        mark.setExternalMarks(externalMarks != null ? externalMarks : 0.0);
        mark.setRemarks(remarks);
        mark.setEnteredBy(faculty);
        mark.calculateGradeAndTotal();

        return markRepository.save(mark);
    }

    public List<Mark> getMarksForStudent(Long studentId) {
        return markRepository.findByStudentIdOrderBySubjectCodeAsc(studentId);
    }

    public List<Mark> getMarksForSubject(Long subjectId) {
        return markRepository.findBySubjectIdOrderByStudentRollNumberAsc(subjectId);
    }

    public Map<Long, Mark> getMarksMapForSubject(Long subjectId) {
        List<Mark> marks = getMarksForSubject(subjectId);
        Map<Long, Mark> map = new HashMap<>();
        for (Mark m : marks) {
            map.put(m.getStudent().getId(), m);
        }
        return map;
    }

    // ==================== PERFORMANCE ANALYTICS ====================

    public StudentPerformanceDto getStudentPerformance(Student student) {
        List<Attendance> attRecords = attendanceRepository.findByStudentIdOrderByAttendanceDateDesc(student.getId());
        int totalClasses = attRecords.size();
        int presentClasses = (int) attRecords.stream().filter(a -> a.getStatus() == AttendanceStatus.PRESENT).count();
        double attendancePct = totalClasses > 0
                ? Math.round(((double) presentClasses / totalClasses) * 1000.0) / 10.0
                : 100.0;

        // Also check if any individual subject has < 75% attendance
        List<AttendanceSummaryDto> subjectSummaries = getSubjectWiseAttendanceForStudent(student.getId());
        boolean anySubjectLow = subjectSummaries.stream().anyMatch(AttendanceSummaryDto::isLowAttendance);
        boolean lowAttendanceAlert = (attendancePct < 75.0) || anySubjectLow;

        List<Mark> marks = markRepository.findByStudentIdOrderBySubjectCodeAsc(student.getId());
        int subjectsEvaluated = marks.size();
        int subjectsPassed = (int) marks.stream().filter(m -> "PASS".equalsIgnoreCase(m.getResultStatus())).count();
        int backlogs = subjectsEvaluated - subjectsPassed;

        double avgPct = 0.0;
        double sgpa = 0.0;
        if (!marks.isEmpty()) {
            double totalPct = marks.stream().mapToDouble(Mark::getPercentage).sum();
            avgPct = Math.round((totalPct / marks.size()) * 10.0) / 10.0;

            double totalWeightedPoints = 0.0;
            int totalCredits = 0;
            for (Mark m : marks) {
                int credits = m.getSubject().getCredits() != null ? m.getSubject().getCredits() : 4;
                totalWeightedPoints += (m.getGradePoints() * credits);
                totalCredits += credits;
            }
            sgpa = totalCredits > 0 ? Math.round((totalWeightedPoints / totalCredits) * 100.0) / 100.0 : 0.0;
        }

        String overallGrade;
        if (avgPct >= 90) overallGrade = "O";
        else if (avgPct >= 80) overallGrade = "A+";
        else if (avgPct >= 70) overallGrade = "A";
        else if (avgPct >= 60) overallGrade = "B+";
        else if (avgPct >= 50) overallGrade = "B";
        else if (avgPct >= 40) overallGrade = "C";
        else overallGrade = subjectsEvaluated == 0 ? "N/A" : "F";

        return new StudentPerformanceDto(
                student,
                totalClasses,
                presentClasses,
                attendancePct,
                lowAttendanceAlert,
                avgPct,
                sgpa,
                subjectsEvaluated,
                subjectsPassed,
                backlogs,
                overallGrade
        );
    }

    public List<StudentPerformanceDto> getAllStudentPerformances(Long departmentId) {
        List<Student> students = (departmentId != null && departmentId > 0)
                ? studentRepository.findByDepartmentIdOrderByRollNumberAsc(departmentId)
                : studentRepository.findAll();
        return students.stream()
                .map(this::getStudentPerformance)
                .collect(Collectors.toList());
    }

    public List<StudentPerformanceDto> getLowAttendanceStudents(Long departmentId) {
        return getAllStudentPerformances(departmentId).stream()
                .filter(StudentPerformanceDto::isLowAttendanceAlert)
                .sorted(Comparator.comparingDouble(StudentPerformanceDto::getAttendancePercentage))
                .collect(Collectors.toList());
    }

    // ==================== TIMETABLE, EXAMS, ASSIGNMENTS & NOTICES ====================

    public List<Timetable> getAllTimetables() {
        return timetableRepository.findAllByOrderByDayOfWeekAscStartTimeAsc();
    }

    public List<Timetable> getTimetableForDepartmentAndSemester(Long departmentId, Integer semester) {
        if (departmentId != null && semester != null) {
            return timetableRepository.findByDepartmentIdAndSemesterOrderByDayOfWeekAscStartTimeAsc(departmentId, semester);
        }
        return getAllTimetables();
    }

    public List<Timetable> getTimetableForFaculty(Long facultyId) {
        return timetableRepository.findBySubjectAssignedFacultyIdOrderByDayOfWeekAscStartTimeAsc(facultyId);
    }

    public Timetable saveTimetableSlot(String dayOfWeek, LocalTime startTime, LocalTime endTime, Long subjectId,
                                       String section, String roomNumber, String slotType) {
        Subject subject = getSubjectById(subjectId);
        Timetable slot = new Timetable(
                dayOfWeek,
                startTime,
                endTime,
                subject,
                subject.getDepartment(),
                subject.getSemester(),
                section != null && !section.isBlank() ? section.trim().toUpperCase() : "A",
                roomNumber,
                slotType != null ? slotType : "LECTURE"
        );
        return timetableRepository.save(slot);
    }

    public void deleteTimetableSlot(Long id) {
        timetableRepository.deleteById(id);
    }

    public List<Examination> getAllExaminations() {
        return examinationRepository.findAllByOrderByExamDateAscStartTimeAsc();
    }

    public List<Examination> getExaminationsForStudent(Student student) {
        return examinationRepository.findBySubjectDepartmentIdAndSemesterOrderByExamDateAsc(
                student.getDepartment().getId(), student.getSemester());
    }

    public Examination saveExamination(String name, String examType, Long subjectId, LocalDate examDate,
                                       LocalTime startTime, LocalTime endTime, String roomNumber,
                                       Integer maxMarks, String status) {
        Subject subject = getSubjectById(subjectId);
        Examination exam = new Examination(
                name,
                examType,
                subject.getSemester(),
                subject,
                examDate,
                startTime,
                endTime,
                roomNumber,
                maxMarks != null ? maxMarks : 100,
                status != null ? status : "UPCOMING"
        );
        return examinationRepository.save(exam);
    }

    public void deleteExamination(Long id) {
        examinationRepository.deleteById(id);
    }

    public List<Assignment> getAllAssignments() {
        return assignmentRepository.findAllByOrderByDueDateAsc();
    }

    public List<Assignment> getAssignmentsForFaculty(Long facultyId) {
        return assignmentRepository.findByCreatedByIdOrderByDueDateAsc(facultyId);
    }

    public List<Assignment> getAssignmentsForStudent(Student student) {
        return assignmentRepository.findBySubjectDepartmentIdAndSubjectSemesterOrderByDueDateAsc(
                student.getDepartment().getId(), student.getSemester());
    }

    public Assignment saveAssignment(String title, String description, Long subjectId, Faculty faculty,
                                     LocalDate dueDate, Integer maxMarks, String referenceLink) {
        Subject subject = getSubjectById(subjectId);
        if (faculty != null) {
            verifyFacultyOwnsSubject(faculty, subject);
        }
        Assignment assignment = new Assignment(
                title,
                description,
                subject,
                faculty != null ? faculty : subject.getAssignedFaculty(),
                dueDate != null ? dueDate : LocalDate.now().plusDays(7),
                maxMarks != null ? maxMarks : 20,
                referenceLink
        );
        return assignmentRepository.save(assignment);
    }

    public void deleteAssignment(Long id, Faculty faculty) {
        Assignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found: " + id));
        if (faculty != null && assignment.getCreatedBy() != null
                && !Objects.equals(assignment.getCreatedBy().getId(), faculty.getId())) {
            throw new AccessDeniedException("You can only delete assignments created by you.");
        }
        assignmentRepository.deleteById(id);
    }

    public List<Notice> getAllNotices() {
        return noticeRepository.findAllByOrderByPinnedDescPublishedDateDesc();
    }

    public List<Notice> getNoticesForAudience(String roleAudience) {
        return noticeRepository.findByTargetAudienceInOrderByPinnedDescPublishedDateDesc(
                Arrays.asList("ALL", roleAudience));
    }

    public Notice saveNotice(String title, String content, String category, String targetAudience,
                             Long departmentId, User postedBy, boolean pinned) {
        Department dept = (departmentId != null && departmentId > 0) ? getDepartmentById(departmentId) : null;
        Notice notice = new Notice(
                title,
                content,
                category != null ? category : "ACADEMIC",
                targetAudience != null ? targetAudience : "ALL",
                dept,
                postedBy,
                pinned
        );
        return noticeRepository.save(notice);
    }

    public void togglePinNotice(Long noticeId) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new IllegalArgumentException("Notice not found: " + noticeId));
        notice.setPinned(!notice.isPinned());
        noticeRepository.save(notice);
    }

    public void deleteNotice(Long noticeId) {
        noticeRepository.deleteById(noticeId);
    }

    // ==================== PROFILE & PASSWORD UPDATE ====================

    public void updateStudentContactAndPassword(Student student, String phone, String address, String newPassword) {
        if (phone != null && !phone.isBlank()) {
            student.setPhone(phone.trim());
        }
        if (address != null && !address.isBlank()) {
            student.setAddress(address.trim());
        }
        studentRepository.save(student);

        if (newPassword != null && !newPassword.isBlank()) {
            User user = student.getUser();
            user.setPassword(passwordEncoder.encode(newPassword.trim()));
            userRepository.save(user);
        }
    }
}

