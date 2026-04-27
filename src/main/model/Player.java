package main.model;

import java.awt.Rectangle;
import java.util.Random;

public class Player {
    public int id;
    public int x, y;
    public int size = 30;
    public double speed = 4;
    public boolean isIt = false;
    public long timeAsIt = 0;
    public boolean isFrozen = false;
    public boolean isImmune = false;
    public boolean isInvisible = false;
    public int trapCharges;

    // Tag cooldown
    public boolean isInvulnerable = false;
    public long lastTaggedTime = 0;
    

    // For simple bot movement
    public boolean bot;
    // public boolean isStatic = false;
    private int botDx = 0;
    private int botDy = 0;
    private int directionTimer = 0;

    private final Random random = new Random();

    public Player(int id, int x, int y, boolean bot) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.bot = bot;
    }

    public void move(int dx, int dy) {
    	if (isFrozen) return;
    	
        x += dx * speed;
        y += dy * speed;
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, size, size);
    }

    public void updateBotMovement(int panelWidth, int panelHeight) {
        if (!bot) return;

        directionTimer--;

        if (directionTimer <= 0) {
            botDx = random.nextInt(3) - 1; // -1, 0, 1
            botDy = random.nextInt(3) - 1; // -1, 0, 1
            directionTimer = 20 + random.nextInt(40);
        }

        move(botDx, botDy);
        clampToBounds(panelWidth, panelHeight);
    }

    public void clampToBounds(int panelWidth, int panelHeight) {
        if (x < 0) x = 0;
        if (y < 0) y = 0;
        if (x > panelWidth - size) x = panelWidth - size;
        if (y > panelHeight - size) y = panelHeight - size;
    }

    public void updateInvulnerability() {
        if (isInvulnerable) {
            long currentTime = System.currentTimeMillis();

            if (currentTime - lastTaggedTime >= 2000) {
                isInvulnerable = false;
            }
        }
    }

}