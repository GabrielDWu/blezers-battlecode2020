package blezerbot.buildings;

import battlecode.common.*;
import java.util.*;
import blezerbot.*;

public class DesignSchool extends Building {

	int builtLandscapers;

	public DesignSchool(RobotController rc) throws GameActionException {
		super(rc);
	}

	public void run() throws GameActionException {
		super.run();
		if (builtLandscapers < 2) {
			for (Direction dir : directions) {
				if (rc.canBuildRobot(RobotType.LANDSCAPER, dir)) {
					rc.buildRobot(RobotType.LANDSCAPER, dir);
					builtLandscapers++;
				}
			}
		}
	}

}