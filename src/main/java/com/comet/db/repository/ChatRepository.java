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
     * Creates a new chat with the specified name and group status.
     *
     * @param name the name of the chat
     * @param isGroup true if the chat is a group chat, false otherwise
     * @return the ID of the newly created chat
     * @throws SQLException if a database access error occurs
     */
    public int createChat(String name, boolean isGroup) throws SQLException {
        String insert = "INSERT INTO chats (name, is_group) VALUES (?, ?) RETURNING id";
        
        logger.log(Level.INFO, "[ChatRepo] Creating chat: {0}, isGroup: {1}", new Object[]{name, isGroup});
        try (PreparedStatement stmt = connection.prepareStatement(insert)) {
            stmt.setString(1, name);
            stmt.setBoolean(2, isGroup);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int chatId = rs.getInt(1);
                logger.log(Level.INFO, "[ChatRepo] Created chat with ID: {0}", chatId);
                return chatId;
            } else {
                logger.log(Level.SEVERE, "[ChatRepo] Failed to create chat, no ID returned.");
                throw new SQLException("No chat ID returned on creation.");
            }
        }
    }

    /**
     * Retrieves the list of chat names for a given user.
     *
     * @param userId the ID of the user
     * @return a list of chat names the user is a member of
     */
    public List<String> getChatsForUser(int userId) {
        List<String> chats = new ArrayList<>();
        String query = "SELECT c.name FROM chats c JOIN chat_members cm ON c.id = cm.chat_id WHERE cm.user_id = ?";
        
        try (
            PreparedStatement stmt = connection.prepareStatement(query)
        ) {
            logger.log(Level.INFO, "[ChatRepo] Retrieving chats for user: {0}", userId);
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                logger.log(Level.INFO, "[ChatRepo] Found chat: {0} for user: {1}", new Object[]{rs.getString("name"), userId});
                chats.add(rs.getString("name"));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "[ChatRepo] Error retrieving chats for user: " + userId, e);
        }
        return chats;
    }

    /**
     * Retrieves the chat ID for a given chat name.
     *
     * @param chatName the name of the chat
     * @return the chat ID if found, or -1 if not found or an error occurs
     */
    public int getChatId(String chatName) {
        String query = "SELECT id FROM chats WHERE name = ?";
        
        try (
            PreparedStatement stmt = connection.prepareStatement(query)
        ) {
            logger.log(Level.INFO, "[ChatRepo] Retrieving chat ID for chat name: {0}", chatName);
            stmt.setString(1, chatName);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                logger.log(Level.INFO, "[ChatRepo] Found chat ID: {0} for chat name: {1}", new Object[]{rs.getInt("id"), chatName});
                return rs.getInt("id");
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "[ChatRepo] Error retrieving chat ID for name: " + chatName, e);
        }
        return -1; // Return -1 or handle error appropriately
    }

    /**
     * Retrieves the private chat ID for two users, or creates one if it does not exist.
     *
     * @param userId1 the ID of the first user
     * @param userId2 the ID of the second user
     * @return the private chat ID if found or created, or -1 if an error occurs
     */
    public int getPrivateChatId(int userId1, int userId2) {
        String query = "SELECT id FROM chats WHERE is_group = FALSE AND id IN " +
                "(SELECT chat_id FROM chat_members WHERE user_id = ?) AND id IN " +
                "(SELECT chat_id FROM chat_members WHERE user_id = ?)";
        
        try (
            PreparedStatement stmt = connection.prepareStatement(query)
        ) {
            logger.log(Level.INFO, "[ChatRepo] Retrieving private chat ID for users: {0} and {1}", new Object[]{userId1, userId2});
            stmt.setInt(1, userId1);
            stmt.setInt(2, userId2);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                logger.log(Level.INFO, "[ChatRepo] Found existing private chat ID: {0} for users: {1} and {2}", new Object[]{rs.getInt("id"), userId1, userId2});
                return rs.getInt("id");
            } else {
                // If no private chat exists, create one
                logger.log(Level.INFO, "[ChatRepo] No existing private chat found for users: {0} and {1}. Creating new chat.", new Object[]{userId1, userId2});
                return createPrivateChat(userId1, userId2);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "[ChatRepo] Error retrieving private chat ID:", e);
        }
        return -1; // Return -1 if an error occurs
    }

    /**
     * Creates a private chat for two users and adds them as members.
     *
     * @param userId1 the ID of the first user
     * @param userId2 the ID of the second user
     * @return the chat ID if created successfully, or -1 if an error occurs
     */
    private int createPrivateChat(int userId1, int userId2) {
        try {
            int chatId = createChat("Private Chat", false);
            
            logger.log(Level.INFO, "[ChatRepo] Creating private chat for users: {0} and {1}, chat ID: {2}", new Object[]{userId1, userId2, chatId});
            addUserToChat(chatId, userId1);
            addUserToChat(chatId, userId2);
            
            return chatId;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "[ChatRepo] Error creating private chat:", e);
        }
        return -1; // Return -1 if an error occurs
    }

    /**
     * Adds a user to a chat.
     *
     * @param chatId the ID of the chat
     * @param userId the ID of the user to add
     * @throws SQLException if a database access error occurs
     */
    public void addUserToChat(int chatId, int userId) throws SQLException {
        String insert = "INSERT INTO chat_members (chat_id, user_id) VALUES (?, ?)";
        
        logger.log(Level.INFO, "[ChatRepo] Adding user: {0} to chat: {1}", new Object[]{userId, chatId});
        try (PreparedStatement stmt = connection.prepareStatement(insert)) {
            stmt.setInt(1, chatId);
            stmt.setInt(2, userId);
            stmt.executeUpdate();

            logger.log(Level.INFO, "[ChatRepo] User: {0} added to chat: {1}", new Object[]{userId, chatId});
        }
    }

    /**
     * Retrieves the list of user IDs who are members of the specified chat.
     *
     * @param chatId the ID of the chat
     * @return a list of user IDs who are members of the chat
     * @throws SQLException if a database access error occurs
     */
    public List<Integer> getChatMembers(int chatId) throws SQLException {
        String query = "SELECT user_id FROM chat_members WHERE chat_id = ?";
        List<Integer> members = new ArrayList<>();

        logger.log(Level.INFO, "[ChatRepo] Retrieving members for chat: {0}", chatId);
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, chatId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                logger.log(Level.INFO, "[ChatRepo] Found member: {0} in chat: {1}", new Object[]{rs.getInt("user_id"), chatId});
                members.add(rs.getInt("user_id"));
            }
        }

        logger.log(Level.INFO, "[ChatRepo] Retrieved members for chat: {0}", chatId);
        return members;
    }

    /**
     * Sends a message in the specified chat from the given sender.
     *
     * @param chatId the ID of the chat
     * @param senderId the ID of the user sending the message
     * @param content the content of the message
     */
    public void sendMessage(int chatId, int senderId, String content) {
        String insert = "INSERT INTO messages (chat_id, sender_id, content) VALUES (?, ?, ?)";

        logger.log(Level.INFO, "[ChatRepo] Sending message in chat: {0}, from user: {1}", new Object[]{chatId, senderId});
        try (PreparedStatement stmt = connection.prepareStatement(insert)) {
            stmt.setInt(1, chatId);
            stmt.setInt(2, senderId);
            stmt.setString(3, content);
            stmt.executeUpdate();

            logger.log(Level.INFO, "[ChatRepo] Message sent in chat: {0}, from user: {1}", new Object[]{chatId, senderId});
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "[ChatRepo] Error sending message in chat: " + chatId + ", from user: " + senderId, e);
        }
    }

    /**
     * Retrieves the list of messages for the specified chat, ordered by timestamp.
     *
     * @param chatId the ID of the chat
     * @return a list of message contents for the chat
     * @throws SQLException if a database access error occurs
     */
    public List<String> getMessages(int chatId) throws SQLException {
        String query = "SELECT content FROM messages WHERE chat_id = ? ORDER BY timestamp";
        List<String> messages = new ArrayList<>();


        logger.log(Level.INFO, "[ChatRepo] Retrieving messages for chat: {0}", chatId);
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, chatId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                logger.log(Level.INFO, "[ChatRepo] Found message in chat: {0}, content: {1}", new Object[]{chatId, rs.getString("content")});
                messages.add(rs.getString("content"));
            }
        }

        logger.log(Level.INFO, "[ChatRepo] Retrieved messages for chat: {0}", chatId);
        return messages;
    }
    
}
