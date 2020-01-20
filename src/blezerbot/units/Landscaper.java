package blezerbot.units;

import battlecode.common.*;
import java.util.*;
import blezerbot.*;

public class Landscaper extends Unit {

	enum LandscaperStatus {
		ATTACKING,
		DEFENDING,
		NOTHING,
		BUILDING
	}
	LandscaperStatus status = null;
	MapLocation locDS = null;
	MapLocation locOpposite;

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
				RobotInfo[] r = rc.senseNearbyRobots(locHQ, 4, rc.getTeam());
				for (int i = 0; i < r.length; i++) {
					if(locDS == null && r[i].getType() == RobotType.DESIGN_SCHOOL) {
						locDS = r[i].getLocation();
						locOpposite = locHQ.add(locHQ.directionTo(locDS).opposite());
					}else if(r[i].getType() == RobotType.LANDSCAPER){
						if(r[i].getLocation().isAdjacentTo(locHQ)){
							//filled[0] is Northeast, then proceeds on clockwise
							filled[(locHQ.directionTo(r[i].getLocation()).ordinal()+7)%8] = true;
							System.out.println("filled "+(locHQ.directionTo(r[i].getLocation()).ordinal()+7)%8);
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

				if((locHQ.directionTo(mloc).ordinal()+7)%8-1 <= filledUpTo){
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
							if (rc.canDepositDirt(moveDir)) rc.depositDirt(moveDir);
						}
						if (!mloc.add(moveDir).equals(locHQ.add(locHQ.directionTo(locDS)))) tryMove(moveDir);
					}
				}
				break;
			case BUILDING:
				if (rc.getDirtCarrying() < 1) {
					if (rc.canDigDirt(d.opposite())) rc.digDirt(d.opposite());
					else if (rc.canDigDirt(d.opposite().rotateLeft())) rc.digDirt(d.opposite().rotateLeft());
                    else if (rc.canDigDirt(d.opposite().rotateRight())) rc.digDirt(d.opposite().rotateRight());
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
		}
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
		}
		return false;
	}

}
