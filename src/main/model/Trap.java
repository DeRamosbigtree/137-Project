package main.model;

import java.awt.Rectangle;

public class Trap {
    public int x, y, size = 20;
    public Player owner;

    public Trap(int x, int y, Player owner) {
        this.x = x;
        this.y = y;
        this.owner = owner;
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, size, size);
    }
    

    
}