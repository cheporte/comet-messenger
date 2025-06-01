package com.comet.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private static DatabaseManager instance;
    private final HikariDataSource dataSource;
    private Connection connection;

    private static final String DB_URL = System.getenv("COMET_DB_URL");
    private static final String DB_USER = System.getenv("COMET_DB_USER");
    private static final String DB_PASS = System.getenv("COMET_DB_PASS"); // haven't come up with env handling yet

    public DatabaseManager() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(DB_URL);
        config.setUsername(DB_USER);
        config.setPassword(DB_PASS);
        config.setMaximumPoolSize(10); // Set the maximum pool size

        dataSource = new HikariDataSource(config);

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
            password VARCHAR(255) NOT NULL,
            display_name VARCHAR(255) DEFAULT 'Another User',
            image_url VARCHAR(255)
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

    public boolean createUser(String username, String displayName, String password) {
        String insert = "INSERT INTO users (username, display_name, password) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(insert)) {
            stmt.setString(1, username);
            stmt.setString(2, displayName);
            stmt.setString(3, password); // hash later!
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

    public int createChat(String name, boolean isGroup) throws SQLException {
        String insert = "INSERT INTO chats (name, is_group) VALUES (?, ?) RETURNING id";
        try (Connection connection = getConnection();
             PreparedStatement stmt = connection.prepareStatement(insert)) {
            stmt.setString(1, name);
            stmt.setBoolean(2, isGroup);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1); // Return the generated chat ID
            }
        }
        throw new SQLException("Creating chat failed, no ID obtained.");
    }

    public void addUserToChat(int chatId, int userId) throws SQLException {
        String insert = "INSERT INTO chat_members (chat_id, user_id) VALUES (?, ?)";
        try (Connection connection = getConnection();
             PreparedStatement stmt = connection.prepareStatement(insert)) {
            stmt.setInt(1, chatId);
            stmt.setInt(2, userId);
            stmt.executeUpdate();
        }
    }

    public List<Integer> getChatMembers(int chatId) throws SQLException {
        List<Integer> members = new ArrayList<>();
        String query = "SELECT user_id FROM chat_members WHERE chat_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, chatId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                members.add(rs.getInt("user_id"));
            }
        }
        return members;
    }

    public void addContact(int userId, int contactId) throws SQLException {
        String insert = "INSERT INTO contacts (user_id, contact_id) VALUES (?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(insert)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, contactId);
            stmt.executeUpdate();
        }
    }

    public List<Integer> getContacts(int userId) throws SQLException {
        List<Integer> contacts = new ArrayList<>();
        String query = "SELECT contact_id FROM contacts WHERE user_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                contacts.add(rs.getInt("contact_id"));
            }
        }
        return contacts;
    }

    public void sendMessage(int chatId, int senderId, String content) throws SQLException {
        String insert = "INSERT INTO messages (chat_id, sender_id, content) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(insert)) {
            stmt.setInt(1, chatId);
            stmt.setInt(2, senderId);
            stmt.setString(3, content);
            stmt.executeUpdate();
        }
    }

    public List<String> getMessages(int chatId) throws SQLException {
        List<String> messages = new ArrayList<>();
        String query = "SELECT content FROM messages WHERE chat_id = ? ORDER BY timestamp";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, chatId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                messages.add(rs.getString("content"));
            }
        }
        return messages;
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
}
