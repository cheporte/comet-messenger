package com.comet.demo.core.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class ChatServer {
    private ServerSocket serverSocket;
    private final Map<Integer, List<ClientHandler>> chatRooms = new HashMap<>();

    public void start(int port) {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("[Server] Listening on port " + port + "...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("[Server] New client connected: " + clientSocket.getInetAddress().getHostAddress());

                // Assume the first message from the client is the chat ID they want to join
                String currentChatId = new Scanner(clientSocket.getInputStream()).nextLine();

                ClientHandler handler = new ClientHandler(clientSocket, chatRooms.computeIfAbsent(Integer.valueOf(currentChatId), k -> new ArrayList<>()), currentChatId);

                synchronized (chatRooms.get(currentChatId)) {
                    if (!chatRooms.get(currentChatId).contains(handler)) {
                        chatRooms.get(currentChatId).add(handler);
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
        } catch (IOException e) {
            System.err.println("[Server] Error while stopping: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        ChatServer server = new ChatServer();
        server.start(12345);
    }
}
