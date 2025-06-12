package com.comet.db.repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.comet.db.DatabaseManager;

public class ContactRepository {
    private static final Logger logger = Logger.getLogger(ContactRepository.class.getName());
    
    private final Connection connection;

    /**
     * Default constructor that initializes the ContactRepository with a database connection
     * from the DatabaseManager singleton. Throws a RuntimeException if the connection fails.
     */
    public ContactRepository() {
        try {
            this.connection = DatabaseManager.getInstance().getConnection();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "[ContactRepo] Failed to get database connection:", e);
            throw new RuntimeException("Database connection error", e);
        }
    }

    /**
     * Constructs a ContactRepository with the given database connection.
     *
     * @param connection the SQL connection to use for database operations
     */
    public ContactRepository(Connection connection) {
        this.connection = connection;
    }

    /**
     * Adds a contact for the specified user.
     *
     * @param userId the ID of the user
     * @param contactId the ID of the contact to add
     * @throws SQLException if a database access error occurs
     */
    public void addContact(int userId, int contactId) throws SQLException {
        String insert = "INSERT INTO contacts (user_id, contact_id) VALUES (?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(insert)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, contactId);
            stmt.executeUpdate();
            
            logger.log(Level.INFO, "[ContactRepo] Added contact: {0} for user: {1}", new Object[]{contactId, userId});
        }
    }

    /**
     * Removes a contact for the specified user. Also removes the reciprocal contact if it exists.
     *
     * @param userId the ID of the user
     * @param contactId the ID of the contact to remove
     * @throws SQLException if a database access error occurs
     */
    public void removeContact(int userId, int contactId) throws SQLException {
        String delete = "DELETE FROM contacts WHERE (user_id = ? AND contact_id = ?) OR (user_id = ? AND contact_id = ?)";
        try (PreparedStatement stmt = connection.prepareStatement(delete)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, contactId);
            stmt.setInt(3, contactId);
            stmt.setInt(4, userId);
            stmt.executeUpdate();
            logger.log(Level.INFO, "[ContactRepo] Removed contact: {0} and reciprocal for user: {1}", new Object[]{contactId, userId});
        }
    }

    /**
     * Retrieves a list of contact IDs for the specified user.
     *
     * @param userId the ID of the user whose contacts are to be retrieved
     * @return a list of contact IDs
     * @throws SQLException if a database access error occurs
     */
    public List<Integer> getContacts(int userId) throws SQLException {
        String query = "SELECT contact_id FROM contacts WHERE user_id = ?";
        List<Integer> contacts = new ArrayList<>();

        logger.log(Level.INFO, "[ContactRepo] Retrieving contacts for user: {0}", userId);
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                logger.log(Level.INFO, "[ContactRepo] Found contact: {0} for user: {1}", new Object[]{rs.getInt("contact_id"), userId});
                contacts.add(rs.getInt("contact_id"));
            }
        }

        logger.log(Level.INFO, "[ContactRepo] Retrieved contacts for user: {0}", userId);
        return contacts;
    }

    /**
     * Retrieves a list of contact display names for the specified user.
     *
     * @param userId the ID of the user whose contact names are to be retrieved
     * @return a list of contact display names
     */
    public List<String> getContactNamesForUser(int userId) {
        List<String> contacts = new ArrayList<>();
        String query = "SELECT u.display_name FROM contacts c JOIN users u ON c.contact_id = u.id WHERE c.user_id = ?";
        
        logger.log(Level.INFO, "[ContactRepo] Retrieving contact names for user: {0}", userId);
        try (
            PreparedStatement stmt = connection.prepareStatement(query)
        ) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                logger.log(Level.INFO, "[ContactRepo] Found contact name: {0} for user: {1}", new Object[]{rs.getString("display_name"), userId});
                contacts.add(rs.getString("display_name"));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "[ContactRepo] Error retrieving contact names for user: {0}", userId);
        }
        return contacts;
    }
}
