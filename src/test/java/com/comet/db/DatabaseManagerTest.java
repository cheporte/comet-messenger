package com.comet.db;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseManagerTest {

    @Test
    public void testDatabaseConnection() {
        DatabaseManager dbManager = DatabaseManager.getInstance();
        assertNotNull(dbManager, "DatabaseManager instance should not be null");

        try (Connection connection = dbManager.getConnection()) {
            assertNotNull(connection, "Database connection should not be null");
            assertFalse(connection.isClosed(), "Database connection should be open");
        } catch (SQLException e) {
            fail("Exception should not be thrown when getting a database connection: " + e.getMessage());
        }
    }

    @Test
    public void testSingletonPattern() {
        DatabaseManager firstInstance = DatabaseManager.getInstance();
        DatabaseManager secondInstance = DatabaseManager.getInstance();
        assertSame(firstInstance, secondInstance, "DatabaseManager should be a singleton");
    }
}
