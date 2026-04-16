package main.engine;

public class GameLoop implements Runnable {

    GamePanel panel;

    public GameLoop(GamePanel panel) {
        this.panel = panel;
    }

    @Override
    public void run() {
        while (true) {
            panel.updateGame();
            panel.repaint();

            try {
                Thread.sleep(16);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}