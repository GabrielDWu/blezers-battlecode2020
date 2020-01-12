package blezerbot.units;

import battlecode.common.*;
import java.util.*;
import blezerbot.*;

public class DeliveryDrone extends Unit {
	enum DeliveryDroneStatus {
		PICK_UP,
		DROP_OFF,
		SEARCHING,
		FIND_ENEMY_HQ,
		RETURNING,
		NOTHING
	}
	DeliveryDroneStatus status;
	boolean findingEnemyHQ;
	MapLocation[] enemyHQs;
	MapLocation lastSeen;
	MapLocation dropLocation;
	int searchID = 0;
	int enemyHQc;

	public DeliveryDrone(RobotController rc) throws GameActionException {
		super(rc);
	}

	public void run() throws GameActionException {
		super.run();
		if (enemyHQs == null) {
			status = DeliveryDroneStatus.FIND_ENEMY_HQ;
			enemyHQs = new MapLocation[3];
			enemyHQc = -1;
		}
		if(rc.isCurrentlyHoldingUnit() && status != DeliveryDroneStatus.DROP_OFF){
			if(rc.canDropUnit(Direction.CENTER)){
				rc.dropUnit(Direction.CENTER);
			}
		}
		if (locHQ == null) return;
		if (status == DeliveryDroneStatus.FIND_ENEMY_HQ) {
			MapLocation enemyHQ = findEnemyHQ();
			if (enemyHQ != null) System.out.println("FOUND "+enemyHQ);
		}
		else if (status == DeliveryDroneStatus.PICK_UP && rc.isCurrentlyHoldingUnit() == false){
			pickUpID();
		}
		else if (status == DeliveryDroneStatus.DROP_OFF){
			dropOff();
		}
		else if (status == DeliveryDroneStatus.RETURNING){
			goTo(locHQ);
		}
	}
	void dropOff() throws GameActionException {
		if(rc.canSenseLocation(dropLocation)) {
			if (rc.isLocationOccupied(dropLocation) ) {
				if (rc.getLocation().isAdjacentTo(dropLocation)) {
					rc.dropUnit(rc.getLocation().directionTo(dropLocation));
					status = DeliveryDroneStatus.NOTHING;
					return;
				}
				goTo(dropLocation);
			}
			else{
				if(rc.canDropUnit(Direction.CENTER)){
					rc.dropUnit(Direction.CENTER);
				}
				status = DeliveryDroneStatus.RETURNING;
				return;
			}
		}
		goTo(dropLocation);

	}
	void pickUpID() throws GameActionException {
		RobotInfo[] robots = rc.senseNearbyRobots();
		for(RobotInfo r: robots){
			if(r.getID() == searchID){
				lastSeen = r.getLocation();
				chasePickUp(r);
				return;
			}
		}
		goTo(lastSeen);

	}

	void chasePickUp(RobotInfo target) throws GameActionException {
		if(rc.getLocation().isAdjacentTo(target.getLocation())){
			rc.pickUpUnit(searchID);
			status = DeliveryDroneStatus.DROP_OFF;
			return;
		}
		/// for now
		goTo(target.getLocation());
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
		return canMove(dir)&&(!netGunRadius(rc.getLocation().add(dir)));
	}

}
