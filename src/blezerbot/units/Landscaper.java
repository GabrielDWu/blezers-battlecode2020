package blezerbot.units;

import battlecode.common.*;
import java.util.*;
import blezerbot.*;

public class Landscaper extends Unit {

	enum LandscaperStatus {
		ATTACKING,
		DEFENDING,
		NOTHING,
		BUILDING,
        TERRAFORMING
	}
	LandscaperStatus status = null;
	MapLocation locDS = null;
	int filledOffset;
	int idDS;
	final static int terraformHeight = 10; /* how high should I make the land? */
	final static int terraformDist = 4; /* how far should I be from the hq before starting? */
	final static int terraformThreshold = 25; /* what height is too high/low to terraform? */
	final static int terraformTries = 20; /* how many random moves away from hq to try? */

	public Landscaper(RobotController rc) throws GameActionException {
		super(rc);
	}

	public void startLife() throws GameActionException{
		super.startLife();
		status = LandscaperStatus.NOTHING;
		idDS = -1;
	}

	public void run() throws GameActionException {
		super.run();
		MapLocation mloc = rc.getLocation();
		Direction d = null;
		if(locHQ != null){
			d = rc.getLocation().directionTo(locHQ);
		}

		switch (status) {
			case ATTACKING:
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
				if (idDS != -1 && !rc.canSenseRobot(idDS)) {
					status = LandscaperStatus.BUILDING;
					break;
				}
				boolean[] filled = new boolean[8];
				int filledUpTo = -1;
				if (locDS != null) filled[(locHQ.directionTo(rc.getLocation()).ordinal()+filledOffset)%8] = true;
				RobotInfo[] r = rc.senseNearbyRobots(locHQ, 4, rc.getTeam());
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
				for(int i=0; i<8; i++){
					if(filled[i]){
						filledUpTo = i;
					}else{
						break;
					}
				}
				if(locDS != null && (locHQ.directionTo(mloc).ordinal()+filledOffset)%8 <= filledUpTo){
					if(rc.canDigDirt(d)) rc.digDirt(d);//heal hq
					if (rc.getDirtCarrying() < 1) {
						if (rc.canDigDirt(d.opposite())) rc.digDirt(d.opposite());
						else if (rc.canDigDirt(d.opposite().rotateLeft())) rc.digDirt(d.opposite().rotateLeft());
						else if (rc.canDigDirt(d.opposite().rotateRight())) rc.digDirt(d.opposite().rotateRight());
					} else {
						attackEnemyBuilding();
						if (rc.canDepositDirt(Direction.CENTER)) rc.depositDirt(Direction.CENTER);
					}
				}else {
					Direction moveDir = orthogonal(mloc.directionTo(locHQ)) ? mloc.directionTo(locHQ).rotateRight().rotateRight() : mloc.directionTo(locHQ).rotateRight();
					if (rc.canSenseLocation(mloc.add(moveDir))) {
						int diff = rc.senseElevation(mloc.add(moveDir)) - rc.senseElevation(mloc);
						if (diff > 3) {
							if (rc.canDigDirt(moveDir)) rc.digDirt(moveDir);
							else {
								attackEnemyBuilding();
								for (Direction dir : directionswcenter) {
									if (rc.canDepositDirt(dir) && !mloc.add(dir).equals(locHQ) && !mloc.add(dir).equals(locDS) && !mloc.add(dir).isAdjacentTo(locHQ)) {
										rc.depositDirt(dir);
									}
								}
							}
						} else if (diff < -3) {
							if (rc.getDirtCarrying() < 1) {
								if (rc.canDigDirt(d.opposite())) rc.digDirt(d.opposite());
								else if (rc.canDigDirt(d.opposite().rotateLeft())) rc.digDirt(d.opposite().rotateLeft());
								else if (rc.canDigDirt(d.opposite().rotateRight())) rc.digDirt(d.opposite().rotateRight());
							}
							else {
								attackEnemyBuilding();
								if (rc.canDepositDirt(moveDir)) rc.depositDirt(moveDir);
							}
						}
						if (!mloc.add(moveDir).equals(locHQ.add(locHQ.directionTo(locDS)))) tryMove(moveDir);
					}
				}
				break;
			case BUILDING:
				if (rc.getRoundNum() % 5 == 0) {
					if (!correctWall()) {
						reinforceWall(mloc, d);
					}
				} else {
					reinforceWall(mloc, d);
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
			for (Direction dir : new Direction[]{d.opposite(), d.opposite().rotateLeft(), d.opposite().rotateRight()}) {
				if (rc.canSenseLocation(mloc.add(dir))) {
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

	/* try to terraform a specific square */
	public boolean tryTerraform(MapLocation mloc, Direction dir, Direction nearLattice) throws GameActionException {
		MapLocation nloc = mloc.add(dir);
		int currentElevation = rc.senseElevation(mloc);
		int newElevation = rc.senseElevation(nloc);

		/* either too high, too low, or already good */
		if (Math.abs(newElevation - currentElevation) > terraformThreshold) return false;
		if (newElevation == terraformHeight) return false;
		

		if (newElevation > terraformHeight) { /* if our target square is higher, dig from it */
			if (rc.getDirtCarrying() >= RobotType.LANDSCAPER.dirtLimit) {
				if (attackEnemyBuilding()) return true;
				if (rc.canDepositDirt(nearLattice)) {
					rc.depositDirt(nearLattice);
					return true;
				}
			} else {
				if (rc.canDigDirt(nearLattice)) {
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
				if (rc.canDepositDirt(nearLattice)) {
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

		return null; /* should never happen */
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

	public boolean moveOnWall(boolean clockwise) throws GameActionException {
		MapLocation mloc = rc.getLocation();

		MapLocation loc = null;
		for (int i = 0; i < directions.length; i++) {
			loc = locHQ.add(directions[i]);

			if (loc.equals(mloc)) {
				if (clockwise) loc = locHQ.add(directions[(i + 1) % 8]);
				else loc = locHQ.add(directions[(i + 7) % 8]);
				break;
			}
		}

		if (loc == null) return false;
		return tryMove(mloc.directionTo(loc));
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
		formula to later adapt (?):
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
                }
                return false;

		}
		return false;
	}

}
