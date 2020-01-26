package blezerbot.buildings;

import battlecode.common.*;
import java.util.*;
import blezerbot.*;

public class DesignSchool extends Building {
	enum DesignSchoolStatus {
		TURTLE_MAKING,
		MAKING,
		RUSH_ENEMY_HQ,
		NOTHING
	}
	int builtLandscapers;
	boolean waitingForDrone;
	DesignSchoolStatus status;

	public DesignSchool(RobotController rc) throws GameActionException {
		super(rc);
	}

	public void startLife() throws GameActionException{
		super.startLife();
		status = DesignSchoolStatus.TURTLE_MAKING;
	}

	public void run() throws GameActionException {
		super.run();
		if (locHQ == null) return;
		if(enemyHQ != null && rc.getLocation().distanceSquaredTo(enemyHQ)<= 8){
			status = DesignSchoolStatus.RUSH_ENEMY_HQ;
		}
		switch (status){
			case TURTLE_MAKING:
				/* for convenience of landscapers, try this specific location first */
				Direction adj = rc.getLocation().directionTo(locHQ.add(locHQ.directionTo(rc.getLocation())));
				if (rc.canBuildRobot(RobotType.LANDSCAPER, adj)) {
					rc.buildRobot(RobotType.LANDSCAPER, adj);
					builtLandscapers++;
				}

				for (Direction dir : directions) {
					if (rc.getLocation().add(dir).isAdjacentTo(locHQ)) {
						if (rc.canBuildRobot(RobotType.LANDSCAPER, dir)) {
							rc.buildRobot(RobotType.LANDSCAPER, dir);
							builtLandscapers++;
						}
					}
				}
				break;
			case RUSH_ENEMY_HQ:
				/// don't want to waste resources
				if(builtLandscapers>3) break;
				Direction di = null;
				int best = Integer.MAX_VALUE;
				for(Direction d: directions){
					if(rc.canBuildRobot(RobotType.LANDSCAPER, d) && enemyHQ.distanceSquaredTo(rc.getLocation().add(d)) <= best){
						best = Integer.MAX_VALUE;
						di = d;
					}
				}
				if(di != null) {
					rc.buildRobot(RobotType.LANDSCAPER, di);
					builtLandscapers++;
				}
				//System.out.println(rc.getTeamSoup() + " SOUP");
			case MAKING:
				if(numVaporators<maxVaporators) break;
				if(builtLandscapers > 20) break;
				for (Direction dir : directions) {
					if (rc.canBuildRobot(RobotType.LANDSCAPER, dir)) {
						rc.buildRobot(RobotType.LANDSCAPER, dir);
						builtLandscapers++;
					}
				}
				break;
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
					break;
				}
			}
		}*/
	}

	public boolean executeMessage(Message message){
		/*Returns true if message applies to me*/
		if(super.executeMessage(message)){
			return true;
		}
		switch (message.type) {
			case WAIT:
				if (message.data[0] != rc.getID()) return false;
				status = DesignSchoolStatus.NOTHING;
				return true;
			case BUILD_WALL:
				//when landscapers start turtling, no longer need to new scapers next to hq
				status = DesignSchoolStatus.MAKING;
				return true;

		}
		return false;
	}

}