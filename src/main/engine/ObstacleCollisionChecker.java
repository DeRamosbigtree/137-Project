package main.engine;

import main.model.Entity;
import main.model.Player;

public class ObstacleCollisionChecker {
	
	GamePanel gp;

	public ObstacleCollisionChecker(GamePanel gp) {
		this.gp = gp;
	}
	
	public void checkTile(Entity entity) {
        // cast entity to player to access the correct x, y, and speed variables
        Player player = (Player) entity;

		int leftWorldX = player.x + player.solidArea.x;
		int rightWorldX = player.x + player.solidArea.x + player.solidArea.width;
		int topWorldY = player.y + player.solidArea.y;
		int bottomWorldY = player.y + player.solidArea.y + player.solidArea.height;
		
		int leftCol = leftWorldX / gp.tileSize;
		int rightCol = rightWorldX / gp.tileSize;
		int topRow = topWorldY / gp.tileSize;
		int bottomRow = bottomWorldY / gp.tileSize;
		
		int tileNum1, tileNum2;
        int speed = (int) player.speed;
		
		switch(player.direction) {
		case "up":
			topRow = (topWorldY - speed) / gp.tileSize; 
			tileNum1 = gp.tileM.mapTileNum[leftCol][topRow];
			tileNum2 = gp.tileM.mapTileNum[rightCol][topRow];
			
			if(gp.tileM.tile[tileNum1].collision || gp.tileM.tile[tileNum2].collision) {
				player.collisionOn = true;
			}
			break;
		case "down":
            bottomRow = (bottomWorldY + speed) / gp.tileSize; 
			tileNum1 = gp.tileM.mapTileNum[leftCol][bottomRow];
			tileNum2 = gp.tileM.mapTileNum[rightCol][bottomRow];
			
			if(gp.tileM.tile[tileNum1].collision || gp.tileM.tile[tileNum2].collision) {
				player.collisionOn = true;
			}
			break;
		case "left":
            leftCol = (leftWorldX - speed) / gp.tileSize; 
			tileNum1 = gp.tileM.mapTileNum[leftCol][topRow];
			tileNum2 = gp.tileM.mapTileNum[leftCol][bottomRow];
			
			if(gp.tileM.tile[tileNum1].collision || gp.tileM.tile[tileNum2].collision) {
				player.collisionOn = true;
			}
			break;
		case "right":
            rightCol = (rightWorldX + speed) / gp.tileSize; 
			tileNum1 = gp.tileM.mapTileNum[rightCol][topRow];
			tileNum2 = gp.tileM.mapTileNum[rightCol][bottomRow];
			
			if(gp.tileM.tile[tileNum1].collision || gp.tileM.tile[tileNum2].collision) {
				player.collisionOn = true;
			}
			break;
		}
	}
}