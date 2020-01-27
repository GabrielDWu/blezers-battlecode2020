package blezerbot.units;

import battlecode.common.*;
import java.util.*;
import blezerbot.*;
// import javafx.beans.binding.IntegerBinding;

import static blezerbot.units.Landscaper.terraformHeight;

public class DeliveryDrone extends Unit {

	enum DeliveryDroneStatus {
		PICK_UP,
		DROP_OFF,
		FIND_ENEMY_HQ,
		RETURNING,
		CIRCLING,	//Surrouning HQ in preparation for attack
		ATTACKING,	//Diving in to abduct landscapers from enemy wall
		NOTHING,
		HARASS,
		DROP_WATER,
		DEFENDING_HQ,
	}

	DeliveryDroneStatus status;

	boolean findingEnemyHQ;
	MapLocation[] enemyHQs;
	int[][] flooded;

	MapLocation investigate;
	MapLocation harassCenter;
	final static int harassRadius = 100; // Radius of circle about which you circle
	MapLocation dropLocation;
	MapLocation waitLocation;
	MapLocation lastSeen;

	final int wallThreshold = terraformHeight + 1; // detection of turtle landscapers

	MapLocation closeWater;
	int closeWaterDist;	//King distance to closeWater
	ArrayList<MapLocation> waterLocations;

	MapLocation searchDest;	//Utility variable for findWater
	DeliveryDroneStatus prevStatus;	//What to do once DROP_WATER is done.
	int searchDestTries;

	int searchID;
	int enemyHQc;

	int rushRound = -1;
	final static int circleThreshold = 12;
	boolean sentFound = false;
	Team holdingTeam = null;

	final static int cornerThreshold = 3;
	final static int waterSenseRadius = 8;
	public DeliveryDrone(RobotController rc) throws GameActionException {
		super(rc);
	}

	public boolean badMap(){
		boolean b1 = (locHQ.x<=cornerThreshold || locHQ.x>=rc.getMapWidth() - cornerThreshold);
		boolean b2 = (locHQ.y<=cornerThreshold || locHQ.y>=rc.getMapHeight() - cornerThreshold);
		return (b1|| b2);
	}

	public boolean HQInCorner(){
		boolean b1 = (locHQ.x<=cornerThreshold || locHQ.x>=rc.getMapWidth() - cornerThreshold);
		boolean b2 = (locHQ.y<=cornerThreshold || locHQ.y>=rc.getMapHeight() - cornerThreshold);
		return (b1 && b2);
	}

	MapLocation getCloseCornerHQ(){
		int x, y;
		if(locHQ.x<=cornerThreshold) x = 0;
		else x = rc.getMapWidth()-1;
		if(locHQ.y<=cornerThreshold) y = 0;
		else y = rc.getMapHeight()-1;
		return new MapLocation(x, y);
	}

	public void startLife() throws GameActionException{
		super.startLife();
		status = DeliveryDroneStatus.FIND_ENEMY_HQ;
		flooded = new int[rc.getMapHeight()][rc.getMapWidth()];
		enemyHQs = new MapLocation[3];
		enemyHQc = -1;
		rushRound = -1;
	}

	boolean adjacentToBase() throws GameActionException{
		// for now i'll do this
		for(Direction dir: directions){
			MapLocation nloc = rc.getLocation().add(dir);
			if(rc.canSenseLocation(nloc) == false) continue;
			RobotInfo rinfo = rc.senseRobotAtLocation(nloc);
			if(rinfo != null &&
					(rinfo.type == RobotType.LANDSCAPER||rinfo.type ==RobotType.HQ) &&
					rinfo.getTeam() == rc.getTeam() &&
					rc.senseElevation(nloc)>=wallThreshold){
				return true;
			}
		}
		return false;
	}

	boolean adjacentToBase(MapLocation loc) throws GameActionException{
		// for now i'll do this
		for(Direction dir: directions){
			MapLocation nloc = loc.add(dir);
			if(rc.canSenseLocation(nloc) == false) continue;
			RobotInfo rinfo = rc.senseRobotAtLocation(nloc);
			if(rinfo != null &&
					(rinfo.type == RobotType.LANDSCAPER||rinfo.type ==RobotType.HQ) &&
					rinfo.getTeam() == rc.getTeam() &&
					rc.senseElevation(nloc)>=wallThreshold){
				return true;
			}
		}
		return false;
	}

	public void run() throws GameActionException {
		super.run();

		//Update closest water
		if(closeWater != null && rc.canSenseLocation(closeWater) && (!rc.senseFlooding(closeWater) || rc.isLocationOccupied(closeWater))) closeWater=null;

		ArrayList<MapLocation> senseLocations = getLocationsInRadius(rc.getLocation(), Math.min(rc.getCurrentSensorRadiusSquared(), waterSenseRadius));

		for(MapLocation loc: senseLocations){
			if(rc.canSenseLocation(loc) && rc.senseFlooding(loc) && !rc.isLocationOccupied(loc) &&
					(closeWater == null || kingDistance(rc.getLocation(),loc) <= closeWaterDist)){
				closeWater = loc;
				closeWaterDist = kingDistance(rc.getLocation(),loc);
			}
		};

		if(rc.isCurrentlyHoldingUnit() && !(status == DeliveryDroneStatus.DROP_OFF || status == DeliveryDroneStatus.DROP_WATER) && holdingTeam !=rc.getTeam()){
			for(Direction dir: directions){
				if(rc.canDropUnit(dir)){
					rc.dropUnit(dir);
					holdingTeam  = null;
				}
			}
		}

		//if (locHQ == null) return;

		if(rc.isCurrentlyHoldingUnit() && status != DeliveryDroneStatus.DEFENDING_HQ && holdingTeam != rc.getTeam()){
			if(status != DeliveryDroneStatus.DROP_WATER) prevStatus = status;
			status = DeliveryDroneStatus.DROP_WATER;
		}

		// assuming we will only harass their hq or ours
		// start the bad hardcoding of conditions
		int random = r.nextInt(100);
		if(random>10 && enemyHQ != null && harassCenter!= null &&
				harassCenter.distanceSquaredTo(enemyHQ) <= harassCenter.distanceSquaredTo(locHQ)
				&& status == DeliveryDroneStatus.HARASS){
			status = DeliveryDroneStatus.DEFENDING_HQ;
		}
		else {
			if(status != DeliveryDroneStatus.DEFENDING_HQ && enemyHQ != null){
				harassCenter = enemyHQ;
				status = DeliveryDroneStatus.HARASS;
			}
			else status = DeliveryDroneStatus.DEFENDING_HQ;
		}

		// should i rush or not
		if(rushRound != -1){
			status = DeliveryDroneStatus.CIRCLING;
		}
		if(rushRound +120 <= rc.getRoundNum()  && rushRound != -1) {
			status = DeliveryDroneStatus.ATTACKING;
		}
		if(rushRound + 175<= rc.getRoundNum() && rushRound!=-1){
			status = DeliveryDroneStatus.DEFENDING_HQ;
			rushRound = -1;
		}

		if(enemyHQ == null) status = DeliveryDroneStatus.FIND_ENEMY_HQ;

		switch(status) {
			case DEFENDING_HQ:
				if(locHQ == null){
					status = DeliveryDroneStatus.NOTHING;
					break;
				}

				//Update investigate
				if(rc.isCurrentlyHoldingUnit()){
					for(Direction drop: directionswcenter){
						MapLocation nloc = rc.getLocation().add(drop);
						if(rc.canSenseLocation(nloc) && rc.senseFlooding(nloc) ){
							if(rc.canDropUnit(drop) && holdingTeam != rc.getTeam()){
								rc.dropUnit(drop);
								holdingTeam = null;
							}
						}
					}
				}

				if(investigate != null) {
					if(rc.canSenseLocation(investigate)) {
						RobotInfo rob = rc.senseRobotAtLocation(investigate);
						if (rob == null || rob.getTeam() == rc.getTeam() || rob.getType() == RobotType.DELIVERY_DRONE) {
							investigate = null;
						}
					}else{
						investigate = null;
					}
				}

				boolean closeToHQ = kingDistance(locHQ, rc.getLocation()) <= 2;
				boolean pickedUp = false;

				for(Direction dir: directionswcenter){
					MapLocation nloc = rc.getLocation().add(dir);
					if(rc.canSenseLocation(nloc) == false) continue;
					RobotInfo rinfo = rc.senseRobotAtLocation(nloc);
					if(rinfo == null) continue;
					if(rinfo.getTeam() != rc.getTeam()&& rinfo.type != RobotType.DELIVERY_DRONE&& !isBuilding(rinfo.type) ){
						if(rc.canPickUpUnit(rinfo.getID())){
							rc.pickUpUnit(rinfo.getID());
							pickedUp = true;
							break;
						}
					}
				}

				if(pickedUp){
					//do nothing
				}
				else {
					if (closeToHQ){
						if(!badMap()) break;
						MapLocation use = locHQ;
						if(HQInCorner()) use = getCloseCornerHQ();
						Direction dir = rc.getLocation().directionTo(use);
						for (int i = 0; i < 8; i++) {
							Direction nxt = directions[(getDirectionValue(dir) + i) % 8];
							if (adjacentToBase(rc.getLocation().add(nxt)) && rc.canMove(nxt)) {
								rc.move(nxt);
								break;
							}
						}
						break;
					}
					Direction bestDir = null;
					int dist = rc.getLocation().distanceSquaredTo(locHQ);
					Direction dir = rc.getLocation().directionTo(locHQ);
					if (!badMap()){
						for (int i = 0; i < 4; i++) {
							Direction nxt = directions[(getDirectionValue(dir) + i) % 8];
							if (rc.canMove(nxt) && rc.getLocation().add(nxt).distanceSquaredTo(locHQ) < dist &&
									kingDistance(rc.getLocation().add(nxt), locHQ) >= 2) {
								bestDir = nxt;
								dist = rc.getLocation().add(nxt).distanceSquaredTo(locHQ);
							}
						}
						if (bestDir != null) {
							rc.move(bestDir);
						}
						else {
							for (int i = 0; i < 4; i++) {
								Direction nxt = directions[(getDirectionValue(dir) + i) % 8];
								if (rc.canMove(nxt)) {
									rc.move(nxt);
									break;
								}
							}
						}
					}
					else{
						MapLocation use = locHQ;
						if(HQInCorner()) use = getCloseCornerHQ();
						dir = rc.getLocation().directionTo(use);
						if(rc.getLocation().distanceSquaredTo(use)>= circleThreshold &&goToFixed(use)){
							break;
						}
						else {
							for (int i = 0; i < 4; i++) {
								Direction nxt = directions[(getDirectionValue(dir) + i) % 8];
								if (rc.canMove(nxt)) {
									rc.move(nxt);
									break;
								}
							}
						}
					}
				}
				break;

			case FIND_ENEMY_HQ:
				if (enemyHQ != null) {
					status = DeliveryDroneStatus.HARASS;
					harassCenter = enemyHQ;
					break;
				}

				MapLocation loc = findEnemyHQ();
				if (loc != null && !sentFound) {
					enemyHQ = loc;
					writeMessage(Message.enemyHqLocation(enemyHQ));
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
							holdingTeam = other.getTeam();
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
				if(harassCenter == null){
					status = DeliveryDroneStatus.NOTHING;
					break;
				}

				//Update investigate
				if(investigate != null) {
					if(rc.canSenseLocation(investigate)) {
						RobotInfo rob = rc.senseRobotAtLocation(investigate);
						if (rob == null || rob.getTeam() == rc.getTeam() || rob.getType() == RobotType.DELIVERY_DRONE) {
							investigate = null;
						}
					}else{
						investigate = null;
					}
				}

				for(RobotInfo enemy: rc.senseNearbyRobots(-1, (rc.getTeam() == Team.B)?Team.A:Team.B)){
					if(enemy.type != RobotType.DELIVERY_DRONE && !isBuilding(enemy.type)){
						if(investigate == null || rc.getLocation().distanceSquaredTo(enemy.getLocation()) <
								rc.getLocation().distanceSquaredTo(investigate)) {
							investigate = enemy.getLocation();
						}
					}
				}

				if(investigate != null) {
					if (rc.getLocation().isAdjacentTo(investigate)) {
						int robID = rc.senseRobotAtLocation(investigate).getID();
						if (rc.canPickUpUnit(robID)) {
							holdingTeam = rc.senseRobotAtLocation(investigate).getTeam();
							rc.pickUpUnit(robID);
							status = DeliveryDroneStatus.DROP_WATER;
							prevStatus = DeliveryDroneStatus.HARASS;
						}
					}
					else {
						goTo(investigate);
					}
				}
				else{
					if(rc.getLocation().distanceSquaredTo(harassCenter) > harassRadius){
						goTo(harassCenter);
					}
					else{
						Direction ccw = rc.getLocation().directionTo(harassCenter).rotateRight().rotateRight();
						randomOrthogonalMove();
					}
				}
				break;

            case DROP_WATER:
            	if(rc.isCurrentlyHoldingUnit() == false) {
            		status = prevStatus;
            		break;
				}
            	if(closeWater == null){
            		findWater();
				}
            	else{
            		if(rc.getLocation().isAdjacentTo(closeWater)){
            			if(rc.canDropUnit(rc.getLocation().directionTo(closeWater))){
            				rc.dropUnit(rc.getLocation().directionTo(closeWater));
            				holdingTeam = null;

            				if(prevStatus == null){
            					status = DeliveryDroneStatus.NOTHING;
							}
            				else{
            					status = prevStatus;
							}
						}
					}
            		else{
						goTo(closeWater);
					}
				}
            	break;

		}

	}

	void findWater() throws GameActionException {
		if(closeWater != null) return;

		MapLocation myloc = rc.getLocation(); 
		if(searchDest == null || searchDestTries <= 0){
			searchDest = new MapLocation(r.nextInt(rc.getMapWidth()), rc.getMapHeight());
			searchDestTries = kingDistance(myloc, searchDest) * 3 / 2;
		}

		goTo(searchDest);
		searchDestTries--;
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

	void dropWater() throws GameActionException{
	    if(closeWater == null){
	        findWater();
	        return;
        }

	    if(rc.getLocation().isAdjacentTo(closeWater)){
	        if(rc.canDropUnit(rc.getLocation().directionTo(closeWater))){
                rc.dropUnit(rc.getLocation().directionTo(closeWater));
                holdingTeam = null;
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

	void dropOff() throws GameActionException {
		if(rc.canSenseLocation(dropLocation)) {
			if (rc.isLocationOccupied(dropLocation) == false) {
				if (rc.getLocation().isAdjacentTo(dropLocation)) {
					rc.dropUnit(rc.getLocation().directionTo(dropLocation));
					holdingTeam = null;
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
			holdingTeam = rc.senseRobotAtLocation(target.getLocation()).getTeam();
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

	public boolean executeMessage(Message message) throws GameActionException {
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
				int ri = r.nextInt(5);
				if(ri<=2) return false;
				if(!adjacentToBase() && status != DeliveryDroneStatus.CIRCLING && status != DeliveryDroneStatus.ATTACKING && !(status == DeliveryDroneStatus.DROP_OFF || status == DeliveryDroneStatus.DROP_WATER)) rushRound = rc.getRoundNum();
				if(!adjacentToBase() && status != DeliveryDroneStatus.CIRCLING && status != DeliveryDroneStatus.ATTACKING && !(status == DeliveryDroneStatus.DROP_OFF || status == DeliveryDroneStatus.DROP_WATER)) status = DeliveryDroneStatus.CIRCLING;

			/*	if(status == DeliveryDroneStatus.CIRCLING && rc.getLocation().distanceSquaredTo(enemyHQ)<40){
					status = DeliveryDroneStatus.ATTACKING;
				}*/

				return true;
			case HARASS:
				if (message.data[0] != rc.getID()) return false;
				status = DeliveryDroneStatus.HARASS;
				harassCenter = new MapLocation(message.data[1], message.data[2]);
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
		return rc.canMove(dir)&&(!netGunRadius(rc.getLocation().add(dir))) &&
				((enemyHQ != null && rc.getLocation().add(dir).distanceSquaredTo(enemyHQ) > 15) ||
						(enemyHQ == null && (enemyHQc < 0 ||
								rc.getLocation().add(dir).distanceSquaredTo(enemyHQs[enemyHQc]) > 15)));
	}

	public void goTo(MapLocation loc) throws GameActionException {
		if(!rc.isReady() || rc.getLocation().equals(loc)) return;
		if (unitVisited == null) return;
		if (!loc.equals(dest)) {
			dest = loc;
			lastDist = 0;
			hugging = false;
			unitVisitedIndex++;
			if (unitVisitedIndex > 15) {
				unitVisited = new long[rc.getMapWidth()][rc.getMapHeight()];
				unitVisitedIndex = 0;
			}
		}
		MapLocation mloc = rc.getLocation();
		incUnitVisited(mloc);
		if (getUnitVisited(mloc) > 4) {
			randomOrthogonalMove();
			return;
		}
		//Check if still should be hugging (if nothing around you, hugging=false)
		if(hugging){
			hugging = false;
			for(Direction dir : orthogonalDirections){
				if(!canMove(dir)){
					hugging = true;
					break;
				}
			}
		}
		if (!hugging) {
			Direction dir;
			if(Math.abs(mloc.x - loc.x) > Math.abs(mloc.y - loc.y)){
				if(mloc.x > loc.x){
					dir = Direction.WEST;
				}else{
					dir = Direction.EAST;
				}
			}else{
				if(mloc.y > loc.y){
					dir = Direction.SOUTH;
				}else{
					dir = Direction.NORTH;
				}
			}

			if (tryMove(dir)) return;

			//Turn right until you see an empty space
			facing = dir;
			int cnt = 0;
			while(!canMove(facing)){
				facing = facing.rotateRight().rotateRight();
				cnt++;
				if(cnt>4) return;
			}
			lastDist = mloc.distanceSquaredTo(loc);
			hugging = true;
		}
		if (mloc.distanceSquaredTo(loc) < lastDist) {
			hugging = false;
			goTo(loc);
			return;
		}
		Direction dir = facing.rotateLeft().rotateLeft();
		//Left turn
		if(tryMove(dir)){
			facing=dir;
			return;
		}
		dir = dir.rotateRight().rotateRight();

		//Forward
		if(tryMove(dir))return;

		dir = dir.rotateRight().rotateRight();

		//Right turn
		if(tryMove(dir)){
			facing = dir;
			return;
		}
	}
	public boolean goToFixed(MapLocation loc) throws GameActionException {
		if(!rc.isReady() || rc.getLocation().equals(loc)) return false;
		if (unitVisited == null) return false;
		if (!loc.equals(dest)) {
			dest = loc;
			lastDist = 0;
			hugging = false;
			unitVisitedIndex++;
			if (unitVisitedIndex > 15) {
				unitVisited = new long[rc.getMapWidth()][rc.getMapHeight()];
				unitVisitedIndex = 0;
			}
		}
		MapLocation mloc = rc.getLocation();
		incUnitVisited(mloc);
		if (getUnitVisited(mloc) > 4) {
			return false;
		}
		//Check if still should be hugging (if nothing around you, hugging=false)
		if(hugging){
			hugging = false;
			for(Direction dir : orthogonalDirections){
				if(!canMove(dir)){
					hugging = true;
					break;
				}
			}
		}
		if (!hugging) {
			Direction dir;
			if(Math.abs(mloc.x - loc.x) > Math.abs(mloc.y - loc.y)){
				if(mloc.x > loc.x){
					dir = Direction.WEST;
				}else{
					dir = Direction.EAST;
				}
			}else{
				if(mloc.y > loc.y){
					dir = Direction.SOUTH;
				}else{
					dir = Direction.NORTH;
				}
			}

			if (tryMove(dir)) return true;

			//Turn right until you see an empty space
			facing = dir;
			int cnt = 0;
			while(!canMove(facing)){
				facing = facing.rotateRight().rotateRight();
				cnt++;
				if(cnt>4) return false;
			}
			lastDist = mloc.distanceSquaredTo(loc);
			hugging = true;
		}
		if (mloc.distanceSquaredTo(loc) < lastDist) {
			hugging = false;
			goTo(loc);
			return true;
		}
		Direction dir = facing.rotateLeft().rotateLeft();
		//Left turn
		if(tryMove(dir)){
			facing=dir;
			return true;
		}
		dir = dir.rotateRight().rotateRight();

		//Forward
		if(tryMove(dir))return true;

		dir = dir.rotateRight().rotateRight();

		//Right turn
		if(tryMove(dir)){
			facing = dir;
			return true;
		}
		return false;
	}

}
