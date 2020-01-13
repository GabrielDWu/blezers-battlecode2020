package blezerbot.units;

import battlecode.common.*;
import java.util.*;
import blezerbot.*;

public class Landscaper extends Unit {

	enum LandscaperStatus {
		ATTACKING,
		DEFENDING,
		NOTHING
	}
	LandscaperStatus status = null;

	public Landscaper(RobotController rc) throws GameActionException {
		super(rc);
	}

	public void startLife() throws GameActionException{
		super.startLife();
		status = LandscaperStatus.DEFENDING;
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
		}else if(status==LandscaperStatus.DEFENDING) {
			if (locHQ == null) {
				return;
			}
			if (rc.getLocation().isAdjacentTo(locHQ)) {
				Direction dir = rc.getLocation().directionTo(locHQ);
				if (rc.getDirtCarrying() < 1) {
					if (rc.canDigDirt(dir)) {
						rc.digDirt(dir);    //Heal the HQ
					} else {
						if (rc.canDigDirt(dir.opposite())) rc.digDirt(dir.opposite());
					}
				} else {
					if (rc.canDepositDirt(Direction.CENTER)) {
						rc.depositDirt(Direction.CENTER);
					}
				}
			} else {
				goTo(locHQ);
			}
		}
	}

	public boolean executeMessage(int id, int[] m, int ptr){
		/*Returns true if message applies to me*/
		if(super.executeMessage(id, m, ptr)){
			return true;
		}
		if(id == 5){
			if (getInt(m, ptr, 15) != rc.getID()) return false;
			status = LandscaperStatus.NOTHING;
			return true;
		}
		return false;
	}

}
