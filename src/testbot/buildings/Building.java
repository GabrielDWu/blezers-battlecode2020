package testbot.buildings;

import battlecode.common.*;
import testbot.*;

public abstract class Building extends Robot {

	public Building(RobotController rc) throws GameActionException {
		super(rc);
	}

	boolean aboutToDie;

	int maxHealth;

	public void startLife() throws GameActionException {
		super.startLife();
		switch (rc.getType()) {
			case HQ: maxHealth = 50; break;
			case REFINERY: maxHealth = 15; break;
			case VAPORATOR: maxHealth = 15; break;
			case DESIGN_SCHOOL: maxHealth = 15; break;
			case FULFILLMENT_CENTER: maxHealth = 15; break;
			case NET_GUN: maxHealth = 15; break;
		}
	}

	public void run() throws GameActionException {
		super.run();
		int enemyLandscapers = 0;
		RobotInfo[] adjacentRobots = rc.senseNearbyRobots(2, rc.getTeam() == Team.A ? Team.B : Team.A);
		for (RobotInfo r : adjacentRobots) {
			if (r.getType() == RobotType.LANDSCAPER) enemyLandscapers++;
		}
		MapLocation mloc = rc.getLocation();
		if (enemyLandscapers >= maxHealth - rc.getDirtCarrying() || (rc.senseElevation(mloc) <= GameConstants.getWaterLevel(rc.getRoundNum()+1) && (rc.canSenseLocation(mloc.add(Direction.NORTH)) && rc.senseFlooding(mloc.add(Direction.NORTH)) || rc.canSenseLocation(mloc.add(Direction.NORTHEAST)) && rc.senseFlooding(mloc.add(Direction.NORTHEAST)) || rc.canSenseLocation(mloc.add(Direction.EAST)) && rc.senseFlooding(mloc.add(Direction.EAST)) || rc.canSenseLocation(mloc.add(Direction.SOUTHEAST)) && rc.senseFlooding(mloc.add(Direction.SOUTHEAST)) || rc.canSenseLocation(mloc.add(Direction.SOUTH)) && rc.senseFlooding(mloc.add(Direction.SOUTH)) || rc.canSenseLocation(mloc.add(Direction.SOUTHWEST)) && rc.senseFlooding(mloc.add(Direction.SOUTHWEST)) || rc.canSenseLocation(mloc.add(Direction.WEST)) && rc.senseFlooding(mloc.add(Direction.WEST)) || rc.canSenseLocation(mloc.add(Direction.NORTHWEST)) && rc.senseFlooding(mloc.add(Direction.NORTHWEST)) ))) {
			if (!aboutToDie) {
				aboutToDie = true;
				writeMessage(Message.death(rc.getID(), rc.getType(), rc.getLocation()));
				addMessageToQueue();
			}
		}
	}

}
