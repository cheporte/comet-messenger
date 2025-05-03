package com.comet.demo.core.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class ChatClient {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Scanner scanner;

    public void start(String serverAddress, int serverPort) {
        try{
            socket = new Socket(serverAddress, serverPort);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            scanner = new Scanner(System.in);

            System.out.println("✨ Connected to the chat server! Type your name:");
            Scanner console = new Scanner(System.in);
            String username = console.nextLine();
            out.println(username);

            // Reader thread
            new Thread(() -> {
                String response;
                try {
                    while ((response = in.readLine()) != null) {
                        System.out.println("[🛸 Server]: " + response);
                    }
                } catch (IOException e) {
                    System.out.println("💔 Server connection closed.");
                }
            }).start();

            // Writer thread
            String message;
            while (scanner.hasNextLine()) {
                message = scanner.nextLine();
                if (message.equalsIgnoreCase("/quit")) {
                    closeEverything();
                    break;
                }
                out.println(message);
            }

        } catch (IOException e) {
            System.err.println("[Server] Error connecting to the server: " + e.getMessage());
        }
    }

    private void closeEverything() {
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null) socket.close();
            if (scanner != null) scanner.close();
            System.out.println("[<UNK> Server] closed the chat server.");
        } catch (IOException e) {
            System.err.println("[Server] Error closing connection: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        ChatClient chatClient = new ChatClient();
        chatClient.start("localhost", 12345);
    }
}
