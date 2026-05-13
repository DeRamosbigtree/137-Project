package main.model;

import java.awt.Rectangle;

public class Entity {
	
	public int worldX, worldY;
	public int speed;
	public String direction ="down";
	public int spriteCounter = 0;
	public int spriteNum = 1;
	public Rectangle solidArea;
	public boolean collisionOn = false;

	public Entity() {
		// TODO Auto-generated constructor stub
	}

}
