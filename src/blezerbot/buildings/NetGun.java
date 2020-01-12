package blezerbot.buildings;

import battlecode.common.*;
import java.util.*;
import blezerbot.*;

public class NetGun extends Building {

	public NetGun(RobotController rc) throws GameActionException {
		super(rc);
	}

	public void run() throws GameActionException {
		super.run();
		//Shoot enemy drones
		for(RobotInfo enemy: rc.senseNearbyRobots(-1, (rc.getTeam() == Team.A)?Team.A:Team.B)){
			if(enemy.type == robot_types[7]){
				if(rc.canShootUnit(enemy.ID)){
					rc.shootUnit(enemy.ID);
					break;
				}
			}
		}
	}

}