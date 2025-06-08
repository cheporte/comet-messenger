package com.comet.demo.core.client;

import org.junit.jupiter.api.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

public class ChatClientTest {
    private ServerSocket serverSocket;
    private int port;
    private List<String> receivedMessages;
    private CountDownLatch latch;

    @BeforeEach
    public void setUp() throws IOException {
        serverSocket = new ServerSocket(0); // random free port
        port = serverSocket.getLocalPort();
        receivedMessages = new ArrayList<>();
        latch = new CountDownLatch(1);
    }

    @AfterEach
    public void tearDown() throws IOException {
        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
        }
    }

    @Test
    public void testStartAndSendMessage() throws Exception {
        // Start a thread to accept the client and check credentials
        Thread serverThread = new Thread(() -> {
            try (Socket client = serverSocket.accept();
                 BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                 PrintWriter out = new PrintWriter(client.getOutputStream(), true)) {
                String user = in.readLine();
                String pass = in.readLine();
                assertEquals("testuser", user);
                assertEquals("testpass", pass);
                // Echo a message back
                out.println("Welcome " + user);
                // Read a sent message
                String sent = in.readLine();
                assertEquals("testuser: Hello!", sent);
            } catch (IOException e) {
                fail(e);
            } finally {
                latch.countDown();
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();

        ChatClient client = new ChatClient("localhost", port, "testuser", "testpass", msg -> receivedMessages.add(msg));
        client.start();
        // Wait for connection
        Thread.sleep(200);
        client.sendMessage("Hello!");
        // Wait for server to echo
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        // Wait for client to process
        Thread.sleep(100);
        assertTrue(receivedMessages.stream().anyMatch(m -> m.contains("Connected as testuser")));
        assertTrue(receivedMessages.stream().anyMatch(m -> m.contains("Welcome testuser")));
        client.close();
    }

    @Test
    public void testConnectionFailure() {
        // Use a port that is not open
        int unusedPort = 6553;
        ChatClient client = new ChatClient("localhost", unusedPort, "u", "p", msg -> receivedMessages.add(msg));
        client.start();
        assertTrue(receivedMessages.stream().anyMatch(m -> m.contains("Failed to connect")));
    }

    @Test
    public void testCloseIsIdempotent() throws Exception {
        ChatClient client = new ChatClient("localhost", port, "u", "p", msg -> {});
        client.close();
        client.close();
    }
}
