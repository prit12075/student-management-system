package com.university.sms.config;

import com.university.sms.core.concurrent.PortalActivityMonitor;
import com.university.sms.core.io.AuditTrailLogger;
import com.university.sms.core.jdbc.SqliteArchiveDao;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

/**
 * Wires the plain-Java core modules into Spring as beans (Prit Patel - integration).
 * Paths are configurable in application.properties (app.audit-log, app.archive-db).
 */
@Configuration
public class JavaLabConfig implements WebMvcConfigurer {

    @Bean
    public AuditTrailLogger auditTrailLogger(@Value("${app.audit-log:logs/audit-trail.log}") String path) {
        return new AuditTrailLogger(Path.of(path));
    }

    @Bean
    public SqliteArchiveDao sqliteArchiveDao(@Value("${app.archive-db:archive/sms_archive.db}") String path) {
        return new SqliteArchiveDao(Path.of(path));
    }

    @Bean
    public PortalActivityMonitor portalActivityMonitor() {
        return new PortalActivityMonitor();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new ActivityTrackingInterceptor(portalActivityMonitor()))
                .addPathPatterns("/admin/**", "/faculty/**", "/student/**");
    }
}
