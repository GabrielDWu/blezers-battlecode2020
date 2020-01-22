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
	final static int terraformHeight = 10; /* how high should I make the land? */
	final static int terraformDist = 4; /* how far should I be from the hq before starting? */
	final static int terraformThreshold = 25; /* what height is too high/low to terraform? */

	public Landscaper(RobotController rc) throws GameActionException {
		super(rc);
	}

	public void startLife() throws GameActionException{
		super.startLife();
		status = LandscaperStatus.DEFENDING;
	}

	public void run() throws GameActionException {
		super.run();
		MapLocation mloc = rc.getLocation();
		Direction d = null;
		if(locHQ != null){
			d = rc.getLocation().directionTo(locHQ);
		}

		if (rc.getRoundNum() > 250) status = LandscaperStatus.TERRAFORMING;

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
				boolean[] filled = new boolean[8];
				int filledUpTo = -1;
				if (locDS != null) filled[(locHQ.directionTo(rc.getLocation()).ordinal()+filledOffset)%8] = true;
				RobotInfo[] r = rc.senseNearbyRobots(locHQ, 4, rc.getTeam());
				for (int i = 0; i < r.length; i++) {
					if(locDS == null && r[i].getType() == RobotType.DESIGN_SCHOOL) {
						locDS = r[i].getLocation();
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
					if (rc.getDirtCarrying() < 1) {
						if (rc.canDigDirt(d.opposite())) rc.digDirt(d.opposite());
						else if (rc.canDigDirt(d.opposite().rotateLeft())) rc.digDirt(d.opposite().rotateLeft());
						else if (rc.canDigDirt(d.opposite().rotateRight())) rc.digDirt(d.opposite().rotateRight());
					} else {
						if (rc.canDepositDirt(Direction.CENTER)) rc.depositDirt(Direction.CENTER);
					}
				}else {
					Direction moveDir = orthogonal(mloc.directionTo(locHQ)) ? mloc.directionTo(locHQ).rotateRight().rotateRight() : mloc.directionTo(locHQ).rotateRight();
					if (rc.canSenseLocation(mloc.add(moveDir))) {
						int diff = rc.senseElevation(mloc.add(moveDir)) - rc.senseElevation(mloc);
						if (diff > 3) {
							if (rc.canDigDirt(moveDir)) rc.digDirt(moveDir);
							else {
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
							else if (rc.canDepositDirt(moveDir)) rc.depositDirt(moveDir);
						}
						if (!mloc.add(moveDir).equals(locHQ.add(locHQ.directionTo(locDS)))) tryMove(moveDir);
					}
				}
				break;
			case BUILDING:
				if (rc.getDirtCarrying() < 1) {
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
					for (Direction dir : directions) {
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
				break;
			case TERRAFORMING:
				if (kingDistance(mloc, locHQ) < terraformDist || isLattice(mloc)) {
					/* if we're too close to HQ, move */
					/* also if we're in a lattice square, move */
					moveAwayFromHQ(mloc);
				} else {
					Direction nearLattice = findLattice(mloc);
					if (nearLattice != null) {
						tryTerraform(mloc, nearLattice);
					}
				}

				break;
		}
	}

	/* pick a random move taking me not closer to the HQ */
	public boolean moveAwayFromHQ(MapLocation mloc) throws GameActionException{
		int startIndex = r.nextInt(directions.length);
		int stopIndex = startIndex;
		int currentDist = kingDistance(mloc, locHQ);

		do {
			Direction dir = directions[startIndex];
			MapLocation nloc = mloc.add(dir);

			if (kingDistance(nloc, locHQ) >= currentDist && !isLattice(nloc)) {
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

		if (!tryTerraform(mloc, nearLattice)) {
			for (Direction dir: directions) {
				MapLocation nloc = mloc.add(dir);

				if (!isLattice(nloc)) {
					if (tryTerraform(mloc, dir, nearLattice)) return true;
				}
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

			if (isLattice(nloc) && kingDistance(nloc, locHQ) < terraformDist) return dir;
		}

		return null; /* should never happen */
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
			/* add case START_TERRAFORMING */
		}
		return false;
	}

}
