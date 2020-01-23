package turtleblezer.buildings;

import battlecode.common.*;
import java.util.*;
import turtleblezer.*;

public class DesignSchool extends Building {

	int builtLandscapers;
	boolean waitingForDrone;

	public DesignSchool(RobotController rc) throws GameActionException {
		super(rc);
	}

	public void run() throws GameActionException {
		super.run();
		if (locHQ == null) return;
		for (Direction dir : directions) {
			if (rc.getLocation().add(dir).isAdjacentTo(locHQ)) {
				if (rc.canBuildRobot(RobotType.LANDSCAPER, dir)) {
					rc.buildRobot(RobotType.LANDSCAPER, dir);
				}
			}
		}
		/*if (builtLandscapers < 16) {
			for (Direction dir : directions) {
				if (rc.canBuildRobot(RobotType.LANDSCAPER, dir)) {
					rc.buildRobot(RobotType.LANDSCAPER, dir);
					builtLandscapers++;
				}
			}
		}*/

		// drone carry to enemy HQ
		/*
		if (enemyHQ != null) {
			for (Direction dir : directions) {
				if (rc.canBuildRobot(RobotType.LANDSCAPER, dir)) {
					rc.buildRobot(RobotType.LANDSCAPER, dir);
					waitingForDrone = true;
					int lid = rc.senseRobotAtLocation(rc.getLocation().add(dir)).getID();
					System.out.println(lid);
					writeMessage(5, new int[]{lid});
					addMessageToQueue();
					break;
				}
			}
		}*/
	}

}