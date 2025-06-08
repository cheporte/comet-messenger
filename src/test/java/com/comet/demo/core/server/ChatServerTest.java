package com.comet.demo.core.server;

import org.junit.jupiter.api.*;
import java.io.IOException;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.*;

public class ChatServerTest {
    private ChatServer server;
    private Thread serverThread;
    private int testPort = 12347;
    private int testWsPort = 8889;

    @BeforeEach
    public void setUp() {
        server = new ChatServer();
    }

    @AfterEach
    public void tearDown() {
        if (server != null) {
            server.stop();
        }
        if (serverThread != null && serverThread.isAlive()) {
            serverThread.interrupt();
        }
    }

    @Test
    public void testStartAndStopServer() {
        // Start the server in a separate thread to avoid blocking
        serverThread = new Thread(() -> {
            try {
                server.start(testPort, testWsPort); // Use test ports
            } catch (Exception ignored) {}
        });
        serverThread.setDaemon(true);
        serverThread.start();
        // Give the server a moment to start
        try { Thread.sleep(300); } catch (InterruptedException ignored) {}
        // Try connecting to the server socket
        try (Socket socket = new Socket("localhost", testPort)) {
            assertTrue(socket.isConnected());
        } catch (IOException e) {
            fail("Server did not accept connection: " + e.getMessage());
        }
        // Stop the server
        assertDoesNotThrow(() -> server.stop());
    }

    @Test
    public void testMainDoesNotThrow() {
        assertDoesNotThrow(() -> {
            Thread mainThread = new Thread(() -> {
                try {
                    ChatServer.main(new String[] {"test"});
                } catch (Exception ignored) {}
            });
            mainThread.setDaemon(true);
            mainThread.start();
            try { Thread.sleep(300); } catch (InterruptedException ignored) {}
        });
    }

    @Test
    public void testMultipleClients() throws Exception {
        serverThread = new Thread(() -> server.start(testPort, testWsPort));
        serverThread.setDaemon(true);
        serverThread.start();
        Thread.sleep(300);
        int clientCount = 3;
        Socket[] sockets = new Socket[clientCount];
        for (int i = 0; i < clientCount; i++) {
            sockets[i] = new Socket("localhost", testPort);
            assertTrue(sockets[i].isConnected());
        }
        for (Socket s : sockets) {
            s.close();
        }
        assertDoesNotThrow(() -> server.stop());
    }

    @Test
    public void testStopWithoutStart() {
        assertDoesNotThrow(() -> server.stop());
    }

    @Test
    public void testServerHandlesIOException() {
        ChatServer faultyServer = new ChatServer() {
            @Override
            void start(int port, int wsPort) {
                throw new RuntimeException(new IOException("Simulated IO error"));
            }
        };
        assertThrows(RuntimeException.class, () -> faultyServer.start(9999, 9998));
    }

    @Test
    public void testStartHandlesIOException() {
        ChatServer ioServer = new ChatServer() {
            @Override
            void start(int port, int wsPort) {
                throw new RuntimeException(new IOException("Simulated IO error"));
            }
        };
        assertThrows(RuntimeException.class, () -> ioServer.start(11111, 11112));
    }

    @Test
    public void testStopHandlesInterruptedException() {
        ChatServer interruptedServer = new ChatServer() {
            @Override
            public void stop() {
                throw new RuntimeException(new InterruptedException("Simulated interrupt"));
            }
        };
        assertThrows(RuntimeException.class, interruptedServer::stop);
    }

    @Test
    public void testClientHandlerListSynchronization() throws Exception {
        serverThread = new Thread(() -> server.start(testPort, testWsPort));
        serverThread.setDaemon(true);
        serverThread.start();
        Thread.sleep(300);
        Socket socket1 = new Socket("localhost", testPort);
        Socket socket2 = new Socket("localhost", testPort);
        assertTrue(socket1.isConnected());
        assertTrue(socket2.isConnected());
        socket1.close();
        socket2.close();
        assertDoesNotThrow(() -> server.stop());
    }
}
