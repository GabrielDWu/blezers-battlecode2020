package blezerbot.units;

import battlecode.common.*;
import java.util.*;
import blezerbot.*;

public class Landscaper extends Unit {

	enum LandscaperStatus {
		ATTACKING,
		DEFENDING
	}
	LandscaperStatus status = null;

	public Landscaper(RobotController rc) throws GameActionException {
		super(rc);
	}

	public void startLife() throws GameActionException{
		super.startLife();
		status = LandscaperStatus.ATTACKING;
	}

	public void run() throws GameActionException {
		super.run();
		if(status==LandscaperStatus.ATTACKING){
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
		}else if(status==LandscaperStatus.DEFENDING){
			if (locHQ != null && distHQ() > 5) {
				for (Direction dir : directions) {
					if (rc.canDigDirt(dir)) rc.digDirt(dir);
				}
				goTo(locHQ);
			} else {
				for (Direction dir : directions) {
					if (rc.canDepositDirt(dir)) rc.depositDirt(dir);
				}
			}
		}
	}

}
