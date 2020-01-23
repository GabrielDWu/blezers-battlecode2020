package turtleblezer.buildings;

import battlecode.common.*;
import java.util.*;
import turtleblezer.*;

public class NetGun extends Building {

	public NetGun(RobotController rc) throws GameActionException {
		super(rc);
	}

	public void run() throws GameActionException {
		super.run();
		//Shoot enemy drones
		for(RobotInfo enemy: rc.senseNearbyRobots(-1, (rc.getTeam() == Team.B)?Team.A:Team.B)){
			if(enemy.type == RobotType.DELIVERY_DRONE){
				if(rc.canShootUnit(enemy.ID)){
					rc.shootUnit(enemy.ID);
					break;
				}
			}
		}
	}

}