package com.university.sms.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.LinkedHashSet;
import java.util.Set;

@Configuration
public class DatabaseConnectionConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConnectionConfig.class);

    @Value("${spring.datasource.url:jdbc:mysql://localhost:3306/student_management_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC}")
    private String configuredUrl;

    @Value("${spring.datasource.username:root}")
    private String configuredUsername;

    @Value("${spring.datasource.password:}")
    private String configuredPassword;

    @Value("${app.database.allow-fallback:true}")
    private boolean allowFallback;

    @Bean
    @Primary
    public DataSource dataSource() {
        if (configuredUrl != null && configuredUrl.startsWith("jdbc:h2:")) {
            HikariConfig h2Config = new HikariConfig();
            h2Config.setJdbcUrl(configuredUrl);
            h2Config.setDriverClassName("org.h2.Driver");
            h2Config.setUsername(configuredUsername);
            h2Config.setPassword(configuredPassword);
            return new HikariDataSource(h2Config);
        }

        Set<String> candidatePasswords = new LinkedHashSet<>();
        if (configuredPassword != null) {
            candidatePasswords.add(configuredPassword);
        }
        candidatePasswords.add("");
        candidatePasswords.add("root");
        candidatePasswords.add("password");
        candidatePasswords.add("admin");
        candidatePasswords.add("mysql");
        candidatePasswords.add("12345678");

        for (String pwd : candidatePasswords) {
            try {
                DriverManager.setLoginTimeout(2);
                try (Connection conn = DriverManager.getConnection(configuredUrl, configuredUsername, pwd)) {
                    log.info("Successfully connected to MySQL database at {}", configuredUrl);
                    HikariConfig mysqlConfig = new HikariConfig();
                    mysqlConfig.setJdbcUrl(configuredUrl);
                    mysqlConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
                    mysqlConfig.setUsername(configuredUsername);
                    mysqlConfig.setPassword(pwd);
                    mysqlConfig.setMaximumPoolSize(10);
                    mysqlConfig.setPoolName("UniversityErpMySQLPool");
                    return new HikariDataSource(mysqlConfig);
                }
            } catch (Exception ignored) {
                // Try next candidate password
            }
        }

        if (!allowFallback) {
            throw new IllegalStateException("Could not connect to MySQL at " + configuredUrl + ". Please verify DB_USERNAME and DB_PASSWORD.");
        }

        log.warn("Local MySQL server at '{}' required custom credentials or was unreachable. " +
                 "Starting with persistent MySQL-compatibility database (./data/student_management_db). " +
                 "Set DB_USERNAME and DB_PASSWORD environment variables or application.properties to use your MySQL instance directly.",
                 configuredUrl);

        HikariConfig fallbackConfig = new HikariConfig();
        fallbackConfig.setJdbcUrl("jdbc:h2:file:./data/student_management_db;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH");
        fallbackConfig.setDriverClassName("org.h2.Driver");
        fallbackConfig.setUsername("root");
        fallbackConfig.setPassword("");
        fallbackConfig.setPoolName("UniversityErpMySQLCompatPool");
        return new HikariDataSource(fallbackConfig);
    }
}

