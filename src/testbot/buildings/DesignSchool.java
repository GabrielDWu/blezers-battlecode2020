package testbot.buildings;

import battlecode.common.*;
import java.util.*;
import testbot.*;

public class DesignSchool extends Building {
	enum DesignSchoolStatus {
		TURTLING,
		MAKING,
		RUSH_ENEMY_HQ,
		NOTHING,
		TURTLING_CORNER
	}
	int builtLandscapers;
	int lastBuildTurn;
	final static int cooldown = blezerbot.units.Landscaper.blockedCap + 5;
	boolean waitingForDrone;
	DesignSchoolStatus status;
	boolean suicideTimer;
	boolean initialDS;
	int suicideTurns;
	int vapes;
	Direction turtleDir;

	public DesignSchool(RobotController rc) throws GameActionException {
		super(rc);
	}

	public void startLife() throws GameActionException{
		super.startLife();
		lastBuildTurn = -cooldown;
		turtleDir = null;
	}

	public void run() throws GameActionException {
		super.run();
		if (locHQ == null) return;
		if (status == null) {
			if (initialDS) status = DesignSchoolStatus.MAKING;
			else status = DesignSchoolStatus.TURTLING;
		}
		if(enemyHQ != null && rc.getLocation().distanceSquaredTo(enemyHQ)<= 8){
			status = DesignSchoolStatus.RUSH_ENEMY_HQ;
		}
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

				/* are we surrounded by terraform squares and not on one, if so, die */
				boolean surrounded = true;
				for (Direction dir : directions) {
					if (rc.canSenseLocation(rc.getLocation().add(dir)) && rc.senseElevation(rc.getLocation().add(dir)) != blezerbot.units.Unit.terraformHeight) {
						RobotInfo rinfo = rc.senseRobotAtLocation(rc.getLocation().add(dir));
						if (rinfo == null || rinfo.type != RobotType.LANDSCAPER) {
							surrounded = false;
							break;
						}
					}
				}
				if (surrounded && rc.senseElevation(rc.getLocation()) != blezerbot.units.Unit.terraformHeight) rc.disintegrate();
				
				break;
			case TURTLING:
				if (turtleDir != null) {
					if (rc.canBuildRobot(RobotType.LANDSCAPER, turtleDir)) {
						rc.buildRobot(RobotType.LANDSCAPER, turtleDir);
						lastBuildTurn = rc.getRoundNum();
						builtLandscapers++;
					} else {
						for (Direction dir : directions) {
							if (rc.canBuildRobot(RobotType.LANDSCAPER, dir)) {
								if (rc.getRoundNum() - lastBuildTurn >= cooldown) {
									rc.buildRobot(RobotType.LANDSCAPER, dir);
									lastBuildTurn = rc.getRoundNum();
									builtLandscapers++;
								}
							}
						}	
					}
				} else {
					for (Direction dir : directions) {
						if (rc.canBuildRobot(RobotType.LANDSCAPER, dir)) {
							turtleDir = dir;
							break;
						}
					}	
				}
				break;
			case TURTLING_CORNER:
					if (builtLandscapers >= 30) rc.disintegrate();
					System.out.println(vapes);
					if (vapes < 2) break;
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
				if (message.data[1] == 12) {
					rc.disintegrate();
				}
				return true;
			case BUILD_WALL:
				if (status == DesignSchoolStatus.TURTLING) {
					status = DesignSchoolStatus.TURTLING_CORNER;
					System.out.println("changed status");
					return true;
				}
				break;
			case BIRTH_INFO:
				RobotType unitType = robot_types[message.data[0]];
				int unitID = message.data[1];
				MapLocation location = new MapLocation(message.data[2], message.data[3]);
				if (unitType == RobotType.VAPORATOR) vapes++;
		}
		return false;
	}

}