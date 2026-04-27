package main.engine;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import javax.swing.JPanel;
import main.input.KeyHandler;
import main.model.Player;
import main.model.PowerUp;
import main.model.Trap;
import main.network.GameClient;

public class GamePanel extends JPanel {

    private final ArrayList<Player> players = new ArrayList<>();
    private final KeyHandler keyH = new KeyHandler();

    private Player mainPlayer;
    private GameClient client;

    private final GameTimer timer = new GameTimer(70);

    private ScoreManager scoreManager;
    
    private final ArrayList<PowerUp> powerUps = new ArrayList<>();
    private final ArrayList<Trap> traps = new ArrayList<>();
    private final Random rand = new Random();

    private long lastSpawn = 0;
    private final long spawnDelay = 3000; // 7 seconds
    
    

    public GamePanel() {
        this.setPreferredSize(new Dimension(800, 600));
        this.setBackground(Color.BLACK);
        this.setFocusable(true);
        this.addKeyListener(keyH);

        try {
            client = new GameClient();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // initializePlayers();
        for (int i = 0; i < 4; i++) {
            players.add(new Player(i, 100, 100, false));
        }

        if (mainPlayer == null && client.playerId >= 0 && client.playerId < players.size()) {
            mainPlayer = players.get(client.playerId);
        }

        this.scoreManager = new ScoreManager(players);
    }

    private void initializePlayers() {
        mainPlayer = new Player(0, 100, 100, false);
        players.add(mainPlayer);

        players.add(new Player(1, 500, 100, true));
        players.add(new Player(2, 100, 400, true));
        players.add(new Player(3, 500, 400, true));

        // Start with player 0 as It
        players.get(0).isIt = true;
    }

    public void updateGame() {

        // timer.update();

        // if (timer.isGameOver()) {
        //     return;
        // }

        if (mainPlayer == null && client.playerId >= 0 && client.playerId < players.size()) {
            mainPlayer = players.get(client.playerId);
        }

        updateMainPlayer();
        // updateBots();
        
        // updatePowerUps();
        // updateEffects();
        // updateTraps(players);
        
        for (Player p : players) {
            p.updateInvulnerability();
        }

        if (client != null) {
            for (Integer id : client.playerStates.keySet()) {
                if (id < players.size()) {
                    int[] pos = client.playerStates.get(id);
                    players.get(id).x = pos[0];
                    players.get(id).y = pos[1];
                }
            }
        }

        handleTagging();
        scoreManager.updateScore(getCurrentItPlayer());
    }

    private void updateMainPlayer() {
        if (mainPlayer == null) return;
        if (mainPlayer.isFrozen) return;

        int dx = 0;
        int dy = 0;

        if (keyH.up) dy--;
        if (keyH.down) dy++;
        if (keyH.left) dx--;
        if (keyH.right) dx++;

        if (client != null) {
            client.sendMove(dx, dy);
        }

        if (keyH.space) placeTrap(mainPlayer);
    }

    private void updateBots() {
        for (Player p : players) {
            if (p.bot && !p.isFrozen) {
                p.updateBotMovement(getWidth(), getHeight());
            }
        }
    }

    
    private void handleTagging() {
        Player currentIt = getCurrentItPlayer();
        if (currentIt == null) return;

        for (Player p : players) {
            if (p == currentIt) continue;

            // ONLY handle logic if they collide
            if (!currentIt.getBounds().intersects(p.getBounds())) continue;

            // If target is in cooldown
            if (p.isInvulnerable) continue;

            // If target has shield (immune)
            if (p.isImmune) {
                p.isImmune = false; // consume shield
                return; // no tag happens
            }

            // Normal tag
            currentIt.isIt = false;

            // former It gets cooldown
            currentIt.isInvulnerable = true;
            currentIt.lastTaggedTime = System.currentTimeMillis();

            // new It
            p.isIt = true;

            return; // ensure ONLY ONE tag happens
        }
    }

    private Player getCurrentItPlayer() {
        for (Player p : players) {
            if (p.isIt) {
                return p;
            }
        }
        return null;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        drawPlayers(g);
        drawUI(g);
        drawPowerUps(g);

    }

    private void drawPlayers(Graphics g) {
        for (Player p : players) {
            if (p.isIt) {
                g.setColor(Color.RED);
            } else {
            	if(p.isImmune) {
            		g.setColor(Color.GREEN);
            	}else {
            		g.setColor(Color.BLUE);
            	}
                
            }

            g.fillRect(p.x, p.y, p.size, p.size);

            g.setColor(Color.WHITE);
            g.drawString("P" + p.id, p.x + 8, p.y + 18);
        }
    }

    private void drawUI(Graphics g) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString("WASD to move", 10, 20);

        Player itPlayer = getCurrentItPlayer();
        if (itPlayer != null) {
            g.drawString("Current It: Player " + itPlayer.id, 10, 45);
        }

        g.drawString("Week 2: Bots + Collision + Tagging", 10, 70);

        timer.draw(g);

        scoreManager.drawScores(g, getWidth(), getHeight());

        if (timer.isGameOver()) { 
        scoreManager.drawWinner(g, getWidth(), getHeight());
        }
    }
    
    private void drawPowerUps(Graphics g) {
        for (PowerUp p : powerUps) {
            switch (p.type) {
                case SPEED: g.setColor(Color.YELLOW); break;
                case FREEZE: g.setColor(Color.CYAN); break;
                case SHIELD: g.setColor(Color.GREEN); break;
                case GHOST: g.setColor(Color.WHITE); break;
            }
            g.fillOval(p.x, p.y, p.size, p.size);
        }
        
        // draw traps
        g.setColor(Color.MAGENTA);
        for (Trap t : traps) {
            g.fillRect(t.x, t.y, t.size, t.size);
        }
    }
    
    // Power-up
    // Track active effects per player
    public static class Effect {
        PowerUp.Type type;
        long endTime;

        Effect(PowerUp.Type type, long duration) {
            this.type = type;
            this.endTime = System.currentTimeMillis() + duration;
        }
    }

    private final java.util.Map<Player, java.util.List<Effect>> activeEffects = new java.util.HashMap<>();
    
    private void applyEffect(Player player, PowerUp.Type type) {

        activeEffects.putIfAbsent(player, new ArrayList<>());

        switch (type) {
            case SPEED:
                player.speed = 6 ; // boosted speed by 50%
                activeEffects.get(player).add(new Effect(type, 3000));
                break;

            case FREEZE:
                player.trapCharges += 1;
                break;
            case SHIELD:
            	player.isImmune = true;
            	activeEffects.get(player).add(new Effect(type, 10000));
            	break;
            case GHOST:
            	player.isInvisible = true;
            	activeEffects.get(player).add(new Effect(type, 4000));
            	break;
        }
    }
    
    private void spawnPowerUp() {
        int width = Math.max(getWidth(), 800);
        int height = Math.max(getHeight(), 600);

        int x = rand.nextInt(width - 20);
        int y = rand.nextInt(height - 20);
        powerUps.add(new PowerUp(x, y, PowerUp.getRandomType()));
    }
    
    private void updatePowerUps() {
        long now = System.currentTimeMillis();

        // allow spawning of power up every 7 seconds 
        if (now - lastSpawn > spawnDelay) {
            spawnPowerUp();
            lastSpawn = now;
        }

        // collision
        Iterator<PowerUp> it = powerUps.iterator();

        while (it.hasNext()) {
            PowerUp p = it.next();

            for (Player player : players) {
                if (player.getBounds().intersects(p.getBounds())) {

                    applyEffect(player, p.type);
                    it.remove();
                    break;
                }
            }
        }
    }
    
    public void placeTrap(Player player) {
        if (player.trapCharges <= 0) return;

        traps.add(new Trap(player.x, player.y, player));
        player.trapCharges--;
    }
    
    private void updateTraps(ArrayList<Player> players) {
        Iterator<Trap> it = traps.iterator();

        while (it.hasNext()) {
            Trap t = it.next();

            for (Player p : players) {
                if (p == t.owner) continue;

                if (p.getBounds().intersects(t.getBounds())) {

                    // apply freeze effect to player (2 seconds)
                    activeEffects.putIfAbsent(p, new ArrayList<>());
                    activeEffects.get(p).add(new Effect(PowerUp.Type.FREEZE, 2000));

                    it.remove();
                    break;
                }
            }
        }
    }
    
    private void updateEffects() {
        long now = System.currentTimeMillis();

        for (Player player : players) {

            // skip if no effects
            if (!activeEffects.containsKey(player)) continue;

            java.util.List<Effect> effects = activeEffects.get(player);

            Iterator<Effect> it = effects.iterator();

            boolean hasSpeed = false;
            boolean isFrozen = false;
            boolean isImmune = false;
            boolean isInvisible = false;

            while (it.hasNext()) {
                Effect e = it.next();

                if (now > e.endTime) {
                    it.remove();
                } else {
                    if (e.type == PowerUp.Type.SPEED) hasSpeed = true;
                    if (e.type == PowerUp.Type.FREEZE) isFrozen = true;
                    if( e.type == PowerUp.Type.SHIELD) isImmune = true;
                    if( e.type == PowerUp.Type.GHOST) isInvisible = true;
                }
            }

            player.speed = hasSpeed ? 6 : 4;
            player.isFrozen = isFrozen;
            //player.isImmune = isImmune;
            player.isInvisible = isInvisible;
        }
    }
}