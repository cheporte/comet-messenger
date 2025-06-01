package com.comet.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseManager {
    private static DatabaseManager instance;
    private final HikariDataSource dataSource;

    /**
     * Private constructor that initializes the HikariCP data source using environment variables.
     */
    private DatabaseManager() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(System.getenv("COMET_DB_URL"));
        config.setUsername(System.getenv("COMET_DB_USER"));
        config.setPassword(System.getenv("COMET_DB_PASS"));
        config.setMaximumPoolSize(10);
        dataSource = new HikariDataSource(config);
    }

    /**
     * Returns the singleton instance of DatabaseManager, creating it if necessary.
     *
     * @return the singleton DatabaseManager instance
     */
    public static synchronized DatabaseManager getInstance() {
        if (instance == null) instance = new DatabaseManager();
        return instance;
    }

    /**
     * Retrieves a database connection from the HikariCP data source.
     *
     * @return a Connection object from the pool
     * @throws SQLException if a database access error occurs
     */
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
}
