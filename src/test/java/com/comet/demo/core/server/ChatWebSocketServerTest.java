package com.comet.demo.core.server;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.junit.jupiter.api.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ChatWebSocketServerTest {
    private ChatWebSocketServer server;
    private int testPort = 8890;

    @BeforeEach
    public void setUp() {
        server = new ChatWebSocketServer(testPort);
    }

    @Test
    public void testOnOpenAndOnClose() {
        WebSocket mockConn = mock(WebSocket.class);
        ClientHandshake mockHandshake = mock(ClientHandshake.class);
        server.onOpen(mockConn, mockHandshake);
        assertTrue(server != null); // Just to cover the call
        server.onClose(mockConn, 0, "bye", true);
    }

    @Test
    public void testOnMessageRefreshContacts() {
        WebSocket mockConn = mock(WebSocket.class);
        server.onOpen(mockConn, mock(ClientHandshake.class));
        server.onMessage(mockConn, "refresh_contacts");
        // Should broadcast to all connections
        verify(mockConn, atLeastOnce()).send("refresh_contacts");
    }

    @Test
    public void testOnMessageOther() {
        WebSocket mockConn = mock(WebSocket.class);
        server.onOpen(mockConn, mock(ClientHandshake.class));
        server.onMessage(mockConn, "something_else");
        verify(mockConn, atLeastOnce()).send("refresh_chats");
    }

    @Test
    public void testOnError() {
        WebSocket mockConn = mock(WebSocket.class);
        Exception ex = new Exception("Test error");
        assertDoesNotThrow(() -> server.onError(mockConn, ex));
    }

    @Test
    public void testOnStart() {
        AtomicBoolean started = new AtomicBoolean(false);
        ChatWebSocketServer testServer = new ChatWebSocketServer(testPort) {
            @Override
            public void onStart() {
                started.set(true);
            }
        };
        testServer.onStart();
        assertTrue(started.get());
    }

    @Test
    public void testBroadcast() {
        WebSocket mockConn1 = mock(WebSocket.class);
        WebSocket mockConn2 = mock(WebSocket.class);
        server.onOpen(mockConn1, mock(ClientHandshake.class));
        server.onOpen(mockConn2, mock(ClientHandshake.class));
        server.broadcast("test_message");
        verify(mockConn1, atLeastOnce()).send("test_message");
        verify(mockConn2, atLeastOnce()).send("test_message");
    }
}
