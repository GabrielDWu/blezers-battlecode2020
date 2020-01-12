package blezerbot.units;

import battlecode.common.*;
import java.util.*;
import blezerbot.*;

public class DeliveryDrone extends Unit {

	boolean findingEnemyHQ;
	MapLocation[] enemyHQs;
	int enemyHQc;

	public DeliveryDrone(RobotController rc) throws GameActionException {
		super(rc);
	}

	public void run() throws GameActionException {
		super.run();
		if (enemyHQs == null) {
			findingEnemyHQ = true;
			enemyHQs = new MapLocation[3];
			enemyHQc = -1;
		}
		if (locHQ == null) return;
		if (findingEnemyHQ) {
			MapLocation loc = findEnemyHQ();
			if (loc != null){
				enemyHQ = loc;
				System.out.println("FOUND "+enemyHQ);
				writeMessage(2, new int[]{enemyHQ.x, enemyHQ.y});
				addMessageToQueue();
			}
		}
	}

	public MapLocation findEnemyHQ() throws GameActionException {
		if (enemyHQc == -1) {
			findingEnemyHQ = true;
			MapLocation mloc = rc.getLocation();
			int h = rc.getMapHeight();
			int w = rc.getMapWidth();
			MapLocation horizontal = new MapLocation(((((w-1-locHQ.x)%w)+w)%w), locHQ.y);
			MapLocation vertical = new MapLocation(locHQ.x, ((((h-1-locHQ.y)%h)+h)%h));
			MapLocation diagonal = new MapLocation(horizontal.x, vertical.y);
			// check these two first since they are the closet
			if (mloc.distanceSquaredTo(horizontal) < mloc.distanceSquaredTo(vertical)) {
				enemyHQs[0] = horizontal;
				enemyHQs[1] = diagonal;
				enemyHQs[2] = vertical;
			} else {
				enemyHQs[0] = vertical;
				enemyHQs[1] = diagonal;
				enemyHQs[2] = horizontal;
			}
			enemyHQc = 0;
		}
		goTo(enemyHQs[enemyHQc]);
		MapLocation r = detectEnemyHQ();
		if (r != null) {
			findingEnemyHQ = false;
			return r;
		} else if (rc.canSenseLocation(enemyHQs[enemyHQc])) {
			enemyHQc++;
			if (enemyHQc >= enemyHQs.length) {
				findingEnemyHQ = false;
			}
		}
		return null;
	}

	public MapLocation detectEnemyHQ() {
		RobotInfo[] near = rc.senseNearbyRobots(rc.getCurrentSensorRadiusSquared(), (rc.getTeam() == Team.A ? Team.B : Team.A));
		for(RobotInfo x: near){
			if(x.getType() == RobotType.HQ){
				return x.location;
			}
		}
		return null;
	}

	public boolean tryMove(Direction dir) throws GameActionException {
	    if (rc.isReady() && rc.canMove(dir)) {
	        rc.move(dir);
	        return true;
	    } else return false;
	}

	public boolean canMove(Direction dir) throws GameActionException {
		return rc.canMove(dir);
	}

}
