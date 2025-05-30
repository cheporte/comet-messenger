package com.comet.db;

import java.sql.*;

public class DatabaseManager {
    private static DatabaseManager instance;
    private Connection connection;

    private static final String DB_URL = System.getenv("COMET_DB_URL");
    private static final String DB_USER = System.getenv("COMET_DB_USER");
    private static final String DB_PASS = System.getenv("COMET_DB_PASS"); // haven't come up with env handling yet

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
        String usersTable = """
        CREATE TABLE IF NOT EXISTS users (
            id SERIAL PRIMARY KEY,
            username VARCHAR(255) UNIQUE NOT NULL,
            password VARCHAR(255) NOT NULL
        )
    """;

        String contactsTable = """
        CREATE TABLE IF NOT EXISTS contacts (
            id SERIAL PRIMARY KEY,
            user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
            contact_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
            UNIQUE (user_id, contact_id)
        )
    """;

        String chatsTable = """
        CREATE TABLE IF NOT EXISTS chats (
            id SERIAL PRIMARY KEY,
            name VARCHAR(255),
            is_group BOOLEAN DEFAULT FALSE,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
    """;

        String chatMembersTable = """
        CREATE TABLE IF NOT EXISTS chat_members (
            id SERIAL PRIMARY KEY,
            chat_id INTEGER REFERENCES chats(id) ON DELETE CASCADE,
            user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
            joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            UNIQUE (chat_id, user_id)
        )
    """;

        String messagesTable = """
        CREATE TABLE IF NOT EXISTS messages (
            id SERIAL PRIMARY KEY,
            chat_id INTEGER REFERENCES chats(id) ON DELETE CASCADE,
            sender_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
            content TEXT NOT NULL,
            timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
    """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(usersTable);
            stmt.execute(contactsTable);
            stmt.execute(chatsTable);
            stmt.execute(chatMembersTable);
            stmt.execute(messagesTable);
            System.out.println("[DB] Schema ensured ✅ with group chat support 🚀");
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
