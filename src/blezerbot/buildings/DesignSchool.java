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
	int lastBuildTurn;
	final static int cooldown = blezerbot.units.Landscaper.blockedCap + 5;
	boolean waitingForDrone;
	DesignSchoolStatus status;
	boolean suicideTimer;
	int suicideTurns;

	public DesignSchool(RobotController rc) throws GameActionException {
		super(rc);
	}

	public void startLife() throws GameActionException{
		super.startLife();
		status = DesignSchoolStatus.MAKING;
		lastBuildTurn = -cooldown;
	}

	public void run() throws GameActionException {
		super.run();
		if (suicideTimer) {
			if (suicideTurns++ > 20) {
				rc.disintegrate();
			}
			return;
		}
		if (locHQ == null) return;
		if(enemyHQ != null && rc.getLocation().distanceSquaredTo(enemyHQ)<= 8){
			status = DesignSchoolStatus.RUSH_ENEMY_HQ;
		}
		// System.out.println(status);
		switch (status){
			// case TURTLE_MAKING:
			// 	int turn = rc.getRoundNum();
			// //	System.out.println("HERE");
			// 	/* for convenience of landscapers, try this specific location first */

			// 	Direction adj = rc.getLocation().directionTo(locHQ.add(locHQ.directionTo(rc.getLocation())));
			// 	//System.out.println(adj +  " ADJ");
			// 	if (rc.canBuildRobot(RobotType.LANDSCAPER, adj)) {
			// 		rc.buildRobot(RobotType.LANDSCAPER, adj);
			// 		builtLandscapers++;
			// 		lastBuildTurn = turn;
			// 	}

			// 	if (turn - lastBuildTurn >= cooldown) {
			// 		for (Direction dir : directions) {
			// 			if (rc.getLocation().add(dir).isAdjacentTo(locHQ)) {
			// 			//	System.out.println(dir + " YAY " +  rc.canBuildRobot(RobotType.LANDSCAPER, dir));
			// 				if (rc.canBuildRobot(RobotType.LANDSCAPER, dir)) {
			// 					rc.buildRobot(RobotType.LANDSCAPER, dir);
			// 					builtLandscapers++;
			// 					lastBuildTurn = turn;
			// 				}
			// 			}
			// 		}
			// 	}
				
			// 	break;
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
				if (numVaporators < 2 && builtLandscapers > 5) break;
				if (numVaporators<maxVaporators/2 && builtLandscapers > 10) break;

				if (rc.getLocation().distanceSquaredTo(locHQ) >= 8) { /* is this the first design school */
					/* for convenience of landscapers, try this specific location first */
					Direction adj = rc.getLocation().directionTo(locHQ.add(locHQ.directionTo(rc.getLocation())));
					if (rc.canBuildRobot(RobotType.LANDSCAPER, adj)) {
						rc.buildRobot(RobotType.LANDSCAPER, adj);
						builtLandscapers++;
					} else {
						for (Direction dir : directions) {
							if (rc.canBuildRobot(RobotType.LANDSCAPER, dir)) {
								rc.buildRobot(RobotType.LANDSCAPER, dir);
								builtLandscapers++;
							}
						}
					}
				} else {
					Direction bestDir = null;
					int mdist = Integer.MAX_VALUE;
					for (Direction dir : directions) {
						if (rc.canBuildRobot(RobotType.LANDSCAPER, dir)) {
							int ndist = rc.getLocation().add(dir).distanceSquaredTo(locHQ);
							if (ndist < mdist) {
								mdist = ndist;
								bestDir = dir;
							}
						}
					}

					if (bestDir != null) {
						rc.buildRobot(RobotType.LANDSCAPER, bestDir);
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

	public boolean executeMessage(Message message) throws GameActionException {
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
				suicideTimer = true;
				return true;

		}
		return false;
	}

}