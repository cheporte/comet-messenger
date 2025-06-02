package com.comet.demo.core.server;

import com.comet.db.schema.SchemaInitializer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ChatServer {
    private ServerSocket serverSocket;
    private ChatWebSocketServer webSocketServer;
    private static final List<ClientHandler> clientHandlers = new ArrayList<>();

    /**
     * Starts the chat server and WebSocket server on the specified ports.
     * Accepts incoming client connections and starts a handler thread for each client.
     *
     * @param port the port for the main chat server
     * @param webSocketPort the port for the WebSocket server
     */
    private void start(int port, int webSocketPort) {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("[Server] Listening on port " + port + "...");

            webSocketServer = new ChatWebSocketServer(webSocketPort);
            webSocketServer.start();
            System.out.println("[WebSocket Server] Listening on port " + webSocketPort + "...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("[Server] New client connected: " + clientSocket.getInetAddress().getHostAddress());

                ClientHandler handler = new ClientHandler(clientSocket, clientHandlers);

                synchronized (clientHandlers) {
                    if (!clientHandlers.contains(handler)) {
                        clientHandlers.add(handler);
                    }
                }

                new Thread(handler).start();
            }

        } catch (IOException e) {
            System.err.println("[Server] Error: " + e.getMessage());
        }
    }

    /**
     * Stops the chat server and WebSocket server, closing all resources.
     */
    public void stop() {
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
            if (webSocketServer != null) {
                webSocketServer.stop();
            }
        } catch (IOException e) {
            System.err.println("[Server] Error while stopping: " + e.getMessage());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Main entry point for the chat server application.
     *
     * @param args command-line arguments (not used)
     */
    public static void main(String[] args) throws SQLException {
        ChatServer server = new ChatServer();
        SchemaInitializer.init();
        server.start(12345, 8887);
    }
}
