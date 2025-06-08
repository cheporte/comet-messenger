package com.comet.db.schema;

import com.comet.db.DatabaseManager;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.logging.Level;
import java.util.logging.Logger;

public class SchemaInitializer {
    private static final Logger logger = Logger.getLogger(SchemaInitializer.class.getName());

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

        String privateChatsTable = """
        CREATE TABLE IF NOT EXISTS private_chats (
            id SERIAL PRIMARY KEY,
            user1_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
            user2_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            UNIQUE (user1_id, user2_id)
        )
        """;

        String groupChatsTable = """
        CREATE TABLE IF NOT EXISTS group_chats (
            id SERIAL PRIMARY KEY,
            name VARCHAR(255),
            created_by INTEGER REFERENCES users(id) ON DELETE SET NULL,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
        """;

        String groupMembersTable = """
        CREATE TABLE IF NOT EXISTS group_members (
            id SERIAL PRIMARY KEY,
            group_id INTEGER REFERENCES group_chats(id) ON DELETE CASCADE,
            user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
            joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            UNIQUE (group_id, user_id)
        )
        """;

        String messagesTable = """
        CREATE TABLE IF NOT EXISTS messages (
            id SERIAL PRIMARY KEY,
            sender_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
            private_chat_id INTEGER REFERENCES private_chats(id) ON DELETE CASCADE,
            group_chat_id INTEGER REFERENCES group_chats(id) ON DELETE CASCADE,
            content TEXT NOT NULL,
            timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            CHECK (
                (private_chat_id IS NOT NULL AND group_chat_id IS NULL) OR
                (group_chat_id IS NOT NULL AND private_chat_id IS NULL)
            )
        )
        """;

        // Ensure the schema is created
        logger.log(Level.INFO, "[DB] Ensuring schema...");
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(usersTable);
            stmt.execute(contactsTable);
            stmt.execute(privateChatsTable);
            stmt.execute(groupChatsTable);
            stmt.execute(groupMembersTable);
            stmt.execute(messagesTable);
            
            logger.log(Level.INFO, "[DB] Schema initialized successfully.");
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "[DB] Schema init failed:", e);
        }
    }
}
