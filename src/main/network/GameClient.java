package main.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;
import main.model.PowerUp;
import main.model.Trap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class GameClient {

    // int[] = {x, y, isIt, isInvulnerable, isFrozen, isImmune, isInvisible}
    public final Map<Integer, int[]> playerStates = new ConcurrentHashMap<>();

    // Game phase: LOBBY, COUNTDOWN, PLAYING, GAME_OVER
    public volatile String phase = "LOBBY";
    public volatile int timeLeft = 70;
    public volatile int countdownValue = 0;
    public volatile int winnerId = -1;
    public final long[] scores = new long[4];
    public final List<PowerUp> powerUps = new CopyOnWriteArrayList<>(); // to prevent errors if thread updates while drawing thread is reading
    public final List<Trap> traps = new CopyOnWriteArrayList<>();

    // private static final String HOST = "localhost";
    private static final int PORT = 5000;
    public static GameClient client;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public int playerId = -1;

    // public GameClient() {
    //     try {
    //         socket = new Socket(HOST, PORT);
    //         in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    //         out = new PrintWriter(socket.getOutputStream(), true);
    //         System.out.println("Connected to server.");
    //         new Thread(this::listenToServer).start();
    //     } catch (IOException e) {
    //         System.out.println("Could not connect to server.");
    //         e.printStackTrace();
    //     }
    // }

    public GameClient(String host) {
        try {
            socket = new Socket(host, PORT);

            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            System.out.println("Connected to server: " + host);

            new Thread(this::listenToServer).start();

        } catch (IOException e) {
            System.out.println("Could not connect to server.");
            e.printStackTrace();
        }
    }

    public GameClient() {
        this("localhost");
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
                            if (data.length < 10) continue;
                            int id             = Integer.parseInt(data[0]);
                            int x              = Integer.parseInt(data[1]);
                            int y              = Integer.parseInt(data[2]);
                            int isIt           = Integer.parseInt(data[3]);
                            int isInvulnerable = Integer.parseInt(data[4]);
                            int isFrozen       = Integer.parseInt(data[5]);
                            int isImmune       = Integer.parseInt(data[6]);
                            int isInvisible    = Integer.parseInt(data[7]);
                            int direction      = Integer.parseInt(data[8]);
                            int spriteNum      = Integer.parseInt(data[9]);
                            playerStates.put(id, new int[]{x, y, isIt, isInvulnerable, isFrozen, isImmune, isInvisible, direction, spriteNum});
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
                        
                    case "POWERUPS":
                        powerUps.clear();
                        for (int i = 1; i < parts.length; i++) {
                            String[] pData = parts[i].split(",");
                            if (pData.length == 3) {
                                int px = Integer.parseInt(pData[0]);
                                int py = Integer.parseInt(pData[1]);
                                PowerUp.Type pType = PowerUp.Type.valueOf(pData[2]);
                                powerUps.add(new PowerUp(px, py, pType));
                            }
                        }
                        break;
                    
                    case "TRAPS":
                        traps.clear();
                        for (int i = 1; i < parts.length; i++) {
                            String[] tData = parts[i].split(",");
                            if (tData.length == 3) {
                                int tx = Integer.parseInt(tData[0]);
                                int ty = Integer.parseInt(tData[1]);
                                int ownerId = Integer.parseInt(tData[2]);
                                traps.add(new Trap(tx, ty, ownerId));
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

    // public static void main(String[] args) {
    //     new GameClient();
    // }

    public static void main(String[] args) {

        String host = "localhost";

        if (args.length > 0) {
            host = args[0];
        }

        new GameClient(host);
    }
}
