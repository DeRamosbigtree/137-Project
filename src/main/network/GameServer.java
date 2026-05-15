package main.network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import main.model.PowerUp;
import main.model.Trap; 
import java.awt.Rectangle;
import java.util.Iterator;
import java.util.Random;

public class GameServer {

    private static final int PORT = 5000;
    private static final int MAX_PLAYERS = 4;
    //static final int SPEED = 5;
    static final int PLAYER_SIZE = 30;
    static final int ARENA_W = 900;
    static final int ARENA_H = 600;
    private static final int MATCH_SECONDS = 70;

    private ServerSocket serverSocket;
    private final ArrayList<ClientHandler> clients = new ArrayList<>();
    // for the game phases
    private enum GamePhase { LOBBY, COUNTDOWN, PLAYING, GAME_OVER }
    private volatile GamePhase phase = GamePhase.LOBBY;

    private long timeLeftMs = MATCH_SECONDS * 1000L;
    private int timeLeftSeconds = MATCH_SECONDS;
    private final long[] scores = new long[MAX_PLAYERS];
    private long lastTickTime;
    private long lastScoreBroadcast;
    
    private final ArrayList<PowerUp> powerUps = new ArrayList<>();
    private final ArrayList<Trap> traps = new ArrayList<>();
    private long lastPowerUpSpawn = 0;
    private final Random rand = new Random();

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
                System.out.println("Player " + playerId + " joined. (" + clients.size() + "/" + MAX_PLAYERS + ")");
                broadcast("SERVER Player " + playerId + " joined the game.");
            }

            System.out.println("Lobby full. Starting countdown.");
            phase = GamePhase.COUNTDOWN;
            runCountdown();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // countdown after players connect
    private void runCountdown() {
        new Thread(() -> {
            try {
                for (int i = 5; i >= 1; i--) {
                    broadcast("COUNTDOWN " + i);
                    Thread.sleep(1000);
                }
                broadcast("COUNTDOWN 0");

                synchronized (GameServer.this) {
                    clients.get(0).isIt = true;
                    phase = GamePhase.PLAYING;
                    lastTickTime = System.currentTimeMillis();
                    lastScoreBroadcast = lastTickTime;
                    broadcastAllPlayers();
                }

                startGameLoop();

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    // tick-based loop. update players, updateInvulnerability, handleTagging, updateTimerAndScores
    // every 16ms
    private void startGameLoop() {
        new Thread(() -> {
            while (phase != GamePhase.LOBBY) {
                try {
                    synchronized (GameServer.this) {
                        if (phase == GamePhase.PLAYING) {
                            long now = System.currentTimeMillis();
                            long delta = now - lastTickTime;
                            lastTickTime = now;

                            updatePlayers();
                            updateInvulnerability();
                            updateEffects();
                            handleTagging();
                            spawnAndCheckPowerUps();
                            checkTraps();
                            updateTimerAndScores(delta);
                        }
                        broadcastAllPlayers();
                    }
                    Thread.sleep(16);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }).start();
    }
    // update the players 
    private void updatePlayers() {
        for (ClientHandler c : clients) {
            if (c.isFrozen) continue;
            c.x += c.dx * c.speed;
            c.y += c.dy * c.speed;
            if (c.x < 0) c.x = 0;
            if (c.y < 0) c.y = 0;
            if (c.x > ARENA_W - PLAYER_SIZE) c.x = ARENA_W - PLAYER_SIZE;
            if (c.y > ARENA_H - PLAYER_SIZE) c.y = ARENA_H - PLAYER_SIZE;
            
            // to determine direction
            if (c.dy < 0) c.direction = 0; // up
            else if (c.dy > 0) c.direction = 1; // down
            else if (c.dx < 0) c.direction = 2; // left
            else if (c.dx > 0) c.direction = 3; // right
            
            // animate sprite if player is moving
            if (c.dx != 0 || c.dy != 0) {
                c.spriteCounter++;
                if (c.spriteCounter > 10) {
                    c.spriteNum = (c.spriteNum == 1) ? 2 : 1;
                    c.spriteCounter = 0;
                }
            } else {
                c.spriteNum = 1; // Resets character to a standing frame when stopped
            }
        }
    }

    // update invulnerability of players
    private void updateInvulnerability() {
        long now = System.currentTimeMillis();
        for (ClientHandler c : clients) {
            if (c.isInvulnerable && now - c.lastTaggedTime >= 2000) {
                c.isInvulnerable = false;
            }
        }
    }

    // handles the tagging of players
    private void handleTagging() {
        ClientHandler itPlayer = getItPlayer();
        if (itPlayer == null) return;

        for (ClientHandler target : clients) {
            if (target == itPlayer) continue;
            if (!intersects(itPlayer, target)) continue;
            if (target.isInvulnerable) continue;

            if (target.isImmune) {
                target.isImmune = false;
                return;
            }

            itPlayer.isIt = false;
            itPlayer.isInvulnerable = true;
            itPlayer.lastTaggedTime = System.currentTimeMillis();
            target.isIt = true;
            return;
        }
    }

    // update the timer and scores of each player
    private void updateTimerAndScores(long deltaMs) {
        ClientHandler itPlayer = getItPlayer();
        if (itPlayer != null) {
            scores[itPlayer.playerId] += deltaMs;
        }

        timeLeftMs -= deltaMs;
        int newSeconds = (int) (timeLeftMs / 1000);

        if (newSeconds != timeLeftSeconds) {
            timeLeftSeconds = Math.max(newSeconds, 0);
            broadcast("TIMER " + timeLeftSeconds);
        }

        long now = System.currentTimeMillis();
        if (now - lastScoreBroadcast >= 500) {
            broadcastScores();
            lastScoreBroadcast = now;
        }

        if (timeLeftMs <= 0) {
            timeLeftMs = 0;
            phase = GamePhase.GAME_OVER;

            int winnerId = 0;
            for (int i = 1; i < MAX_PLAYERS; i++) {
                if (scores[i] < scores[winnerId]) winnerId = i;
            }
            broadcast("WINNER " + winnerId);
            broadcastScores();
            System.out.println("Game over. Winner: Player " + winnerId);
        }
    }

    // print the score
    private void broadcastScores() {
        StringBuilder sb = new StringBuilder("SCORE");
        for (int i = 0; i < MAX_PLAYERS; i++) {
            sb.append(" ").append(i).append(",").append(scores[i]);
        }
        broadcast(sb.toString());
    }

    // get the current it player
    private ClientHandler getItPlayer() {
        for (ClientHandler c : clients) {
            if (c.isIt) return c;
        }
        return null;
    }

    // check if the players interesects
    private boolean intersects(ClientHandler a, ClientHandler b) {
        return a.x < b.x + PLAYER_SIZE &&
            a.x + PLAYER_SIZE > b.x &&
            a.y < b.y + PLAYER_SIZE &&
            a.y + PLAYER_SIZE > b.y;
    }

    public synchronized void removeClient(ClientHandler client) {
        clients.remove(client);
        System.out.println("Player " + client.playerId + " removed.");
        broadcast("DISCONNECT " + client.playerId);
        if (client.isIt && !clients.isEmpty()) {
            clients.get(0).isIt = true;
        }
        broadcastAllPlayers();
    }

    public synchronized void broadcast(String message) {
        for (ClientHandler client : clients) {
            client.send(message);
        }
    }

    // state per player: id,x,y,isIt,isInvulnerable,isFrozen,isImmune,isInvisible
    public synchronized void broadcastAllPlayers() {
        StringBuilder sb = new StringBuilder("STATE");
        for (ClientHandler c : clients) {
            sb.append(" ")
              .append(c.playerId).append(",")
              .append(c.x).append(",")
              .append(c.y).append(",")
              .append(c.isIt ? 1 : 0).append(",")
              .append(c.isInvulnerable ? 1 : 0).append(",")
              .append(c.isFrozen ? 1 : 0).append(",")
              .append(c.isImmune ? 1 : 0).append(",")
              .append(c.isInvisible ? 1 : 0).append(",")
              .append(c.direction).append(",")
              .append(c.spriteNum);
        }
        broadcast(sb.toString());
    }

    public static void main(String[] args) {
        new GameServer();
    }
    
    private void updateEffects() {
        long now = System.currentTimeMillis();
        for (ClientHandler c : clients) {
            c.speed = (now < c.speedEndTime) ? 8 : 5;
            c.isFrozen = (now < c.freezeEndTime);
            c.isImmune = (now < c.immuneEndTime);
            c.isInvisible = (now < c.invisibleEndTime);
        }
    }
    
    private void spawnAndCheckPowerUps() {
        long now = System.currentTimeMillis();
        boolean changed = false;

        // spawn logic (every 3 seconds)
        if (now - lastPowerUpSpawn > 3000) {
            int x = rand.nextInt(ARENA_W - 20);
            int y = rand.nextInt(ARENA_H - 20);
            powerUps.add(new PowerUp(x, y, PowerUp.getRandomType()));
            lastPowerUpSpawn = now;
            changed = true;
        }

        // collision logic
        Iterator<PowerUp> it = powerUps.iterator();
        while (it.hasNext()) {
            PowerUp p = it.next();
            Rectangle pBounds = new Rectangle(p.x, p.y, p.size, p.size);

            for (ClientHandler c : clients) {
                Rectangle cBounds = new Rectangle(c.x, c.y, PLAYER_SIZE, PLAYER_SIZE);
                if (pBounds.intersects(cBounds)) {
                    applyEffect(c, p.type);
                    it.remove();
                    changed = true;
                    break;
                }
            }
        }

        if (changed) {
            broadcastPowerUps();
        }
    }
    
    private void applyEffect(ClientHandler c, PowerUp.Type type) {
        long now = System.currentTimeMillis();
        switch (type) {
            case SPEED: c.speedEndTime = now + 3000; break;
            case FREEZE: c.trapCharges += 1; break;
            case SHIELD: c.immuneEndTime = now + 10000; c.isImmune = true; break;
            case GHOST: c.invisibleEndTime = now + 4000; c.isInvisible = true; break;
        }
    }

    private void broadcastPowerUps() {
        StringBuilder sb = new StringBuilder("POWERUPS");
        for (PowerUp p : powerUps) {
            sb.append(" ").append(p.x).append(",").append(p.y).append(",").append(p.type.name());
        }
        broadcast(sb.toString());
    }
    
    public synchronized void placeTrap(ClientHandler client) {
        // allow trap placement if they have charges and aren't currently frozen
        if (client.trapCharges > 0 && !client.isFrozen) {
            traps.add(new Trap(client.x, client.y, client.playerId));
            client.trapCharges--;
            broadcastTraps();
        }
    }
    
    private void checkTraps() {
        long now = System.currentTimeMillis();
        boolean changed = false;
        Iterator<Trap> it = traps.iterator();
        
        while (it.hasNext()) {
            Trap t = it.next();
            Rectangle tBounds = new Rectangle(t.x, t.y, t.size, t.size);
            
            for (ClientHandler c : clients) {
                if (c.playerId == t.ownerId) continue; // don't trigger your own trap
                if (c.isFrozen || c.isInvulnerable) continue; 
                
                Rectangle cBounds = new Rectangle(c.x, c.y, PLAYER_SIZE, PLAYER_SIZE);
                if (tBounds.intersects(cBounds)) {
                    // apply freeze or consume shield
                    if (c.isImmune) {
                        c.isImmune = false;
                        c.immuneEndTime = 0; // consume the shield
                    } else {
                        c.isFrozen = true;
                        c.freezeEndTime = now + 2000; // Freeze for 2 seconds
                    }
                    it.remove();
                    changed = true;
                    break; // trap is destroyed, move to next trap
                }
            }
        }
        
        if (changed) {
            broadcastTraps();
        }
    }

    private void broadcastTraps() { 
        StringBuilder sb = new StringBuilder("TRAPS");
        for (Trap t : traps) {
            sb.append(" ").append(t.x).append(",").append(t.y).append(",").append(t.ownerId);
        }
        broadcast(sb.toString());
    }
}
