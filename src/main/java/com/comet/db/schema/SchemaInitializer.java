package com.comet.db.schema;

import com.comet.db.DatabaseManager;
import com.comet.demo.core.client.ChatClient;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.logging.Level;
import java.util.logging.Logger;

public class SchemaInitializer {
    private static final Logger logger = Logger.getLogger(ChatClient.class.getName());

    public static void init() throws SQLException {
        Connection connection = DatabaseManager.getInstance().getConnection();

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

        // Ensure the schema is created
        logger.log(Level.INFO, "[DB] Ensuring schema...");
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(usersTable);
            stmt.execute(contactsTable);
            stmt.execute(chatsTable);
            stmt.execute(chatMembersTable);
            stmt.execute(messagesTable);
            
            logger.log(Level.INFO, "[DB] Schema initialized successfully.");
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "[DB] Schema init failed:", e);
        }
    }
}
