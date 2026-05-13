package main.model;

import java.awt.Rectangle;

import java.util.Random;

public class PowerUp {

    public int x, y;
    public int size = 30;

    public enum Type {
        SPEED, FREEZE, SHIELD, GHOST;
    }

    public Type type;

    public PowerUp(int x, int y, Type type) {
        this.x = x;
        this.y = y;
        this.type = type;
    }



	public Rectangle getBounds() {
        return new Rectangle(x, y, size, size);
    }

    public static Type getRandomType() {
        Type[] types = Type.values();
        return types[new Random().nextInt(types.length)];
    }
    
    
}