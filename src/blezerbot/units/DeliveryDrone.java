package blezerbot.units;

import battlecode.common.*;
import java.util.*;
import blezerbot.*;

public class DeliveryDrone extends Unit {
	enum DeliveryDroneStatus {
		PICK_UP,
		DROP_OFF,
		SEARCH_WATER,
		FIND_ENEMY_HQ,
		RETURNING,
		CIRCLING,	//Surrouning HQ in preparation for attack
		ATTACKING,	//Diving in to abduct landscapers from enemy wall
		NOTHING,
		HARASS,
		DROP_WATER
	}
	DeliveryDroneStatus status;
	boolean findingEnemyHQ;
	MapLocation[] enemyHQs;
	int[][] flooded;
	MapLocation lastSeen;
	MapLocation dropLocation;
	MapLocation waitLocation;
	MapLocation closeWater;
	ArrayList<MapLocation> waterLocations = new ArrayList<MapLocation>();
	int searchID = 0;
	int enemyHQc;
	Direction spiral = Direction.SOUTH;
	boolean sentFound = false;

	public DeliveryDrone(RobotController rc) throws GameActionException {
		super(rc);

	}

	public void startLife() throws GameActionException{
		super.startLife();
		status = DeliveryDroneStatus.FIND_ENEMY_HQ;
		//status = DeliveryDroneStatus.SEARCH_WATER;
		flooded = new int[rc.getMapHeight()][rc.getMapWidth()];
		enemyHQs = new MapLocation[3];
		enemyHQc = -1;
	}


	public void run() throws GameActionException {
		super.run();

		if(rc.isCurrentlyHoldingUnit() && !(status == DeliveryDroneStatus.DROP_OFF || status == DeliveryDroneStatus.ATTACKING)){
			for(Direction dir: directions){
				if(rc.canDropUnit(dir)){
					rc.dropUnit(dir);
				}
			}
		}
		if (locHQ == null) return;
		switch(status) {
			case FIND_ENEMY_HQ:
				if (enemyHQ != null) {
					status = DeliveryDroneStatus.CIRCLING;
					break;
				}
				MapLocation loc = findEnemyHQ();
				if (loc != null && !sentFound) {
					enemyHQ = loc;
					writeMessage(Message.enemyHqLocation(enemyHQ));
					addMessageToQueue();
					sentFound = true;
				}
				break;

			case PICK_UP:
				if(rc.isCurrentlyHoldingUnit() == false) {
					pickUpID();
				}
				break;
			case DROP_OFF:
				dropOff();
				break;
			case RETURNING:
				goTo(locHQ);
				break;
			case CIRCLING:
				if (enemyHQ == null) {
					status = DeliveryDroneStatus.FIND_ENEMY_HQ;
					break;
				}
				if (rc.getLocation().add(rc.getLocation().directionTo(enemyHQ)).distanceSquaredTo(enemyHQ)
						> GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED) {
					goTo(enemyHQ);
					break;
				}
				if (waitLocation == null || (!rc.getLocation().equals(waitLocation) &&
						rc.canSenseLocation(waitLocation) && rc.isLocationOccupied(waitLocation))) {
					MapLocation potential = new MapLocation(enemyHQ.x - 3, enemyHQ.y - 3);
					if (rc.canSenseLocation(potential) && !rc.isLocationOccupied(potential)) {
						waitLocation = potential;
					}
					potential = new MapLocation(enemyHQ.x - 3, enemyHQ.y + 3);
					if (rc.canSenseLocation(potential) && !rc.isLocationOccupied(potential)) {
						waitLocation = potential;
					}
					potential = new MapLocation(enemyHQ.x + 3, enemyHQ.y - 3);
					if (rc.canSenseLocation(potential) && !rc.isLocationOccupied(potential)) {
						waitLocation = potential;
					}
					potential = new MapLocation(enemyHQ.x + 3, enemyHQ.y + 3);
					if (rc.canSenseLocation(potential) && !rc.isLocationOccupied(potential)) {
						waitLocation = potential;
					}
				}
				if (waitLocation != null) {
					goTo(waitLocation);
				}
				break;
			case ATTACKING:
				if (!rc.isCurrentlyHoldingUnit()) {
					for (RobotInfo other : rc.senseNearbyRobots(2, (rc.getTeam() == Team.B) ? Team.A : Team.B)) {
						if (rc.canPickUpUnit(other.getID())) {
							rc.pickUpUnit(other.getID());
						}
					}
					Direction d = rc.getLocation().directionTo(enemyHQ);
					//Move in the general direction of hq
					if (rc.canMove(d)) rc.move(d);
					else if (rc.canMove(d.rotateLeft())) rc.move(d.rotateLeft());
					else if (rc.canMove(d.rotateRight())) rc.move(d.rotateRight());
				} else {
					Direction d = rc.getLocation().directionTo(enemyHQ).opposite();
					//Move in the general opposite direction of hq
					if (rc.canMove(d)) rc.move(d);
					else if (rc.canMove(d.rotateLeft())) rc.move(d.rotateLeft());
					else if (rc.canMove(d.rotateRight())) rc.move(d.rotateRight());
					if (!netGunRadius(rc.getLocation())) {
						status = DeliveryDroneStatus.CIRCLING;
					}
				}
				break;
			case HARASS:
				break;

			case SEARCH_WATER:
				findWater();
				break;

            case DROP_WATER:

		}


	}

	void findWater() throws GameActionException {
		if(waterLocations.size()>0) return;
		MapLocation myloc = rc.getLocation();
	/*	visited[myloc.x][myloc.y] = 2;
		if(!rc.canSenseLocation(new MapLocation(0, 0))){
			goTo(new MapLocation(0, 0));
		}
		MapLocation cur = rc.getLocation();
		cur = cur.add(spiral);
		while(rc.canSenseLocation(cur)){
			if(visited[cur.x][cur.y] == 2) {
				spiral = directionCounterClockwise(spiral);
				spiral = directionCounterClockwise(spiral);
			}
		}
		if(canMove(spiral)){
			rc.move(spiral);
		}*/
	}

	MapLocation closestSafe(MapLocation loc) throws GameActionException {
		ArrayList<MapLocation>  nearby = getLocationsInRadius(rc.getLocation(), rc.getCurrentSensorRadiusSquared());
		MapLocation best = null;
		int closest = Integer.MAX_VALUE;
		for(MapLocation x: nearby){
			if(rc.senseRobotAtLocation(x) == null && netGunRadius(x) == false){
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

	void dropWater() throws GameActionException {
	    if(closeWater == null){
	        findWater();
	        return;
        }

	    if(rc.getLocation().isAdjacentTo(closeWater)){
	        if(rc.canDropUnit(rc.getLocation().directionTo(closeWater))){
                rc.dropUnit(rc.getLocation().directionTo(closeWater));
            }
        }

		if(waterLocations.size() == 0){
            findWater();
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
			case DRONE_ATTACK:
				if(status == DeliveryDroneStatus.CIRCLING && rc.getLocation().distanceSquaredTo(enemyHQ)<40){
					status = DeliveryDroneStatus.ATTACKING;
				}
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
		RobotInfo [] robots = rc.senseNearbyRobots(-1, (rc.getTeam() == Team.A ? Team.B : Team.A));
		for(RobotInfo r: robots){
			if(r.getType() == RobotType.NET_GUN || r.getType() == RobotType.HQ){
				if(loc.distanceSquaredTo(r.getLocation()) <= GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED){
					return true;
				}
			}
		}
		return false;
	}

	public boolean canMove(Direction dir) throws GameActionException {
		switch (dir) {
			case NORTHEAST:
			case NORTHWEST:
			case SOUTHEAST:
			case SOUTHWEST:
				return false;
		}
		return rc.canMove(dir)&&(!netGunRadius(rc.getLocation().add(dir)))&&((enemyHQ != null && rc.getLocation().add(dir).distanceSquaredTo(enemyHQ) > 15) || (enemyHQ == null && rc.getLocation().add(dir).distanceSquaredTo(enemyHQs[enemyHQc]) > 15));
	}

}
