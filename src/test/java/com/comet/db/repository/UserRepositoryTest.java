package com.comet.db.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class UserRepositoryTest {

    @Mock
    private Connection mockConnection;

    @Mock
    private PreparedStatement mockPreparedStatement;

    @Mock
    private ResultSet mockResultSet;

    private UserRepository userRepository;

    @BeforeEach
    public void setUp() throws SQLException {
        MockitoAnnotations.openMocks(this);
        when(mockConnection.prepareStatement(any(String.class))).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        userRepository = new UserRepository(mockConnection);
    }

    @Test
    public void testCheckLoginSuccess() throws SQLException {
        when(mockResultSet.next()).thenReturn(true);

        boolean loginSuccess = userRepository.checkLogin("testUser", "password");
        assertTrue(loginSuccess);
    }

    @Test
    public void testCheckLoginFailure() throws SQLException {
        when(mockResultSet.next()).thenReturn(false);

        boolean loginSuccess = userRepository.checkLogin("testUser", "wrongPassword");
        assertFalse(loginSuccess);
    }

    @Test
    public void testCheckLoginSQLException() throws SQLException {
        when(mockConnection.prepareStatement(any(String.class))).thenThrow(new SQLException("DB error"));
        boolean loginSuccess = userRepository.checkLogin("testUser", "password");
        assertFalse(loginSuccess);
    }

    @Test
    public void testCreateUserSuccess() throws SQLException {
        boolean userCreated = userRepository.createUser("newUser", "New User", "password");
        assertTrue(userCreated);
        verify(mockPreparedStatement).executeUpdate();
    }

    @Test
    public void testCreateUserFailure() throws SQLException {
        doThrow(new SQLException("Duplicate", "23505", 1)).when(mockPreparedStatement).executeUpdate();

        boolean userCreated = userRepository.createUser("existingUser", "Existing User", "password");
        assertFalse(userCreated);
    }

    @Test
    public void testCreateUserSQLException() throws SQLException {
        doThrow(new SQLException("Other error", "99999", 1)).when(mockPreparedStatement).executeUpdate();
        boolean userCreated = userRepository.createUser("failUser", "Fail User", "password");
        assertFalse(userCreated);
    }

    @Test
    public void testGetUserId() throws SQLException {
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt("id")).thenReturn(1);

        int userId = userRepository.getUserId("testUser", "password");
        assertEquals(1, userId);
    }

    @Test
    public void testGetUserIdNotFound() throws SQLException {
        when(mockResultSet.next()).thenReturn(false);
        int userId = userRepository.getUserId("testUser", "password");
        assertEquals(-1, userId);
    }

    @Test
    public void testGetUserIdSQLException() throws SQLException {
        when(mockConnection.prepareStatement(any(String.class))).thenThrow(new SQLException("DB error"));
        int userId = userRepository.getUserId("testUser", "password");
        assertEquals(-1, userId);
    }

    @Test
    public void testGetUserIdByDisplayName() throws SQLException {
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt("id")).thenReturn(1);

        int userId = userRepository.getUserIdByDisplayName("Test User");
        assertEquals(1, userId);
    }

    @Test
    public void testGetUserIdByDisplayNameNotFound() throws SQLException {
        when(mockResultSet.next()).thenReturn(false);
        int userId = userRepository.getUserIdByDisplayName("No User");
        assertEquals(-1, userId);
    }

    @Test
    public void testGetUserIdByDisplayNameSQLException() throws SQLException {
        when(mockConnection.prepareStatement(any(String.class))).thenThrow(new SQLException("DB error"));
        int userId = userRepository.getUserIdByDisplayName("No User");
        assertEquals(-1, userId);
    }

    @Test
    public void testGetUserIdByUsername() throws SQLException {
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt("id")).thenReturn(1);

        int userId = userRepository.getUserIdByUsername("testUser");
        assertEquals(1, userId);
    }

    @Test
    public void testGetUserIdByUsernameNotFound() throws SQLException {
        when(mockResultSet.next()).thenReturn(false);
        int userId = userRepository.getUserIdByUsername("noUser");
        assertEquals(-1, userId);
    }

    @Test
    public void testGetUserIdByUsernameSQLException() throws SQLException {
        when(mockConnection.prepareStatement(any(String.class))).thenThrow(new SQLException("DB error"));
        int userId = userRepository.getUserIdByUsername("noUser");
        assertEquals(-1, userId);
    }

    @Test
    public void testUpdateUserProfile() throws SQLException {
        userRepository.updateUserProfile(1, "New Display Name", "http://newimage.com/image.jpg");
        verify(mockPreparedStatement).executeUpdate();
    }

    @Test
    public void testUpdateUserProfileSQLException() throws SQLException {
        when(mockConnection.prepareStatement(any(String.class))).thenThrow(new SQLException("DB error"));
        assertDoesNotThrow(() -> userRepository.updateUserProfile(1, "Name", "url"));
    }
}
