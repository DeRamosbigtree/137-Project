package main.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class GameClient {

    // int[] = {x, y, isIt, isInvulnerable, isFrozen, isImmune, isInvisible}
    public final Map<Integer, int[]> playerStates = new ConcurrentHashMap<>();

    // Game phase: LOBBY, COUNTDOWN, PLAYING, GAME_OVER
    public volatile String phase = "LOBBY";
    public volatile int timeLeft = 70;
    public volatile int countdownValue = 0;
    public volatile int winnerId = -1;
    public final long[] scores = new long[4];

    private static final String HOST = "localhost";
    private static final int PORT = 5000;
    public static GameClient client;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public int playerId = -1;

    public GameClient() {
        try {
            socket = new Socket(HOST, PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            System.out.println("Connected to server.");
            new Thread(this::listenToServer).start();
        } catch (IOException e) {
            System.out.println("Could not connect to server.");
            e.printStackTrace();
        }
    }

    private void listenToServer() {
        try {
            String msg;
            while ((msg = in.readLine()) != null) {
                String[] parts = msg.split(" ");

                switch (parts[0]) {
                    case "ID":
                        playerId = Integer.parseInt(parts[1]);
                        System.out.println("My ID: " + playerId);
                        break;

                    case "STATE":
                        for (int i = 1; i < parts.length; i++) {
                            String[] data = parts[i].split(",");
                            if (data.length < 8) continue;
                            int id             = Integer.parseInt(data[0]);
                            int x              = Integer.parseInt(data[1]);
                            int y              = Integer.parseInt(data[2]);
                            int isIt           = Integer.parseInt(data[3]);
                            int isInvulnerable = Integer.parseInt(data[4]);
                            int isFrozen       = Integer.parseInt(data[5]);
                            int isImmune       = Integer.parseInt(data[6]);
                            int isInvisible    = Integer.parseInt(data[7]);
                            playerStates.put(id, new int[]{x, y, isIt, isInvulnerable, isFrozen, isImmune, isInvisible});
                        }
                        break;

                    case "COUNTDOWN":
                        countdownValue = Integer.parseInt(parts[1]);
                        phase = countdownValue > 0 ? "COUNTDOWN" : "PLAYING";
                        break;

                    case "TIMER":
                        timeLeft = Integer.parseInt(parts[1]);
                        if ("LOBBY".equals(phase) || "COUNTDOWN".equals(phase)) phase = "PLAYING";
                        break;

                    case "SCORE":
                        for (int i = 1; i < parts.length; i++) {
                            String[] data = parts[i].split(",");
                            if (data.length < 2) continue;
                            int id = Integer.parseInt(data[0]);
                            if (id >= 0 && id < scores.length) {
                                scores[id] = Long.parseLong(data[1]);
                            }
                        }
                        break;

                    case "WINNER":
                        winnerId = Integer.parseInt(parts[1]);
                        phase = "GAME_OVER";
                        break;

                    case "DISCONNECT":
                        int disconnectedId = Integer.parseInt(parts[1]);
                        playerStates.remove(disconnectedId);
                        System.out.println("Player " + disconnectedId + " disconnected.");
                        break;
                }
            }
        } catch (IOException e) {
            System.out.println("Disconnected from server.");
        }
    }

    public void send(String message) {
        if (out != null) out.println(message);
    }

    public void sendInput(int dx, int dy) {
        if (out != null) out.println("INPUT " + dx + " " + dy);
    }

    public static void main(String[] args) {
        new GameClient();
    }
}
