package com.university.sms.config;

import com.university.sms.core.collections.SeatingPlanGenerator;
import com.university.sms.core.concurrent.AlertScheduler;
import com.university.sms.core.concurrent.NotificationCenter;
import com.university.sms.core.risk.AcademicRiskAssessor;
import com.university.sms.core.verify.MarksheetFingerprint;
import com.university.sms.service.JavaLabService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the four "Smart Campus" novel features into Spring (Prit Patel - integration).
 * The alert scheduler is started when the application starts and stopped on shutdown.
 */
@Configuration
public class SmartCampusConfig {

    @Bean
    public NotificationCenter notificationCenter() {
        return new NotificationCenter();
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public AlertScheduler alertScheduler(JavaLabService labService, NotificationCenter center,
                                         @Value("${app.alert-scan-seconds:120}") long period) {
        // method reference used as a Supplier<List<StudentRecord>>
        return new AlertScheduler(labService::loadStudentRecords, center, period);
    }

    @Bean
    public MarksheetFingerprint marksheetFingerprint(
            @Value("${app.verify-secret:apex-erp-marksheet-seal-2026}") String secret) {
        return new MarksheetFingerprint(secret);
    }

    @Bean
    public AcademicRiskAssessor academicRiskAssessor() {
        return new AcademicRiskAssessor();
    }

    @Bean
    public SeatingPlanGenerator seatingPlanGenerator() {
        return new SeatingPlanGenerator();
    }
}
