package testbot.buildings;

import battlecode.common.*;
import java.util.*;
import testbot.*;

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
			for (Direction dir : directions) {
				if (rc.canBuildRobot(RobotType.DELIVERY_DRONE, dir)) {
					rc.buildRobot(RobotType.DELIVERY_DRONE, dir);
					builtDrones++;
				}

			}
		}
	}

}