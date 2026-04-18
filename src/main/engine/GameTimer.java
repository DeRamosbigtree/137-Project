package main.engine;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;

public class GameTimer {
    private int matchLengthSeconds;
    private long matchStartTime;
    private int timeLeft;
    private boolean isGameOver;

    public GameTimer(int seconds) {
        this.matchLengthSeconds = seconds;
        this.matchStartTime = System.currentTimeMillis();
        this.isGameOver = false;
    }

    public void update() {
        if (isGameOver) return;

        long currentTime = System.currentTimeMillis();
        int elapsedSeconds = (int) ((currentTime - matchStartTime) / 1000);
        timeLeft = matchLengthSeconds - elapsedSeconds;

        if (timeLeft <= 0) {
            timeLeft = 0;
            isGameOver = true;
        }
    }

    public void draw(Graphics g) {
        Font font = new Font("Arial", Font.BOLD, 16);
        g.setFont(font);
        int margin = 10;
        int panelWidth = g.getClipBounds().width;
        int timerY = 20;

        String timerText;
        if (timeLeft >= 60) {
            int minutes = timeLeft / 60;
            int seconds = timeLeft % 60;
            timerText = String.format("Time Left: %d:%02d", minutes, seconds);
        } else {
            timerText = "Time Left: " + timeLeft + "s";
        }

        int timerTextWidth = g.getFontMetrics(font).stringWidth(timerText);
        int timerX = panelWidth - timerTextWidth - margin;

        g.setColor(Color.white);
        g.drawString(timerText, timerX, timerY);
    }

    public boolean isGameOver() {
        return isGameOver;
    }
}