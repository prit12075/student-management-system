-- =============================================================================
-- APEX UNIVERSITY ERP - STUDENT MANAGEMENT SYSTEM (MySQL 8.x Schema)
-- Note: Spring Data JPA / Hibernate (`spring.jpa.hibernate.ddl-auto=update`)
-- automatically creates and updates these tables and seeds demo data via
-- DataInitializer.java on startup. You may also run this script manually in
-- MySQL Workbench or the MySQL CLI.
-- =============================================================================

CREATE DATABASE IF NOT EXISTS student_management_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE student_management_db;

-- 1. Users Table (Role-Based Authentication with BCrypt Passwords)
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(60) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(120) NOT NULL UNIQUE,
    full_name VARCHAR(120) NOT NULL,
    role VARCHAR(30) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. Departments Table
CREATE TABLE IF NOT EXISTS departments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    head_of_department VARCHAR(120),
    description VARCHAR(500),
    courses_offered VARCHAR(250),
    total_semesters INT DEFAULT 8
);

-- 3. Courses Table
CREATE TABLE IF NOT EXISTS courses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    duration_years INT NOT NULL DEFAULT 4,
    total_semesters INT NOT NULL DEFAULT 8,
    department_id BIGINT NOT NULL,
    CONSTRAINT fk_course_department FOREIGN KEY (department_id) REFERENCES departments(id)
);

-- 4. Faculty Table
CREATE TABLE IF NOT EXISTS faculty (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id VARCHAR(30) NOT NULL UNIQUE,
    first_name VARCHAR(60) NOT NULL,
    last_name VARCHAR(60) NOT NULL,
    email VARCHAR(120) NOT NULL UNIQUE,
    phone VARCHAR(25),
    designation VARCHAR(80),
    qualification VARCHAR(120),
    specialization VARCHAR(150),
    joining_date DATE,
    user_id BIGINT NOT NULL UNIQUE,
    department_id BIGINT NOT NULL,
    CONSTRAINT fk_faculty_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_faculty_department FOREIGN KEY (department_id) REFERENCES departments(id)
);

-- 5. Students Table
CREATE TABLE IF NOT EXISTS students (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    roll_number VARCHAR(30) NOT NULL UNIQUE,
    first_name VARCHAR(60) NOT NULL,
    last_name VARCHAR(60) NOT NULL,
    email VARCHAR(120) NOT NULL UNIQUE,
    phone VARCHAR(25),
    date_of_birth DATE,
    gender VARCHAR(20),
    address VARCHAR(300),
    guardian_name VARCHAR(120),
    guardian_phone VARCHAR(25),
    semester INT NOT NULL DEFAULT 5,
    section VARCHAR(10) DEFAULT 'A',
    enrollment_year INT DEFAULT 2024,
    user_id BIGINT NOT NULL UNIQUE,
    department_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    CONSTRAINT fk_student_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_student_department FOREIGN KEY (department_id) REFERENCES departments(id),
    CONSTRAINT fk_student_course FOREIGN KEY (course_id) REFERENCES courses(id)
);

-- 6. Subjects Table
CREATE TABLE IF NOT EXISTS subjects (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    credits INT NOT NULL DEFAULT 4,
    semester INT NOT NULL DEFAULT 5,
    class_section VARCHAR(10) DEFAULT 'A',
    subject_type VARCHAR(30) DEFAULT 'THEORY',
    max_internal_marks INT DEFAULT 30,
    max_assignment_marks INT DEFAULT 20,
    max_practical_marks INT DEFAULT 20,
    max_external_marks INT DEFAULT 30,
    department_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    faculty_id BIGINT,
    CONSTRAINT fk_subject_department FOREIGN KEY (department_id) REFERENCES departments(id),
    CONSTRAINT fk_subject_course FOREIGN KEY (course_id) REFERENCES courses(id),
    CONSTRAINT fk_subject_faculty FOREIGN KEY (faculty_id) REFERENCES faculty(id)
);

-- 7. Attendance Table (PRESENT, ABSENT, LEAVE)
CREATE TABLE IF NOT EXISTS attendance (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    subject_id BIGINT NOT NULL,
    attendance_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    remarks VARCHAR(250),
    marked_by_faculty_id BIGINT,
    CONSTRAINT uq_student_subject_date UNIQUE (student_id, subject_id, attendance_date),
    CONSTRAINT fk_attendance_student FOREIGN KEY (student_id) REFERENCES students(id),
    CONSTRAINT fk_attendance_subject FOREIGN KEY (subject_id) REFERENCES subjects(id),
    CONSTRAINT fk_attendance_faculty FOREIGN KEY (marked_by_faculty_id) REFERENCES faculty(id)
);

-- 8. Examinations Table
CREATE TABLE IF NOT EXISTS examinations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    exam_type VARCHAR(30) NOT NULL,
    semester INT NOT NULL DEFAULT 5,
    subject_id BIGINT NOT NULL,
    exam_date DATE NOT NULL,
    start_time TIME,
    end_time TIME,
    room_number VARCHAR(40),
    max_marks INT DEFAULT 100,
    status VARCHAR(30) DEFAULT 'UPCOMING',
    CONSTRAINT fk_exam_subject FOREIGN KEY (subject_id) REFERENCES subjects(id)
);

-- 9. Marks Table (Internal 30 + Assignment 20 + Practical 20 + External 30 = 100)
CREATE TABLE IF NOT EXISTS marks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    subject_id BIGINT NOT NULL,
    semester INT NOT NULL DEFAULT 5,
    internal_marks DOUBLE NOT NULL DEFAULT 0,
    assignment_marks DOUBLE NOT NULL DEFAULT 0,
    practical_marks DOUBLE NOT NULL DEFAULT 0,
    external_marks DOUBLE NOT NULL DEFAULT 0,
    total_marks DOUBLE NOT NULL DEFAULT 0,
    percentage DOUBLE NOT NULL DEFAULT 0,
    grade VARCHAR(10) NOT NULL DEFAULT 'F',
    grade_points INT NOT NULL DEFAULT 0,
    result_status VARCHAR(20) NOT NULL DEFAULT 'FAIL',
    remarks VARCHAR(250),
    entered_by_faculty_id BIGINT,
    updated_at DATETIME,
    CONSTRAINT uq_mark_student_subject UNIQUE (student_id, subject_id),
    CONSTRAINT fk_mark_student FOREIGN KEY (student_id) REFERENCES students(id),
    CONSTRAINT fk_mark_subject FOREIGN KEY (subject_id) REFERENCES subjects(id),
    CONSTRAINT fk_mark_faculty FOREIGN KEY (entered_by_faculty_id) REFERENCES faculty(id)
);

-- 10. Assignments Table
CREATE TABLE IF NOT EXISTS assignments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(160) NOT NULL,
    description VARCHAR(800),
    subject_id BIGINT NOT NULL,
    faculty_id BIGINT NOT NULL,
    due_date DATE NOT NULL,
    max_marks INT DEFAULT 20,
    reference_link VARCHAR(250),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_assignment_subject FOREIGN KEY (subject_id) REFERENCES subjects(id),
    CONSTRAINT fk_assignment_faculty FOREIGN KEY (faculty_id) REFERENCES faculty(id)
);

-- 11. Timetables Table
CREATE TABLE IF NOT EXISTS timetables (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    day_of_week VARCHAR(20) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    subject_id BIGINT NOT NULL,
    department_id BIGINT NOT NULL,
    semester INT NOT NULL DEFAULT 5,
    class_section VARCHAR(10) DEFAULT 'A',
    room_number VARCHAR(40),
    slot_type VARCHAR(30) DEFAULT 'LECTURE',
    CONSTRAINT fk_timetable_subject FOREIGN KEY (subject_id) REFERENCES subjects(id),
    CONSTRAINT fk_timetable_department FOREIGN KEY (department_id) REFERENCES departments(id)
);

-- 12. Notices Table
CREATE TABLE IF NOT EXISTS notices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(180) NOT NULL,
    content VARCHAR(1500) NOT NULL,
    category VARCHAR(40) NOT NULL DEFAULT 'ACADEMIC',
    target_audience VARCHAR(30) NOT NULL DEFAULT 'ALL',
    department_id BIGINT,
    posted_by_user_id BIGINT,
    pinned BOOLEAN NOT NULL DEFAULT FALSE,
    published_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notice_department FOREIGN KEY (department_id) REFERENCES departments(id),
    CONSTRAINT fk_notice_user FOREIGN KEY (posted_by_user_id) REFERENCES users(id)
);

