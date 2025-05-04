package com.comet.db;

import java.sql.*;

public class DatabaseManager {
    private static DatabaseManager instance;
    private Connection connection;

    private static final String DB_URL = "jdbc:postgresql://localhost:5432/cometdb";
    private static final String DB_USER = "USERNAME";
    private static final String DB_PASS = "PASSWORD"; // haven't come up with env handling yet

    private DatabaseManager() {
        try {
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
            initSchema();
        } catch (SQLException e) {
            System.err.println("[DB] Failed to connect to database:");
            e.printStackTrace();
        }
    }

    public static DatabaseManager getInstance() {
        if (instance == null) instance = new DatabaseManager();
        if (instance.connection == null) throw new IllegalStateException("DB connection is null");
        return instance;
    }

    private void initSchema() throws SQLException {
        String query = """
            CREATE TABLE IF NOT EXISTS users (
                id SERIAL PRIMARY KEY,
                username VARCHAR(255) UNIQUE NOT NULL,
                password VARCHAR(255) NOT NULL
            )
        """;
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(query);
            System.out.println("[DB] User table ensured.");
        }
    }

    public boolean checkLogin(String username, String password) {
        String query = "SELECT 1 FROM users WHERE username = ? AND password = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, username);
            stmt.setString(2, password); // ⚠️ hash later
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.err.println("[DB] Login check failed:");
            e.printStackTrace();
            return false;
        }
    }

    public boolean createUser(String username, String password) {
        String insert = "INSERT INTO users (username, password) VALUES (?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(insert)) {
            stmt.setString(1, username);
            stmt.setString(2, password); // hash later!
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            if (e.getSQLState().equals("23505")) { // unique_violation
                System.err.println("[DB] Username already exists!");
            } else {
                System.err.println("[DB] User creation failed:");
                e.printStackTrace();
            }
            return false;
        }
    }

    public Connection getConnection() {
        return connection;
    }
}
