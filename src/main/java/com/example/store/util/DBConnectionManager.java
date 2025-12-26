package com.example.store.util;

import java.sql.Connection;

/**
 * DBConnectionManager: provide JDBC Connections (wrap DataSource or DriverManager).
 * Explicitly loads MySQL driver to avoid "No suitable driver" in some classloader setups.
 */
public class DBConnectionManager {
    private final String jdbcUrl;
    private final String username;
    private final String password;

    static {
        try {
            // Ensure MySQL driver is registered with DriverManager (JDBC 4 usually auto-registers)
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException ignored) {
            // Will fail later if driver is truly missing
        }
    }

    public DBConnectionManager(String jdbcUrl, String username, String password) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
    }

    public Connection getConnection() throws java.sql.SQLException {
        return java.sql.DriverManager.getConnection(jdbcUrl, username, password);
    }
}
