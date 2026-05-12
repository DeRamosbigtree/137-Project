package main.model;

import java.awt.Rectangle;

public class Trap {
    public int x, y, size = 20;
    public int ownerId;

    public Trap(int x, int y, int ownerId) {
        this.x = x;
        this.y = y;
        this.ownerId = ownerId;
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, size, size);
    }
    

    
}