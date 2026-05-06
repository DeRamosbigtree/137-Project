package main.engine;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;

/**
 * GameTimer manages the countdown timer for the match, tracks remaining time, and displays it on screen.
 * The game ends when time runs out.
 */
public class GameTimer {
    // for computing the logic of time in GameTimer
    private int matchLengthSeconds;
    private long matchStartTime;
    private int timeLeft;
    private boolean isGameOver;

    // constructor 
    public GameTimer(int seconds) {
        this.matchLengthSeconds = seconds;
        this.matchStartTime = System.currentTimeMillis();
        this.isGameOver = false;
    }

    // check if the game is done
    public void update() {
        // return of the game is done
        if (isGameOver) return;
        
        // else compute the time lft
        long currentTime = System.currentTimeMillis();
        int elapsedSeconds = (int) ((currentTime - matchStartTime) / 1000);
        timeLeft = matchLengthSeconds - elapsedSeconds;

        if (timeLeft <= 0) {
            timeLeft = 0;
            isGameOver = true;
        }
    }

    // draw the timer
    public void draw(Graphics g) {
        Font font = new Font("Arial", Font.BOLD, 16);
        g.setFont(font);
        int margin = 10;
        int panelWidth = g.getClipBounds().width;
        int timerY = 20;

        String timerText;

        // print in minute:seconds format if greater than a minute else print in seconds format
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

    // Set time directly from server broadcast
    public void setTimeLeft(int seconds) {
        this.timeLeft = seconds;
        this.isGameOver = seconds <= 0;
    }

    // getter for checking if the game is over
    public boolean isGameOver() {
        return isGameOver;
    }
}