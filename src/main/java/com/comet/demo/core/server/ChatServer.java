package com.comet.demo.core.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ChatServer {
    private ServerSocket serverSocket;
    private ChatWebSocketServer webSocketServer;
    private static final List<ClientHandler> clientHandlers = new ArrayList<>();

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

    public static void main(String[] args) {
        ChatServer server = new ChatServer();
        server.start(12345, 8887);
    }
}
