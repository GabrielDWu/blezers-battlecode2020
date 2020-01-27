package blezerbot.buildings;

import battlecode.common.*;
import java.util.*;
import blezerbot.*;

public class DesignSchool extends Building {
	enum DesignSchoolStatus {
		TURTLING,
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
	boolean initialDS;
	int suicideTurns;

	public DesignSchool(RobotController rc) throws GameActionException {
		super(rc);
	}

	public void startLife() throws GameActionException{
		super.startLife();
		lastBuildTurn = -cooldown;
	}

	public void run() throws GameActionException {
		super.run();
		if (locHQ == null) return;
		if (initialDS) status = DesignSchoolStatus.MAKING;
		else status = DesignSchoolStatus.TURTLING;
		if(enemyHQ != null && rc.getLocation().distanceSquaredTo(enemyHQ)<= 8){
			status = DesignSchoolStatus.RUSH_ENEMY_HQ;
		}
		// System.out.println(status);
		switch (status){
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
				for (Direction dir : directions) {
					if (rc.canBuildRobot(RobotType.LANDSCAPER, dir) && builtLandscapers < TERRAFORM_LANDSCAPERS) {
						rc.buildRobot(RobotType.LANDSCAPER, dir);
						builtLandscapers++;
					}
				}
				if (builtLandscapers >= TERRAFORM_LANDSCAPERS) {
					rc.disintegrate();
				}
				break;
			case TURTLING:
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
			case UNWAIT:
				if (message.data[0] != rc.getID()) return false;
				if (message.data[1] == 7) {
					initialDS = true;
				}
				return true;
			case BUILD_WALL:
				if (status == DesignSchoolStatus.TURTLING) {
					rc.disintegrate();
					return true;
				}
		}
		return false;
	}

}