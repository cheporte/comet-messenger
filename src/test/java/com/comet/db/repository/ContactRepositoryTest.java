package com.comet.db.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ContactRepositoryTest {

    @Mock
    private Connection mockConnection;

    @Mock
    private PreparedStatement mockPreparedStatement;

    @Mock
    private ResultSet mockResultSet;

    private ContactRepository contactRepository;

    @BeforeEach
    public void setUp() throws SQLException {
        MockitoAnnotations.openMocks(this);
        when(mockConnection.prepareStatement(any(String.class))).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        contactRepository = new ContactRepository(mockConnection);
    }

    @Test
    public void testAddContact() throws SQLException {
        contactRepository.addContact(1, 2);
        verify(mockPreparedStatement).executeUpdate();
    }

    @Test
    public void testGetContacts() throws SQLException {
        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getInt("contact_id")).thenReturn(2);

        List<Integer> contacts = contactRepository.getContacts(1);
        assertEquals(1, contacts.size());
        assertEquals(2, contacts.getFirst());
    }

    @Test
    public void testGetContactNamesForUser() throws SQLException {
        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getString("display_name")).thenReturn("Test User");

        List<String> contactNames = contactRepository.getContactNamesForUser(1);
        assertEquals(1, contactNames.size());
        assertEquals("Test User", contactNames.getFirst());
    }
}
