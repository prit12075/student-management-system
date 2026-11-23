package com.university.sms.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notices")
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(nullable = false, length = 1500)
    private String content;

    @Column(nullable = false, length = 40)
    private String category = "ACADEMIC"; // ACADEMIC, EXAMINATION, EVENT, URGENT, GENERAL

    @Column(name = "target_audience", nullable = false, length = 30)
    private String targetAudience = "ALL"; // ALL, STUDENTS, FACULTY

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "department_id")
    private Department department; // nullable for university-wide notice

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "posted_by_user_id")
    private User postedBy;

    @Column(nullable = false)
    private boolean pinned = false;

    @Column(name = "published_date", nullable = false)
    private LocalDateTime publishedDate = LocalDateTime.now();

    public Notice() {
    }

    public Notice(String title, String content, String category, String targetAudience, Department department, User postedBy, boolean pinned) {
        this.title = title;
        this.content = content;
        this.category = category;
        this.targetAudience = targetAudience;
        this.department = department;
        this.postedBy = postedBy;
        this.pinned = pinned;
        this.publishedDate = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getTargetAudience() {
        return targetAudience;
    }

    public void setTargetAudience(String targetAudience) {
        this.targetAudience = targetAudience;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public User getPostedBy() {
        return postedBy;
    }

    public void setPostedBy(User postedBy) {
        this.postedBy = postedBy;
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }

    public LocalDateTime getPublishedDate() {
        return publishedDate;
    }

    public void setPublishedDate(LocalDateTime publishedDate) {
        this.publishedDate = publishedDate;
    }
}

