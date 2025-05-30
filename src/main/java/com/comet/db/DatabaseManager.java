package com.comet.db;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private static DatabaseManager instance;
    private final Connection connection;

    private DatabaseManager() {
        try {
            String dbUrl = System.getenv("COMET_DB_URL");
            String dbUser = System.getenv("COMET_DB_USER");
            String dbPass = System.getenv("COMET_DB_PASS");

            connection = DriverManager.getConnection(dbUrl, dbUser, dbPass);
            initSchema();
        } catch (SQLException e) {
            System.err.println("[DB] Failed to connect to database:");
            e.printStackTrace();
            throw new IllegalStateException("Failed to initialize database connection", e);
        }
    }

    public static DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        if (instance.connection == null) {
            throw new IllegalStateException("DB connection is null");
        }
        return instance;
    }

    private void initSchema() throws SQLException {
        String[] tableDefinitions = {
                "CREATE TABLE IF NOT EXISTS users (id SERIAL PRIMARY KEY, username VARCHAR(255) UNIQUE NOT NULL, password VARCHAR(255) NOT NULL)",
                "CREATE TABLE IF NOT EXISTS contacts (id SERIAL PRIMARY KEY, user_id INTEGER REFERENCES users(id) ON DELETE CASCADE, contact_id INTEGER REFERENCES users(id) ON DELETE CASCADE, UNIQUE (user_id, contact_id))",
                "CREATE TABLE IF NOT EXISTS chats (id SERIAL PRIMARY KEY, name VARCHAR(255), is_group BOOLEAN DEFAULT FALSE, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)",
                "CREATE TABLE IF NOT EXISTS chat_members (id SERIAL PRIMARY KEY, chat_id INTEGER REFERENCES chats(id) ON DELETE CASCADE, user_id INTEGER REFERENCES users(id) ON DELETE CASCADE, joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, UNIQUE (chat_id, user_id))",
                "CREATE TABLE IF NOT EXISTS messages (id SERIAL PRIMARY KEY, chat_id INTEGER REFERENCES chats(id) ON DELETE CASCADE, sender_id INTEGER REFERENCES users(id) ON DELETE CASCADE, content TEXT NOT NULL, timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP)"
        };

        try (Statement stmt = connection.createStatement()) {
            for (String tableDefinition : tableDefinitions) {
                stmt.execute(tableDefinition);
            }
            System.out.println("[DB] Schema ensured ✅ with group chat support 🚀");
        }
    }

    public boolean checkLogin(String username, String password) {
        String query = "SELECT 1 FROM users WHERE username = ? AND password = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, username);
            stmt.setString(2, hashPassword(password));
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
            stmt.setString(2, hashPassword(password));
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            if (e.getSQLState().equals("23505")) {
                System.err.println("[DB] Username already exists!");
            } else {
                System.err.println("[DB] User creation failed:");
                e.printStackTrace();
            }
            return false;
        }
    }

    public List<String> searchUsers(String query) {
        List<String> users = new ArrayList<>();
        String sql = "SELECT username FROM users WHERE username LIKE ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, "%" + query + "%");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                users.add(rs.getString("username"));
            }
        } catch (SQLException e) {
            System.err.println("[DB] Search users failed:");
            e.printStackTrace();
        }
        return users;
    }

    public boolean addContact(String username, String contactUsername) {
        String sql = "INSERT INTO contacts (user_id, contact_id) VALUES ((SELECT id FROM users WHERE username = ?), (SELECT id FROM users WHERE username = ?))";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, contactUsername);
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("[DB] Add contact failed:");
            e.printStackTrace();
            return false;
        }
    }

    public boolean removeContact(String username, String contactUsername) {
        String sql = "DELETE FROM contacts WHERE user_id = (SELECT id FROM users WHERE username = ?) AND contact_id = (SELECT id FROM users WHERE username = ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, contactUsername);
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("[DB] Remove contact failed:");
            e.printStackTrace();
            return false;
        }
    }

    public List<String> getChatHistory(String username1, String username2) {
        List<String> chatHistory = new ArrayList<>();
        String sql = """
            SELECT u.username, m.content
            FROM messages m
            JOIN users u ON m.sender_id = u.id
            WHERE m.chat_id IN (
                SELECT c.id
                FROM chats c
                JOIN chat_members cm1 ON c.id = cm1.chat_id
                JOIN chat_members cm2 ON c.id = cm2.chat_id
                JOIN users u1 ON cm1.user_id = u1.id
                JOIN users u2 ON cm2.user_id = u2.id
                WHERE u1.username = ? AND u2.username = ?
            )
            ORDER BY m.timestamp
        """;
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, username1);
            stmt.setString(2, username2);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String sender = rs.getString("username");
                String content = rs.getString("content");
                chatHistory.add(sender + ": " + content);
            }
        } catch (SQLException e) {
            System.err.println("[DB] Get chat history failed:");
            e.printStackTrace();
        }
        return chatHistory;
    }

    public boolean sendMessage(String senderUsername, String receiverUsername, String messageText) {
        String sql = """
            INSERT INTO messages (chat_id, sender_id, content)
            VALUES (
                (SELECT c.id FROM chats c
                 JOIN chat_members cm1 ON c.id = cm1.chat_id
                 JOIN chat_members cm2 ON c.id = cm2.chat_id
                 JOIN users u1 ON cm1.user_id = u1.id
                 JOIN users u2 ON cm2.user_id = u2.id
                 WHERE u1.username = ? AND u2.username = ?),
                (SELECT id FROM users WHERE username = ?),
                ?
            )
        """;
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, senderUsername);
            stmt.setString(2, receiverUsername);
            stmt.setString(3, senderUsername);
            stmt.setString(4, messageText);
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("[DB] Send message failed:");
            e.printStackTrace();
            return false;
        }
    }

    public List<String> getMessagesForChat(int chatId) {
        List<String> messages = new ArrayList<>();
        String query = "SELECT u.username, m.content FROM messages m JOIN users u ON m.sender_id = u.id WHERE m.chat_id = ? ORDER BY m.timestamp";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, chatId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String username = rs.getString("username");
                String content = rs.getString("content");
                messages.add(username + ": " + content);
            }
        } catch (SQLException e) {
            System.err.println("[DB] Get messages for chat failed:");
            e.printStackTrace();
        }
        return messages;
    }

    private String hashPassword(String password) {
        // Implement password hashing using a library like BCrypt
        // Example: return BCrypt.hashpw(password, BCrypt.gensalt());
        return password; // Placeholder
    }

    public List<String> getContacts(String username) {
        List<String> contacts = new ArrayList<>();
        String sql = """
        SELECT u.username
        FROM contacts c
        JOIN users u ON c.contact_id = u.id
        WHERE c.user_id = (SELECT id FROM users WHERE username = ?)
    """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                contacts.add(rs.getString("username"));
            }
        } catch (SQLException e) {
            System.err.println("[DB] Failed to retrieve contacts:");
            e.printStackTrace();
        }
        return contacts;
    }
}