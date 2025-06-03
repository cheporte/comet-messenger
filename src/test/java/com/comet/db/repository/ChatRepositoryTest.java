package com.comet.db.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ChatRepositoryTest {

    @Mock
    private Connection mockConnection;

    @Mock
    private PreparedStatement mockPreparedStatement;

    @Mock
    private ResultSet mockResultSet;

    private ChatRepository chatRepository;

    @BeforeEach
    public void setUp() throws SQLException {
        MockitoAnnotations.openMocks(this);
        when(mockConnection.prepareStatement(any(String.class))).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        chatRepository = new ChatRepository(mockConnection);
    }

    @Test
    public void testCreateChat() throws SQLException {
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt(1)).thenReturn(1);

        int chatId = chatRepository.createChat("Test Chat", false);
        assertEquals(1, chatId);
    }

    @Test
    public void testGetChatsForUser() throws SQLException {
        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getString("name")).thenReturn("Test Chat");

        List<String> chats = chatRepository.getChatsForUser(1);
        assertEquals(1, chats.size());
        assertEquals("Test Chat", chats.getFirst());
    }

    @Test
    public void testGetChatsForUserEmpty() throws SQLException {
        when(mockResultSet.next()).thenReturn(false);
        List<String> chats = chatRepository.getChatsForUser(1);
        assertTrue(chats.isEmpty());
    }

    @Test
    public void testGetChatsForUserSQLException() throws SQLException {
        when(mockConnection.prepareStatement(any(String.class))).thenThrow(new SQLException("DB error"));
        List<String> chats = chatRepository.getChatsForUser(1);
        assertTrue(chats.isEmpty());
    }

    @Test
    public void testGetChatId() throws SQLException {
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt("id")).thenReturn(1);

        int chatId = chatRepository.getChatId("Test Chat");
        assertEquals(1, chatId);
    }

    @Test
    public void testGetChatIdNotFound() throws SQLException {
        when(mockResultSet.next()).thenReturn(false);
        int chatId = chatRepository.getChatId("Nonexistent Chat");
        assertEquals(-1, chatId);
    }

    @Test
    public void testGetChatIdSQLException() throws SQLException {
        when(mockConnection.prepareStatement(any(String.class))).thenThrow(new SQLException("DB error"));
        int chatId = chatRepository.getChatId("Test Chat");
        assertEquals(-1, chatId);
    }

    @Test
    public void testGetPrivateChatId() throws SQLException {
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt("id")).thenReturn(1);

        int chatId = chatRepository.getPrivateChatId(1, 2);
        assertEquals(1, chatId);
    }

    @Test
    public void testGetPrivateChatIdCreatesNewChat() throws SQLException {
        // First call returns false (no chat), then true for createChat
        when(mockResultSet.next()).thenReturn(false).thenReturn(true);
        when(mockResultSet.getInt(1)).thenReturn(42); // For createChat
        // Only need to stub executeUpdate for addUserToChat
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);
        int chatId = chatRepository.getPrivateChatId(1, 2);
        assertEquals(42, chatId);
    }

    @Test
    public void testGetPrivateChatIdSQLException() throws SQLException {
        when(mockConnection.prepareStatement(any(String.class))).thenThrow(new SQLException("DB error"));
        int chatId = chatRepository.getPrivateChatId(1, 2);
        assertEquals(-1, chatId);
    }

    @Test
    public void testGetChatMembersEmpty() throws SQLException {
        when(mockResultSet.next()).thenReturn(false);
        List<Integer> members = chatRepository.getChatMembers(1);
        assertTrue(members.isEmpty());
    }

    @Test
    public void testSendMessage() throws SQLException {
        chatRepository.sendMessage(1, 1, "Hello");
        verify(mockPreparedStatement).executeUpdate();
    }

    @Test
    public void testSendMessageSQLException() throws SQLException {
        when(mockConnection.prepareStatement(any(String.class))).thenThrow(new SQLException("DB error"));
        assertDoesNotThrow(() -> chatRepository.sendMessage(1, 1, "Hello"));
    }

    @Test
    public void testGetMessages() throws SQLException {
        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getString("content")).thenReturn("Hello");

        List<String> messages = chatRepository.getMessages(1);
        assertEquals(1, messages.size());
        assertEquals("Hello", messages.getFirst());
    }

    @Test
    public void testGetMessagesEmpty() throws SQLException {
        when(mockResultSet.next()).thenReturn(false);
        List<String> messages = chatRepository.getMessages(1);
        assertTrue(messages.isEmpty());
    }
}
