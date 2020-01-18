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
	ArrayList<MapLocation> waterLocations = new ArrayList<MapLocation>();
	int searchID = 0;
	int enemyHQc;
	boolean sentFound = false;

	public DeliveryDrone(RobotController rc) throws GameActionException {
		super(rc);
	}


	public void run() throws GameActionException {
		super.run();
		if (enemyHQs == null && enemyHQ == null && turnCount > 9) {
			status = DeliveryDroneStatus.FIND_ENEMY_HQ;
			enemyHQs = new MapLocation[3];
			enemyHQc = -1;
		}
		if(rc.isCurrentlyHoldingUnit() && status != DeliveryDroneStatus.DROP_OFF){
			for(Direction dir: directions){
				if(rc.canDropUnit(dir)){
					rc.dropUnit(dir);
				}
			}
		}
		if (locHQ == null) return;
		if (status == DeliveryDroneStatus.FIND_ENEMY_HQ) {
			MapLocation loc = findEnemyHQ();
			if (loc != null && !sentFound){
				enemyHQ = loc;
				writeMessage(Message.enemyHqLocation(enemyHQ));
				addMessageToQueue();
				sentFound = true;
			}

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
	void searchWater(){
		// write search for water
	}

	MapLocation closestSafe(MapLocation loc) throws GameActionException {
		ArrayList<MapLocation>  nearby = getLocationsInRadius(rc.getLocation(), rc.getCurrentSensorRadiusSquared());
		MapLocation best = null;
		int closest = Integer.MAX_VALUE;
		for(MapLocation x: nearby){
			if(rc.senseRobotAtLocation(x) == null && rc.senseFlooding(x) == false && netGunRadius(x) == false){
				if(best == null){
					best = x;
					closest = loc.distanceSquaredTo(x);
				}
				else{
					if(closest>loc.distanceSquaredTo(x)){
						best = x;
						closest = loc.distanceSquaredTo(x);
					}
				}
			}
		}
		return best;
	}
	void dropWater(){
		if(waterLocations.size() == 0){
			searchWater();
		}
		else{
			MapLocation best = null;
			int closest = Integer.MAX_VALUE;
			for(MapLocation x: waterLocations){
				if(x.distanceSquaredTo(rc.getLocation())<closest){
					best = x;
					closest = x.distanceSquaredTo(rc.getLocation());
				}
			}
			dropLocation = best;
		}
	}
	void patrolEnemy(){

	}
	void dropOff() throws GameActionException {
		if(rc.canSenseLocation(dropLocation)) {
			if (rc.isLocationOccupied(dropLocation) == false) {
				if (rc.getLocation().isAdjacentTo(dropLocation)) {
					rc.dropUnit(rc.getLocation().directionTo(dropLocation));
					status = DeliveryDroneStatus.NOTHING;
					return;
				}
			}
			else{
				MapLocation close = closestSafe(dropLocation);
				if(close == null){
					status = DeliveryDroneStatus.RETURNING;
					return;
				}
				else{
					dropLocation = close;
					dropOff();
				}
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
		if (rc.getCurrentSensorRadiusSquared() > GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED /*|| rc.getLocation().distanceSquaredTo(enemyHQs[enemyHQc]) > 20*/) goTo(enemyHQs[enemyHQc]);
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

	public boolean executeMessage(Message message){
		/*Returns true if message applies to me*/
		if(super.executeMessage(message)){
			return true;
		}
		switch (message.type) {
			case TRANSPORT:
				if (message.data[0] != rc.getID()) return false;
				searchID = message.data[1];
				lastSeen = new MapLocation(message.data[2], message.data[3]);
				dropLocation = new MapLocation(message.data[4], message.data[5]);
				status = DeliveryDroneStatus.PICK_UP;
				return true;
		}
		return false;
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

	public boolean netGunRadius(MapLocation loc) throws GameActionException{
		if(rc.canSenseLocation(loc) == false) return false;
		RobotInfo [] robots = rc.senseNearbyRobots(rc.getCurrentSensorRadiusSquared(), (rc.getTeam() == Team.A ? Team.B : Team.A));
		for(RobotInfo r: robots){
			if(r.getType() == RobotType.NET_GUN || r.getType() == RobotType.HQ){
				if(loc.distanceSquaredTo(r.getLocation()) <= GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED){
					return true;
				}
			}
		}
		return false;
	}

	public boolean tryMove(Direction dir) throws GameActionException {
	    if (rc.isReady() && canMove(dir)) {
	        rc.move(dir);
	        return true;
	    } else return false;
	}

	public boolean canMove(Direction dir) throws GameActionException {
		return rc.canMove(dir)&&(!netGunRadius(rc.getLocation().add(dir)));
	}

}
