package blezerbot.buildings;

import battlecode.common.*;
import java.util.*;
import blezerbot.*;

public class FulfillmentCenter extends Building {

	int builtDrones;

	public FulfillmentCenter(RobotController rc) throws GameActionException {
		super(rc);
	}

	public void run() throws GameActionException {
		super.run();
		if (builtDrones < 100000) {

			if(numDrones >= 2&& (numDefensiveNetGuns<= 1 &&rc.getRoundNum()<=500) && rc.getTeamSoup()<1+RobotType.NET_GUN.cost){
				return;
			}
			if(rc.getRoundNum()<=800 && r.nextInt(2)%2 == 0 &&  rc.getTeamSoup() < 1 + RobotType.LANDSCAPER.cost){
				return;
			}
			for (Direction dir : directions) {
				if (rc.canBuildRobot(RobotType.DELIVERY_DRONE, dir)) {
					rc.buildRobot(RobotType.DELIVERY_DRONE, dir);
					builtDrones++;
				}

			}
		}
	}

}