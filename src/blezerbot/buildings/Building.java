package blezerbot.buildings;

import battlecode.common.*;
import blezerbot.*;

public abstract class Building extends Robot {

	public Building(RobotController rc) throws GameActionException {
		super(rc);
	}

	boolean aboutToDie;

	int maxHealth;

	public void startLife() throws GameActionException {
		super.startLife();
	}

	public void run() throws GameActionException {
		super.run();
		int enemyLandscapers = 0;
		RobotInfo[] adjacentRobots = rc.senseNearbyRobots(2, rc.getTeam() == Team.A ? Team.B : Team.A);
		for (RobotInfo r : adjacentRobots) {
			if (r.getType() == RobotType.LANDSCAPER) enemyLandscapers++;
		}
		if (enemyLandscapers >= maxHealth - rc.getDirtCarrying()) {
			if (!aboutToDie) {
				aboutToDie = true;
				System.out.println("about to DIE");
				writeMessage(Message.death(rc.getID(), rc.getType(), rc.getLocation()));
				addMessageToQueue();
			}
		}
	}

}
