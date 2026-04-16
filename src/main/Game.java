package main;

import javax.swing.JFrame;
import main.engine.GamePanel;
import main.engine.GameLoop;

public class Game {
    public static void main(String[] args) {

        JFrame window = new JFrame("Tag Game");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setResizable(false);

        GamePanel panel = new GamePanel();
        window.add(panel);
        window.pack();

        window.setLocationRelativeTo(null);
        window.setVisible(true);

        new Thread(new GameLoop(panel)).start();
    }
}