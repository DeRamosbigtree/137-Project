package main.network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class GameServer {

    private static final int PORT = 5000;
    private static final int MAX_PLAYERS = 4;
    static final int SPEED = 5;
    static final int PLAYER_SIZE = 30;
    static final int ARENA_W = 800;
    static final int ARENA_H = 600;
    private static final int MATCH_SECONDS = 70;

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

                broadcastAllPlayers();

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

    public synchronized void broadcastAllPlayers() {
        StringBuilder sb = new StringBuilder("STATE");

        for (ClientHandler c : clients) {
            sb.append(" ")
            .append(c.playerId).append(",")
            .append(c.x).append(",")
            .append(c.y);
        }

        broadcast(sb.toString());
    }

    public static void main(String[] args) {
        new GameServer();
    }
}