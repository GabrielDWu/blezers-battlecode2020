package blezerbot.units;

import battlecode.common.*;
import java.util.*;
import blezerbot.*;

public class Landscaper extends Unit {

	enum LandscaperStatus {
		ATTACKING_HQ,
		DEFENDING,
		NOTHING,
		BUILDING,
        TERRAFORMING,
		BURY_ENEMY_BUILDING,
		HQ_TERRAFORM,
		CORNER
	}
	LandscaperStatus status;
	MapLocation buryTarget = null;
	Direction terraformTarget;
	MapLocation locDS;
	int filledOffset;
	RobotType attackType = RobotType.HQ;
	int idDS;
	boolean doneMoving; /* are we in proper wall position */
	int moveTries; /* how many times have we tried to move here */
	boolean tryingClockwise;
	boolean movedOnWall;
	final static int moveCap = 15; /* how many tries to move into a position without our robot */
	final static int blockedCap = 125; /* how many tries to move into a position with our robot */
	final static int terraformHeight = 10; /* how high should I make the land? */
	final static int terraformDist = 3; /* how far should I be from the hq before starting? */
	final static int terraformThreshold = 25; /* what height is too high/low to terraform? */
	final static int terraformTries = 20; /* how many random moves away from hq to try? */
	int currentTerraformRadiusSquared = 1;
	int currentDirection = 0; // 0 cw 1 ccw
	LandscaperStatus lastStatus;
	public Landscaper(RobotController rc) throws GameActionException {
		super(rc);
	}
	public void startLife() throws GameActionException{
		super.startLife();
		status = LandscaperStatus.NOTHING;
		idDS = -1;
		doneMoving = false;
		moveTries = 0;
		tryingClockwise = true;
		movedOnWall = false;
		terraformTarget = null;
		lastStatus = LandscaperStatus.NOTHING;
	}
	public int buryPriority(RobotType r){
		if(r == RobotType.NET_GUN) return 0;
		if(r == RobotType.FULFILLMENT_CENTER) return 1;
		if(r == RobotType.DESIGN_SCHOOL) return 2;
		if(r == RobotType.VAPORATOR) return 3;
		if(r == RobotType.REFINERY) return 4;
		return -1;
	}

	public void run() throws GameActionException {

	//	System.out.println("UGH" + " " + rc.getRoundNum());
		super.run();
	//	System.out.println(status);
		if(status==LandscaperStatus.NOTHING)debug("NOTHING");
		visited[rc.getLocation().x][rc.getLocation().y]++;
		MapLocation mloc = rc.getLocation();
		Direction d = null;
		if(buryTarget == null){
			RobotInfo[] nearRobots = rc.senseNearbyRobots();
			for(RobotInfo r: nearRobots){
				if(r.type != RobotType.HQ){
					if(r.getTeam() != rc.getTeam()){
						int priority = buryPriority(r.type);
						if(priority == -1) continue;
						else{
							if(buryTarget == null) buryTarget = r.location;
							else{
								if(buryPriority(rc.senseRobotAtLocation(buryTarget).type) > buryPriority(r.type)){
									buryTarget = r.location;
								}
							}
							if (status != LandscaperStatus.NOTHING) lastStatus = status;
							else if (mloc.isAdjacentTo(locHQ)) lastStatus = LandscaperStatus.DEFENDING;
							else lastStatus = LandscaperStatus.TERRAFORMING;
							status = LandscaperStatus.BURY_ENEMY_BUILDING;
						}
					}
				}
			}
		}
		// System.out.println(status + " " + lastStatus);
		if(locHQ != null){
			d = rc.getLocation().directionTo(locHQ);
		}
		if(enemyHQ != null && enemyHQ.distanceSquaredTo(rc.getLocation())<= 10 && surroundedLocation(enemyHQ) == false){
			status = LandscaperStatus.ATTACKING_HQ;
		}
		if(status == LandscaperStatus.TERRAFORMING) status = LandscaperStatus.HQ_TERRAFORM;
		System.out.println(status);
		switch (status) {
			case ATTACKING_HQ:
				if(surroundedLocation(enemyHQ)){
					status = LandscaperStatus.TERRAFORMING;
					break;
				}
				Direction attackDir = rc.getLocation().directionTo(enemyHQ);
				if(rc.getDirtCarrying() < 1){
					Direction dir = randomDirection();
					while(dir==attackDir){
						dir = randomDirection();
					}
					if(rc.canDigDirt(dir)) rc.digDirt(dir);
				}
				if(enemyHQ != null){
					if(!rc.getLocation().isAdjacentTo(enemyHQ)){
						goTo(enemyHQ);
					}
					else{
						if(rc.canDepositDirt(attackDir)){
							rc.depositDirt(attackDir);
						}
					}
				}
				break;
			case DEFENDING:
				if (locHQ == null) {
					return;
				}
				if (idDS != -1 && !rc.canSenseRobot(idDS) && mloc.distanceSquaredTo(locDS) <= rc.getCurrentSensorRadiusSquared()) {
					status = LandscaperStatus.BUILDING;
				} else {
					boolean[] filled = new boolean[8];
					int filledUpTo = -1;
					if (locDS != null) filled[(locHQ.directionTo(rc.getLocation()).ordinal()+filledOffset)%8] = true;
					RobotInfo[] r = rc.senseNearbyRobots(locHQ, 5, rc.getTeam());
					for (int i = 0; i < r.length; i++) {
						if(locDS == null && r[i].getType() == RobotType.DESIGN_SCHOOL) {
							locDS = r[i].getLocation();
							idDS = r[i].getID();
							filledOffset = ((-locHQ.directionTo(locDS).ordinal()-1)%8+8)%8;
						}else if(locDS != null && r[i].getType() == RobotType.LANDSCAPER){
							if(r[i].getLocation().isAdjacentTo(locHQ)){
								//filled[0] is Northeast, then proceeds on clockwise
								filled[(locHQ.directionTo(r[i].getLocation()).ordinal()+filledOffset)%8] = true;
							}
						}
					}
				//	System.out.println("HERE");
					// for(int i=0; i<8; i++){
					// 	if(filled[i]){
					// 		filledUpTo = i;
					// 	}else{
					// 		break;
					// 	}
					// }
					if (!doneMoving) {
					//	System.out.println("!doneMoving");
						Direction moveDir = getNextWallDirection(tryingClockwise);

						if (rc.canSenseLocation(mloc.add(moveDir)) && !isOurRobot(mloc.add(moveDir))) {
						//	System.out.println("WHY");
							boolean done = false;
							int diff = rc.senseElevation(mloc.add(moveDir)) - rc.senseElevation(mloc);
							//debug("DIFF " + diff);
							if (diff > 0) {
								// if (tryingClockwise) System.out.println("DIFF " + diff + " " + rc.canDigDirt(moveDir));
								if (rc.canDigDirt(moveDir)) {
									rc.digDirt(moveDir);
									done = true;
								} else {
									if (attackEnemyBuilding()) done = true;
									Direction dir = findWallLattice(mloc);
									if (dir != null) {
										if (rc.canDepositDirt(dir)) {
											rc.depositDirt(dir);
											done = true;
										}
									}
								}
							} else if (diff < 0) {
								if (rc.getDirtCarrying() < 1) {
									Direction mdir = null;
									int mdirt = Integer.MIN_VALUE;
									for (Direction dir : directions) {
										if (rc.canSenseLocation(mloc.add(dir)) && isLattice(mloc.add(dir))) {
											int ndirt = rc.senseElevation(mloc.add(dir));
											if (ndirt > mdirt && rc.canDigDirt(dir)) {
												mdir = dir;
												mdirt = ndirt;
											}
										}
									}
									if (mdir != null && rc.canDigDirt(mdir)) {
										rc.digDirt(mdir);
										done = true;
									}
								}
								else {
									if (attackEnemyBuilding()) done = true;
									if (rc.canDepositDirt(moveDir)) {
										rc.depositDirt(moveDir);
										done = true;
									}
								}
							}

							if (!done) {
								// System.out.println(moveTries);
								/* if totally necessary, replace this with filled logic (and re-test it) */
								if (!isValidWall(mloc.add(moveDir)) || mloc.add(moveDir).equals(locHQ.add(locHQ.directionTo(locDS)))) { 
									if (tryingClockwise && !movedOnWall) tryingClockwise = false;
									else doneMoving = true;
								} else if (isOurRobot(mloc.add(moveDir)) && Math.abs(rc.senseElevation(mloc.add(moveDir)) - rc.senseElevation(mloc)) > 5) { /* 5 is arbitrary */
									if (tryingClockwise && !movedOnWall) tryingClockwise = false;
									else doneMoving = true;
								} else {
									if (isOurRobot(mloc.add(moveDir))) { /* replace with filled later */
										moveTries++;
										if (moveTries >= blockedCap) {
											if (tryingClockwise && !movedOnWall) tryingClockwise = false;
											else doneMoving = true;
											moveTries = 0;
										}
									} else if (tryMove(moveDir)) {
										moveTries = 0;
										movedOnWall = true;
									} else {
										moveTries++;
										if (moveTries >= moveCap) {
											if (tryingClockwise && !movedOnWall) tryingClockwise = false;
											else doneMoving = true;
											moveTries = 0;
										}
									}
								}
							}
						}
						else doneMoving = true;
					}

					if (doneMoving) {
					//	System.out.println("HUH");
						if(rc.canDigDirt(d)) rc.digDirt(d);//heal hq
						if (rc.getDirtCarrying() < 1) {
							Direction mdir = null;
							int mdirt = Integer.MIN_VALUE;
							for (Direction dir : directions) {
								if (rc.canSenseLocation(mloc.add(dir)) && isLattice(mloc.add(dir))) {
									int ndirt = rc.senseElevation(mloc.add(dir));
									if (ndirt > mdirt && rc.canDigDirt(dir)) {
										mdir = dir;
										mdirt = ndirt;
									}
								}
							}
							if (mdir != null && rc.canDigDirt(mdir)) rc.digDirt(mdir);
						} else {
							attackEnemyBuilding();
							if (rc.canDepositDirt(Direction.CENTER)) rc.depositDirt(Direction.CENTER);
						}
					}
					break;
				}
			case BUILDING:
				if (rc.getRoundNum() % 5 == 0) {
					if (!correctWall()) {
						reinforceWall(mloc, d);
					}
				} else {
					reinforceWall(mloc, d);
				}

				break;
			case CORNER:
				if(locHQ == null) return;
				boolean inPlace = (kingDistance(locHQ, rc.getLocation())==2) && !isLattice(rc.getLocation());
				if(inPlace) {
					if (rc.getDirtCarrying() < 1) {
						Direction mdir = null;
						int mdirt = Integer.MIN_VALUE;
						for (Direction dir : directions) {
							if (rc.canSenseLocation(mloc.add(dir)) && isLattice(mloc.add(dir))) {
								int ndirt = rc.senseElevation(mloc.add(dir));
								if (ndirt > mdirt && rc.canDigDirt(dir)) {
									mdir = dir;
									mdirt = ndirt;
								}
							}
						}
						if (mdir != null && rc.canDigDirt(mdir)) rc.digDirt(mdir);
					} else {
						//Make sure it won't flood soon
						if(rc.senseElevation(rc.getLocation()) <= GameConstants.getWaterLevel(rc.getRoundNum()+5)) {
							if (rc.canDepositDirt(Direction.CENTER)) rc.depositDirt(Direction.CENTER);
						}else {
							attackEnemyBuilding();
							Direction mdir = null;
							int mdirt = Integer.MAX_VALUE;
							for (Direction dir : directions) {
								if (rc.canSenseLocation(mloc.add(dir)) && mloc.add(dir).isAdjacentTo(locHQ)) {
									int ndirt = rc.senseElevation(mloc.add(dir));
									if (ndirt < mdirt && rc.canDepositDirt(dir)) {
										mdir = dir;
										mdirt = ndirt;
									}
								}
							}
							rc.depositDirt(mdir);
						}
					}

				}else {
					Direction dir = rc.getLocation().directionTo(locHQ);

					if(canMove(dir)) {
						rc.move(dir);
					}else {
						for(int i=0; i<8; i++) {
							dir = dir.rotateLeft();
							if(canMove(dir) && !isLattice(mloc.add(dir))) {
								rc.move(dir);
								break;
							}
						}
					}
				}

				break;
			case TERRAFORMING:
				if (kingDistance(mloc, locHQ) < terraformDist || isLattice(mloc)) {
					/* if we're too close to HQ, move */
					/* also if we're in a lattice square, move */

					if (enemyHQ != null) moveTowardEnemyHQ(mloc);
					else moveAwayFromHQ(mloc);
				} else {
					Direction nearLattice = findLattice(mloc);
					if (nearLattice != null) {
						if (!tryTerraform(mloc, nearLattice)) {
							if (enemyHQ != null) moveTowardEnemyHQ(mloc);
							else moveAwayFromHQ(mloc);
						}
					} else {
						if (enemyHQ != null) moveTowardEnemyHQ(mloc);
						else moveAwayFromHQ(mloc);
					}
				}

				break;
			case HQ_TERRAFORM:
				if (kingDistance(mloc, locHQ) < terraformDist || isLattice(mloc)) {
					/* if we're too close to HQ, move */
					/* also if we're in a lattice square, move */

					if (enemyHQ != null) moveTowardEnemyHQ(mloc);
					else moveAwayFromHQ(mloc);
				} else {
					Direction nearLattice = findLattice(mloc);
					tryTerraform(mloc, Direction.CENTER, nearLattice);
					if (terraformTarget != null) {
						if (!tryTerraform(mloc, terraformTarget, nearLattice)) {
							tryMove(terraformTarget);
							terraformTarget = null;
						}
					}

					if (terraformTarget == null) {
						Direction dir = bestTerraform(nearLattice);

						if (dir == null) {
							if (enemyHQ != null) moveTowardEnemyHQ(mloc);
							else moveAwayFromHQ(mloc);
						} else {
							terraformTarget = dir;
						}
					}
				}
				break;
			case BURY_ENEMY_BUILDING:
				if(buryTarget == null || surroundedLocation(buryTarget)){
					status = lastStatus;
					break;
				}
				attackDir = rc.getLocation().directionTo(buryTarget);
				if(rc.getDirtCarrying() < 1){
					Direction dir = randomDirection();
					while(dir==attackDir){
						dir = randomDirection();
					}
					if(rc.canDigDirt(dir)) rc.digDirt(dir);
				}
				if(buryTarget != null){
					if(!rc.getLocation().isAdjacentTo(buryTarget)){
						goTo(buryTarget);
					}
					else{
						if(rc.canDepositDirt(attackDir)){
							rc.depositDirt(attackDir);
							/// if buried go back to terraforming
							if(rc.senseRobotAtLocation(rc.getLocation().add(attackDir)) == null){
								status = lastStatus;
								break;
							}
						}
					}
				}
				break;
		}
	}

	public void reinforceWall(MapLocation mloc, Direction d) throws GameActionException {
		if (rc.getDirtCarrying() < 1) {
			/* heal HQ */
			if (rc.canDigDirt(d)) {
				rc.digDirt(d);
			}

			Direction mdir = null;
			int mdirt = Integer.MIN_VALUE;
			for (Direction dir : directions) {
				if (rc.canSenseLocation(mloc.add(dir)) && isLattice(mloc.add(dir))) {
					int ndirt = rc.senseElevation(mloc.add(dir));
					if (ndirt > mdirt && rc.canDigDirt(dir)) {
						mdir = dir;
						mdirt = ndirt;
					}
				}
			}
			if (mdir != null && rc.canDigDirt(mdir)) rc.digDirt(mdir);
		} else {
			Direction mdir = null;
			int mdirt = Integer.MAX_VALUE;
			for (Direction dir : directionswcenter) {
				if (rc.canSenseLocation(mloc.add(dir))) {
					int ndirt = rc.senseElevation(mloc.add(dir));
					if (mloc.add(dir).isAdjacentTo(locHQ) && !mloc.add(dir).equals(locHQ) && ndirt < mdirt && rc.canDepositDirt(dir)) {
						mdir = dir;
						mdirt = ndirt;
					}
				}
			}
			if (mdir != null && rc.canDepositDirt(mdir)) rc.depositDirt(mdir);
		}
	}

	public boolean moveTowardEnemyHQ(MapLocation mloc) throws GameActionException {
		int startIndex = r.nextInt(directions.length);
		int stopIndex = startIndex;
		int currentDist = taxicabDistance(mloc, enemyHQ);

		for (int i = 0; i < terraformTries; i++) {
			int ind = r.nextInt(directions.length);
			Direction dir = directions[ind];
			MapLocation nloc = mloc.add(dir);

			if (taxicabDistance(nloc, enemyHQ) <= currentDist && !isLattice(nloc)) {
				if (tryMove(dir)) return true;
			}
		}

		do {
			Direction dir = directions[startIndex];
			MapLocation nloc = mloc.add(dir);

			if (taxicabDistance(nloc, enemyHQ) <= currentDist && !isLattice(nloc)) {
				if (tryMove(dir)) return true;
			}

			++startIndex;
			startIndex %= directions.length;
		} while (startIndex != stopIndex);

		return false;
	}

	/* pick a random move taking me not closer to the HQ */
	public boolean moveAwayFromHQ(MapLocation mloc) throws GameActionException {
		int startIndex = r.nextInt(directions.length);
		int stopIndex = startIndex;
		int currentDist = taxicabDistance(mloc, locHQ);

		for (int i = 0; i < terraformTries; i++) {
			int ind = r.nextInt(directions.length);
			Direction dir = directions[ind];
			MapLocation nloc = mloc.add(dir);

			if (taxicabDistance(nloc, locHQ) >= currentDist && !isLattice(nloc)) {
				if (tryMove(dir)) return true;
			}
		}

		do {
			Direction dir = directions[startIndex];
			MapLocation nloc = mloc.add(dir);

			if (taxicabDistance(nloc, locHQ) >= currentDist && !isLattice(nloc)) {
				if (tryMove(dir)) return true;
			}

			++startIndex;
			startIndex %= directions.length;
		} while (startIndex != stopIndex);

		return false;
	}
	public boolean isOurRobot(MapLocation loc)  throws GameActionException {
		RobotInfo info = rc.senseRobotAtLocation(loc);

		if (info == null) return false;

		return info.team == rc.getTeam();
	}

	public boolean isOurBuilding(MapLocation loc) throws GameActionException {
		RobotInfo info = rc.senseRobotAtLocation(loc);

		if (info == null) return false;

		return info.team == rc.getTeam() && info.type.isBuilding();
	}

	/* look for any square we can terraform
	look at center first, so we can do stuff 
	*/
	public boolean tryTerraform(MapLocation mloc, Direction nearLattice) throws GameActionException {
		if (tryTerraform(mloc, Direction.CENTER, nearLattice)) return true;

		for (Direction dir: directions) {
			MapLocation nloc = mloc.add(dir);

			if (!isLattice(nloc) && rc.onTheMap(nloc)) {
				if (tryTerraform(mloc, dir, nearLattice)) return true;
			}
		}

		return false;
	}
	public Direction bestTerraform(Direction nearLattice) throws GameActionException {
		Direction dir = null;
		int best = Integer.MAX_VALUE;
		for(Direction d: directions){
			MapLocation nloc = rc.getLocation().add(d);
			if(isLattice(nloc) || rc.onTheMap(nloc) == false) continue;
			if(kingDistance(nloc, locHQ) < terraformDist){
				continue;
			}
			if(canTerraform(rc.getLocation(), d, nearLattice)){
				if(dir == null){
					dir = d;
					best = nloc.distanceSquaredTo(locHQ);
				}
				else if(best >= nloc.distanceSquaredTo(locHQ)){
					best = nloc.distanceSquaredTo(locHQ);
					dir = d;
				}
			}
		}
		return dir;
	}
	public boolean canTerraform(MapLocation mloc, Direction dir, Direction nearLattice) throws GameActionException{
		MapLocation nloc = mloc.add(dir);
		if(rc.canSenseLocation(nloc) == false) return false;
		int currentElevation = rc.senseElevation(mloc);
		int newElevation = rc.senseElevation(nloc);
		if(Math.abs(newElevation - currentElevation)>terraformThreshold) return false;
		if(newElevation == terraformHeight) return false;
		if(isOurBuilding(nloc)) return false;
		if(newElevation>terraformHeight){
			if(rc.getDirtCarrying() >= RobotType.LANDSCAPER.dirtLimit){
				if(attackEnemyBuilding()) return true;
				if(rc.canDepositDirt(nearLattice)) return true;
			}
			else{
				if(rc.canDigDirt(dir)) return true;
			}
		}
		else{
			if(rc.getDirtCarrying()<1){
				if(rc.canDigDirt(nearLattice)) return true;
			}
			else{
				if(rc.canDepositDirt(dir)) return true;
			}
		}
		return false;
	}
	/* try to terraform a specific square */
	public boolean tryTerraform(MapLocation mloc, Direction dir, Direction nearLattice) throws GameActionException {
		MapLocation nloc = mloc.add(dir);
		int currentElevation = rc.senseElevation(mloc);
		int newElevation = rc.senseElevation(nloc);

		/* either too high, too low, or already good */
		if (Math.abs(newElevation - currentElevation) > terraformThreshold) return false;
		if (newElevation == terraformHeight) return false;
		if (isOurBuilding(nloc)) return false;
		
		if (newElevation > terraformHeight) { /* if our target square is higher, dig from it */
			if (rc.getDirtCarrying() >= RobotType.LANDSCAPER.dirtLimit) {
				if (attackEnemyBuilding()) return true;
				if (rc.canDepositDirt(nearLattice)) {
					rc.depositDirt(nearLattice);
					return true;
				}
			} else {
				if (rc.canDigDirt(dir)) {
					rc.digDirt(dir);
					return true;
				}
			}
		} else { /* otherwise deposit to it */
			if (rc.getDirtCarrying() < 1) {
				if (rc.canDigDirt(nearLattice)) {
					rc.digDirt(nearLattice);
					return true;
				}
			} else {
				if (rc.canDepositDirt(dir)) {
					rc.depositDirt(dir);
					return true;
				}
			}
		}
		return false;
	}

	/* find any corner-adjacent lattice point */
	/* as a note, this can probably be made more efficient if bytecode is an issue */
	public Direction findLattice(MapLocation mloc) {
		for (Direction dir: directions) {
			MapLocation nloc = mloc.add(dir);

			if (isLattice(nloc) && kingDistance(nloc, locHQ) >= terraformDist) return dir;
		}

		return null; /* should only happen if between wall and HQ */
	}

	public Direction findWallLattice(MapLocation mloc) {
		for (Direction dir: directions) {
			MapLocation nloc = mloc.add(dir);

			if (isLattice(nloc) && kingDistance(nloc, locHQ) > 0) return dir;
		}

		return null; /* should only happen if between wall and HQ */
	}

	public boolean[][] getOccupied() {
		boolean[][] occupied = new boolean[3][3];
		RobotInfo[] around = rc.senseNearbyRobots(locHQ, 2, rc.getTeam()); /* only robots directly adjacent */

		for (RobotInfo robot: around) {
			if (robot.type == RobotType.LANDSCAPER) {
				int curX = 1 + (robot.location.x - locHQ.x);
				int curY = 1 + (robot.location.y - locHQ.y);
				if (0 <= curX && curX <= 2 && 0 <= curY && curY <= 2) {
					occupied[curX][curY] = true;
				}
			}
		}

		/* THIS landscaper isn't included, so include it */
		MapLocation mloc = rc.getLocation();
		int curX = 1 + (mloc.x - locHQ.x);
		int curY = 1 + (mloc.y - locHQ.y);
		if (0 <= curX && curX <= 2 && 0 <= curY && curY <= 2) {
			occupied[curX][curY] = true;
		}

		return occupied;
	}

	/* clockwise gap between this and another landscaper on the wall */
	public int getClockwiseGap(boolean[][] occupied) {
		MapLocation mloc = rc.getLocation();

		int ind;
		for (ind = 0; ind < directions.length; ind++) {
			MapLocation loc = locHQ.add(directions[ind]);

			if (loc.equals(mloc)) break;
		}

		int orig = ind;

		while (true) {
			ind = (ind + 1) % 8;
			int curX = 1 + directions[ind].dx;
			int curY = 1 + directions[ind].dy;

			if (occupied[curX][curY]) break;
		}

		if (ind < orig) ind += 8;

		return ind - orig - 1;
	}

	public int getCounterclockwiseGap(boolean[][] occupied) {
		MapLocation mloc = rc.getLocation();

		int ind;
		for (ind = 0; ind < directions.length; ind++) {
			MapLocation loc = locHQ.add(directions[ind]);

			if (loc.equals(mloc)) break;
		}

		int orig = ind;

		while (true) {
			ind = (ind + 7) % 8;
			int curX = 1 + directions[ind].dx;
			int curY = 1 + directions[ind].dy;

			if (occupied[curX][curY]) break;
		}

		if (ind > orig) orig += 8;

		return orig - ind - 1;
	}

	public Direction getNextWallDirection(boolean clockwise) {
		MapLocation mloc = rc.getLocation();
		Direction moveDir = null, toHQ = mloc.directionTo(locHQ);
		
		if (!clockwise) {
			if (orthogonal(toHQ)) {
				moveDir = toHQ.rotateRight().rotateRight();
			} else {
				moveDir = toHQ.rotateRight();
			}
		} else {
			if (orthogonal(toHQ)) {
				moveDir = toHQ.rotateLeft().rotateLeft();
			} else {
				moveDir = toHQ.rotateLeft();
			}
		}

		return moveDir;
	}

	public boolean moveOnWall(boolean clockwise) throws GameActionException {
		Direction moveDir = getNextWallDirection(clockwise);

		if (moveDir == null) return false;
		return tryMove(moveDir);

		// MapLocation loc = null;
		// for (int i = 0; i < directions.length; i++) {
		// 	loc = locHQ.add(directions[i]);

		// 	if (loc.equals(mloc)) {
		// 		if (clockwise) loc = locHQ.add(directions[(i + 1) % 8]);
		// 		else loc = locHQ.add(directions[(i + 7) % 8]);
		// 		break;
		// 	}
		// }

		// if (loc == null) return false;
		// return tryMove(mloc.directionTo(loc));
	}

	/* this will see if this landscaper needs to move for adaptive wall, and make it move */
	public boolean correctWall() throws GameActionException {
		boolean[][] occupied = getOccupied();

		int count = 0;
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				if (occupied[i][j] && (i != 1 || j != 1)) ++count;
			}
		}

		/* 
		formula to later adapt (currently not used):
		move if gap > ceil((number of wall cells - number of landscapers) / number of landscapers)
		this will not work with 1 landscaper if we're on an edge
		*/

		/* clockwise */
		int gap = getClockwiseGap(occupied);

		if (count == 1) {
			if (moveOnWall(true)) return true;
		} else if (count == 2) {
			if (gap > 3) { 
				if (moveOnWall(true)) return true; 
			} else if (gap > 0 && taxicabDistance(rc.getLocation(), locHQ) > 1) {
				if (moveOnWall(true)) return true;
			}
		} else if (count == 3) {
			if (gap > 2) if (moveOnWall(true)) return true;
		} else {
			if (gap > 1) if (moveOnWall(true)) return true;
		}

		// /* counterclockwise */
		gap = getCounterclockwiseGap(occupied);

		if (count == 1) {
			if (moveOnWall(false)) return true;
		} else if (count == 2) {
			if (gap > 3) {
				if (moveOnWall(false)) return true;
			} else if (gap > 0 && taxicabDistance(rc.getLocation(), locHQ) > 1) {
				if (moveOnWall(false)) return true;
			}
		} else if (count == 3) {
			if (gap > 2) if (moveOnWall(false)) return true;
		} else {
			if (gap > 1) if (moveOnWall(false)) return true;
		}

		return false;
	}

	public boolean attackEnemyBuilding() throws GameActionException {
		if (rc.getDirtCarrying() < 1) return false;
		RobotInfo[] list = rc.senseNearbyRobots(2, (rc.getTeam() == Team.B) ? Team.A : Team.B);

		for (RobotInfo robot: list) {
			if (robot.type.isBuilding()) {
				Direction dir = rc.getLocation().directionTo(robot.location);

				if (rc.canDepositDirt(dir)) {
					rc.depositDirt(dir);
					return true;
				}
			}
		}

		return false;
	}

	public boolean executeMessage(Message message){
		/*Returns true if message applies to me*/
		if(super.executeMessage(message)){
			return true;
		}
		switch (message.type) {
			case WAIT:
				if (message.data[0] != rc.getID()) return false;
				status = LandscaperStatus.NOTHING;

				return true;
			case BUILD_WALL:
				MapLocation loc = new MapLocation(message.data[0], message.data[1]);
				if (loc.isAdjacentTo(rc.getLocation())) {
					status = LandscaperStatus.BUILDING;
					return true;
				}
				return false;
            case UNWAIT:
                if(message.data[0] != rc.getID()) return false;
                switch(message.data[1]) {
                    case 0:
                        if (status != LandscaperStatus.BUILDING) status = LandscaperStatus.DEFENDING;
                        return true;
					case 1:
						status = LandscaperStatus.TERRAFORMING;
						return true;
					case 3:
						status = LandscaperStatus.CORNER;
						return true;
                }
                return false;

		}
		return false;
	}

	public boolean canMove(Direction dir) throws GameActionException {
		return super.canMove(dir) && (status == LandscaperStatus.DEFENDING || status == LandscaperStatus.BUILDING || locHQ == null || !rc.getLocation().add(dir).isAdjacentTo(locHQ));
	}

}
