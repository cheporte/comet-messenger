package com.comet.demo.core.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ChatServer {
    private ServerSocket serverSocket;
    private static final List<PrintWriter> clientWriters = new ArrayList<>();

    private void start(int port) {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("[Server] Listening on port " + port + "...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("[Server] New client connected: " + clientSocket.getInetAddress().getHostAddress());

                PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);

                synchronized (clientWriters) {
                    clientWriters.add(writer);
                }

                new Thread(new ClientHandler(clientSocket, clientWriters)).start();
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
