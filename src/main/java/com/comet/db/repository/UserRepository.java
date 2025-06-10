package com.comet.db.repository;

import com.comet.db.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.util.logging.Level;
import java.util.logging.Logger;

public class UserRepository {
    private static final Logger logger = Logger.getLogger(UserRepository.class.getName());

    private final Connection connection;

    /**
     * Default constructor that initializes the UserRepository with a database connection
     * from the DatabaseManager singleton. Throws a RuntimeException if the connection fails.
     */
    public UserRepository() {
        try {
            this.connection = DatabaseManager.getInstance().getConnection();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "[UserRepo] Failed to get database connection:", e);
            throw new RuntimeException("Database connection error", e);
        }
    }

    /**
     * Constructs a UserRepository with the given database connection.
     *
     * @param connection the SQL connection to use for database operations
     */
    public UserRepository(Connection connection) {
        this.connection = connection;
    }

    /**
     * Hashes a password using SHA-256. In production, use bcrypt or Argon2 for better security.
     */
    private static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /**
     * Checks if the provided username and password combination exists in the users table.
     *
     * @param username the username to check
     * @param password the password to check (should be hashed in production)
     * @return true if the credentials are valid, false otherwise
     */
    public boolean checkLogin(String username, String password) {
        String query = "SELECT 1 FROM users WHERE username = ? AND password = ?";
        try (
            PreparedStatement stmt = connection.prepareStatement(query)
        ) {
            stmt.setString(1, username);
            stmt.setString(2, hashPassword(password)); // hash password
            ResultSet rs = stmt.executeQuery();

            logger.log(Level.INFO, "[UserRepo] Checking login for user: {0}", username);

            return rs.next();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "[UserRepo] Login check failed:", e);
            return false;
        }
    }

    /**
     * Creates a new user in the users table with the given username, display name, and password.
     *
     * @param username the username for the new user
     * @param displayName the display name for the new user
     * @param password the password for the new user (should be hashed in production)
     * @return true if the user was created successfully, false if the username already exists or an error occurred
     */
    public boolean createUser(String username, String displayName, String password) {
        String insert = "INSERT INTO users (username, display_name, password) VALUES (?, ?, ?)";
        try (
            PreparedStatement stmt = connection.prepareStatement(insert)
        ) {
            stmt.setString(1, username);
            stmt.setString(2, displayName);
            stmt.setString(3, hashPassword(password)); // hash password
            stmt.executeUpdate();

            logger.log(Level.INFO, "[UserRepo] User created successfully: {0}", username);

            return true;
        } catch (SQLException e) {
            if ("23505".equals(e.getSQLState())) {
                logger.log(Level.WARNING, "[UserRepo] Username exists: {0}", username);
            } else {
                logger.log(Level.SEVERE, "[UserRepo] User creation failed:", e);
            }
            return false;
        }
    }

    /**
     * Retrieves the user ID for the given username and password.
     *
     * @param username the username of the user
     * @param password the password of the user (should be hashed in production)
     * @return the user ID if found, or -1 if not found or an error occurred
     */
    public int getUserId(String username, String password) {
        String query = "SELECT id FROM users WHERE username = ? AND password = ?";
        try (
            PreparedStatement stmt = connection.prepareStatement(query)
        ) {
            stmt.setString(1, username);
            stmt.setString(2, hashPassword(password)); // hash password
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1; // Return -1 or handle error appropriately
    }

    /**
     * Retrieves the user ID for the given display name.
     *
     * @param displayName the display name of the user
     * @return the user ID if found, or -1 if not found
     */
    public int getUserIdByDisplayName(String displayName) {
        String query = "SELECT id FROM users WHERE display_name = ?";
        try (
            PreparedStatement stmt = connection.prepareStatement(query)
        ) {
            stmt.setString(1, displayName);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1; // Return -1 if user not found
    }

    /**
     * Retrieves the user ID for the given username.
     *
     * @param username the username of the user
     * @return the user ID if found, or -1 if not found
     */
    public int getUserIdByUsername(String username) {
        String query = "SELECT id FROM users WHERE username = ?";
        try (
            PreparedStatement stmt = connection.prepareStatement(query)
        ) {
            logger.log(Level.INFO, "[UserRepo] Retrieving user ID for username: {0}", username);
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                logger.log(Level.INFO, "[UserRepo] Found user ID: {0} for username: {1}", new Object[]{rs.getInt("id"), username});
                return rs.getInt("id");
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "[UserRepo] Error retrieving user ID for username: " + username, e);
        }
        return -1; // Return -1 if user not found
    }

    /**
     * Updates the user profile with the given display name and image URL for the specified user ID.
     *
     * @param userId the ID of the user to update
     * @param displayName the new display name
     * @param imageUrl the new image URL
     */
    public void updateUserProfile(int userId, String displayName, String imageUrl) {
        // Fetch current values if fields are empty
        String currentDisplayName = displayName;
        String currentImageUrl = imageUrl;
        if (displayName == null || displayName.isEmpty() || imageUrl == null || imageUrl.isEmpty()) {
            String query = "SELECT display_name, image_url FROM users WHERE id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setInt(1, userId);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    if (displayName == null || displayName.isEmpty()) {
                        currentDisplayName = rs.getString("display_name");
                    }
                    if (imageUrl == null || imageUrl.isEmpty()) {
                        currentImageUrl = rs.getString("image_url");
                    }
                }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "[UserRepo] Error fetching current user profile for user ID: " + userId, e);
                return;
            }
        }
        String query = "UPDATE users SET display_name = ?, image_url = ? WHERE id = ?";
        try (
            PreparedStatement stmt = connection.prepareStatement(query)
        ) {
            logger.log(Level.INFO, "[UserRepo] Updating user profile for user ID: {0}", userId);
            stmt.setString(1, currentDisplayName);
            stmt.setString(2, currentImageUrl);
            stmt.setInt(3, userId);
            stmt.executeUpdate();
            logger.log(Level.INFO, "[UserRepo] User profile updated successfully for user ID: {0}", userId);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "[UserRepo] Error updating user profile for user ID: " + userId, e);
        }
    }

    /**
     * Retrieves the image URL for the given user ID.
     *
     * @param userId the ID of the user
     * @return the image URL if found, or null if not found or error
     */
    public String getUserImageUrl(int userId) {
        String query = "SELECT image_url FROM users WHERE id = ?";
        try (
            PreparedStatement stmt = connection.prepareStatement(query)
        ) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("image_url");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
