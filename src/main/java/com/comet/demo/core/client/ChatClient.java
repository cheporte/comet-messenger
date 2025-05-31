package com.comet.demo.core.client;

import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

import java.util.logging.Level;
import java.util.logging.Logger;

public class ChatClient {
    private static final Logger logger = Logger.getLogger(ChatClient.class.getName());

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Thread listenerThread;

    private final String serverAddress;
    private final int serverPort;
    private final Consumer<String> messageHandler;

    private final String username;
    private final String password;

    public ChatClient(
            String serverAddress,
            int serverPort,
            String username,
            String password,
            Consumer<String> messageHandler
    ) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.username = username;
        this.password = password;
        this.messageHandler = messageHandler;
    }

    public void start() {
        try {
            socket = new Socket(serverAddress, serverPort);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Send credentials
            out.println(username);
            out.println(password);

            // Start listener thread
            listenerThread = new Thread(this::listenForMessages);
            listenerThread.setDaemon(true);
            listenerThread.start();

            logger.info("Connected to the server as " + username);
            messageHandler.accept("Connected as " + username);

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to connect to the server.", e);
            messageHandler.accept("Failed to connect: " + e.getMessage());
        }
    }

    private void listenForMessages() {
        try {
            String msg;
            while ((msg = in.readLine()) != null) {
                logger.info("Received message: " + msg);
                messageHandler.accept(msg);
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Connection lost.", e);
            messageHandler.accept("Connection lost.");
        } finally {
            close();
        }
    }

    public void sendMessage(String msg) {
        if (out != null) {
            out.println(username + ": " + msg);
        }
    }

    public void close() {
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null) socket.close();
            if (listenerThread != null) listenerThread.interrupt();
            logger.info("Disconnected from the server.");
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error closing connection.", e);
            messageHandler.accept("Error closing connection: " + e.getMessage());
        }
    }
}
