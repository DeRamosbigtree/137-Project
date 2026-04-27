package main.network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class GameServer {

    private static final int PORT = 5000;
    private static final int MAX_PLAYERS = 4;

    private ServerSocket serverSocket;
    private ArrayList<ClientHandler> clients = new ArrayList<>();

    public GameServer() {
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("Server started on port " + PORT);
            System.out.println("Waiting for players...");

            while (clients.size() < MAX_PLAYERS) {
                Socket socket = serverSocket.accept();

                int playerId = clients.size();
                ClientHandler client = new ClientHandler(socket, playerId, this);

                clients.add(client);
                new Thread(client).start();

                System.out.println("Player " + playerId + " joined.");
                System.out.println("Players connected: " + clients.size() + "/" + MAX_PLAYERS);

                broadcast("SERVER Player " + playerId + " joined the game.");
            }

            System.out.println("Game lobby full. All 4 players connected.");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void broadcast(String message) {
        for (ClientHandler client : clients) {
            client.send(message);
        }
    }

    public static void main(String[] args) {
        new GameServer();
    }
}