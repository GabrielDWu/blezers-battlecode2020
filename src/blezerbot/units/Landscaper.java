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
				if (locDS == null) {
					RobotInfo[] r = rc.senseNearbyRobots(locHQ, 4, rc.getTeam());
					for (int i = 0; i < r.length; i++) {
						if(r[i].getType() == RobotType.DESIGN_SCHOOL) {
							locDS = r[i].getLocation();
							locOpposite = locHQ.add(locHQ.directionTo(locDS).opposite());
							break;
						}
					}
				}
				Direction moveDir = orthogonal(mloc.directionTo(locHQ)) ? mloc.directionTo(locHQ).rotateRight().rotateRight() : mloc.directionTo(locHQ).rotateRight();
				int diff = rc.senseElevation(mloc.add(moveDir)) - rc.senseElevation(mloc);
				System.out.println(diff);
				if (diff > 3) {
					rc.digDirt(moveDir);
				} else if (diff < -3) {
					if (rc.getDirtCarrying() < 1) {
						Direction d = rc.getLocation().directionTo(locHQ);
						if (rc.canDigDirt(d.opposite())) rc.digDirt(d.opposite());
						else if (rc.canDigDirt(d.opposite().rotateLeft())) rc.digDirt(d.opposite().rotateLeft());
	                    else if (rc.canDigDirt(d.opposite().rotateRight())) rc.digDirt(d.opposite().rotateRight());
					}
					rc.depositDirt(moveDir);
				}
				tryMove(moveDir);
				break;
			case BUILDING:
				Direction d = rc.getLocation().directionTo(locHQ);
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
					if (mdir != null) rc.depositDirt(mdir);
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
