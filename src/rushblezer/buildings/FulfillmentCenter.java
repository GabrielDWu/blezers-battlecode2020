package rushblezer.buildings;

import battlecode.common.*;
import java.util.*;
import rushblezer.*;

public class FulfillmentCenter extends Building {

	int builtDrones;

	public FulfillmentCenter(RobotController rc) throws GameActionException {
		super(rc);
	}

	public void run() throws GameActionException {
		super.run();
		if (builtDrones < 1) {
			for (Direction dir : directions) {
				if (rc.canBuildRobot(RobotType.DELIVERY_DRONE, dir)) {
					rc.buildRobot(RobotType.DELIVERY_DRONE, dir);
					builtDrones++;
				}
			}
		}
	}

}