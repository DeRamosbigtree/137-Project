package main.engine;

import main.model.Entity;

public class ObstacleCollisionChecker {
	
	GamePanel gp;

	public ObstacleCollisionChecker(GamePanel gp) {
		this.gp = gp;
	}
	
	public void checkTile(Entity entity) {
		int leftWorldX = entity.worldX + entity.solidArea.x;
		int rightWorldX = entity.worldX + entity.solidArea.x + entity.solidArea.width;
		int topWorldY = entity.worldY + entity.solidArea.y;
		int bottomWorldY = entity.worldY + entity.solidArea.y + entity.solidArea.height;
		
		int leftCol = leftWorldX/gp.tileSize;
		int rightCol = rightWorldX/gp.tileSize;
		int topRow = topWorldY/gp.tileSize;
		int bottomRow = topWorldY/gp.tileSize;
		
		int tileNum1, tileNum2;
		
		switch(entity.direction) {
		case "up":
			topRow = (topWorldY - entity.speed)/gp.tileSize; // predicts where player is trying to move
			tileNum1 = gp.tileM.mapTileNum[leftCol][topRow];
			tileNum2 = gp.tileM.mapTileNum[rightCol][topRow];
			
			if(gp.tileM.tile[tileNum1].collision == true || gp.tileM.tile[tileNum2].collision == true) {
				entity.collisionOn = true;
			}
			break;
		case "down":
			break;
		case "left":
			break;
		case "right":
			break;
		}
	}

}
