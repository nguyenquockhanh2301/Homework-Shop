package com.example.store.util;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Initializes minimal DB schema required for authentication.
 * In dev/classroom setups the schema.sql may not have been executed; this ensures the users table exists.
 */
public final class SchemaInitializer {
    private SchemaInitializer() {}

    public static void ensureUsersTable(DBConnectionManager db) {
        final String ddl = "CREATE TABLE IF NOT EXISTS users (" +
                "id BIGINT NOT NULL AUTO_INCREMENT," +
                "username VARCHAR(100) NOT NULL," +
                "email VARCHAR(255) NOT NULL," +
                "password_hash VARBINARY(255) NOT NULL," +
                "password_salt VARBINARY(255) NOT NULL," +
                "role VARCHAR(20) NOT NULL DEFAULT 'USER'," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "version INT NOT NULL DEFAULT 1," +
                "PRIMARY KEY (id)," +
                "UNIQUE KEY uk_users_username (username)," +
                "UNIQUE KEY uk_users_email (email)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        try (Connection conn = db.getConnection(); Statement st = conn.createStatement()) {
            st.executeUpdate(ddl);
            // Add role column for older schemas; IF NOT EXISTS avoids errors on fresh DBs.
            st.executeUpdate("ALTER TABLE users ADD COLUMN IF NOT EXISTS role VARCHAR(20) NOT NULL DEFAULT 'USER'");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to ensure users table", e);
        }
    }
}
