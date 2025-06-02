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
    public void testGetChatId() throws SQLException {
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt("id")).thenReturn(1);

        int chatId = chatRepository.getChatId("Test Chat");
        assertEquals(1, chatId);
    }

    @Test
    public void testGetPrivateChatId() throws SQLException {
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt("id")).thenReturn(1);

        int chatId = chatRepository.getPrivateChatId(1, 2);
        assertEquals(1, chatId);
    }

    @Test
    public void testAddUserToChat() throws SQLException {
        chatRepository.addUserToChat(1, 1);
        verify(mockPreparedStatement).executeUpdate();
    }

    @Test
    public void testSendMessage() throws SQLException {
        chatRepository.sendMessage(1, 1, "Hello");
        verify(mockPreparedStatement).executeUpdate();
    }

    @Test
    public void testGetMessages() throws SQLException {
        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getString("content")).thenReturn("Hello");

        List<String> messages = chatRepository.getMessages(1);
        assertEquals(1, messages.size());
        assertEquals("Hello", messages.getFirst());
    }
}
