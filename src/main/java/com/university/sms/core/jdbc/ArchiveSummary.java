package com.university.sms.core.jdbc;

import java.util.List;
import java.util.Map;

/**
 * UNIT 4 - JDBC (Arindam).
 * Facts about the archive read through {@link java.sql.DatabaseMetaData} and COUNT queries.
 */
public record ArchiveSummary(String databaseProduct, String databaseVersion, String driverName,
                             String jdbcUrl, List<String> tables, Map<String, Integer> rowCounts,
                             String lastSnapshotAt, long fileSizeBytes) {
}
