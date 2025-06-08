package com.comet.db.repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.comet.db.DatabaseManager;

public class ChatRepository {
    private static final Logger logger = Logger.getLogger(ChatRepository.class.getName());

    private final Connection connection;

    /**
     * Default constructor that initializes the ChatRepository with a database connection
     * from the DatabaseManager singleton. Throws a RuntimeException if the connection fails.
     */
    public ChatRepository() {
        try {
            this.connection = DatabaseManager.getInstance().getConnection();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "[ChatRepo] Failed to get database connection:", e);
            throw new RuntimeException("Database connection error", e);
        }
    }

    /**
     * Constructs a ChatRepository with the given database connection.
     *
     * @param connection the SQL connection to use for database operations
     */
    public ChatRepository(Connection connection) {
        this.connection = connection;
    }

    /**
     * Creates a new private chat between two users. Returns the chat ID.
     */
    public int createPrivateChat(int user1Id, int user2Id) throws SQLException {
        String insert = "INSERT INTO private_chats (user1_id, user2_id) VALUES (?, ?) RETURNING id";
        try (PreparedStatement stmt = connection.prepareStatement(insert)) {
            stmt.setInt(1, user1Id);
            stmt.setInt(2, user2Id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        }
        throw new SQLException("Failed to create private chat");
    }

    /**
     * Gets the private chat ID for two users, or -1 if not found.
     */
    public int getPrivateChatId(int user1Id, int user2Id) throws SQLException {
        String query = "SELECT id FROM private_chats WHERE (user1_id = ? AND user2_id = ?) OR (user1_id = ? AND user2_id = ?)";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, user1Id);
            stmt.setInt(2, user2Id);
            stmt.setInt(3, user2Id);
            stmt.setInt(4, user1Id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        }
        return -1;
    }

    /**
     * Creates a new group chat and returns its ID.
     */
    public int createGroupChat(String name, int creatorId) throws SQLException {
        String insert = "INSERT INTO group_chats (name, created_by) VALUES (?, ?) RETURNING id";
        try (PreparedStatement stmt = connection.prepareStatement(insert)) {
            stmt.setString(1, name);
            stmt.setInt(2, creatorId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        }
        throw new SQLException("Failed to create group chat");
    }

    /**
     * Adds a user to a group chat.
     */
    public void addUserToGroup(int groupId, int userId) throws SQLException {
        String insert = "INSERT INTO group_members (group_id, user_id) VALUES (?, ?) ON CONFLICT DO NOTHING";
        try (PreparedStatement stmt = connection.prepareStatement(insert)) {
            stmt.setInt(1, groupId);
            stmt.setInt(2, userId);
            stmt.executeUpdate();
        }
    }

    /**
     * Gets all group chats for a user (by membership).
     */
    public List<String> getGroupChatsForUser(int userId) throws SQLException {
        List<String> groups = new ArrayList<>();
        String query = "SELECT gc.name FROM group_chats gc JOIN group_members gm ON gc.id = gm.group_id WHERE gm.user_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) groups.add(rs.getString(1));
        }
        return groups;
    }

    /**
     * Gets all private chats for a user (returns display names of the other user).
     */
    public List<String> getPrivateChatsForUser(int userId) throws SQLException {
        List<String> chats = new ArrayList<>();
        String query = "SELECT u.display_name FROM private_chats pc JOIN users u ON (u.id = pc.user1_id OR u.id = pc.user2_id) WHERE (pc.user1_id = ? OR pc.user2_id = ?) AND u.id != ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, userId);
            stmt.setInt(3, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) chats.add(rs.getString(1));
        }
        return chats;
    }

    /**
     * Sends a message in a private chat.
     */
    public void sendPrivateMessage(int privateChatId, int senderId, String content) throws SQLException {
        String insert = "INSERT INTO messages (sender_id, private_chat_id, content) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(insert)) {
            stmt.setInt(1, senderId);
            stmt.setInt(2, privateChatId);
            stmt.setString(3, content);
            stmt.executeUpdate();
        }
    }

    /**
     * Sends a message in a group chat.
     */
    public void sendGroupMessage(int groupChatId, int senderId, String content) throws SQLException {
        String insert = "INSERT INTO messages (sender_id, group_chat_id, content) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(insert)) {
            stmt.setInt(1, senderId);
            stmt.setInt(2, groupChatId);
            stmt.setString(3, content);
            stmt.executeUpdate();
        }
    }

    /**
     * Gets all messages for a private chat.
     */
    public List<String> getPrivateMessages(int privateChatId) throws SQLException {
        List<String> messages = new ArrayList<>();
        String query = "SELECT u.display_name, m.content FROM messages m JOIN users u ON m.sender_id = u.id WHERE m.private_chat_id = ? ORDER BY m.timestamp";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, privateChatId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) messages.add(rs.getString(1) + ": " + rs.getString(2));
        }
        return messages;
    }

    /**
     * Gets all messages for a group chat.
     */
    public List<String> getGroupMessages(int groupChatId) throws SQLException {
        List<String> messages = new ArrayList<>();
        String query = "SELECT u.display_name, m.content FROM messages m JOIN users u ON m.sender_id = u.id WHERE m.group_chat_id = ? ORDER BY m.timestamp";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, groupChatId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) messages.add(rs.getString(1) + ": " + rs.getString(2));
        }
        return messages;
    }

    /**
     * Gets the group chat ID for a given group name.
     */
    public int getGroupChatIdByName(String groupName) throws SQLException {
        String query = "SELECT id FROM group_chats WHERE name = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, groupName);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        }
        return -1;
    }
}
