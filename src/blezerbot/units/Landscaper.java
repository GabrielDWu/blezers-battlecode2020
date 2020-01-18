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
			int shell_dist = Math.max(Math.abs((rc.getLocation().x-locHQ.x)), Math.abs((rc.getLocation()).y-locHQ.y));
			if (shell_dist == 2) {
				Direction dir = rc.getLocation().directionTo(locHQ);
				if (rc.getDirtCarrying() < 1) {
					if (rc.canDigDirt(dir.opposite())) rc.digDirt(dir.opposite());
					else if (rc.canDigDirt(dir.opposite().rotateLeft())) rc.digDirt(dir.opposite().rotateLeft());
                    else if (rc.canDigDirt(dir.opposite().rotateRight())) rc.digDirt(dir.opposite().rotateRight());
				} else {
					if (rc.canDepositDirt(Direction.CENTER)) {
						rc.depositDirt(Direction.CENTER);
					}
				}
			} else if(shell_dist > 2){
				goTo(locHQ);
			} else{
				//Just make random moves to try to break out
				int ri = r.nextInt(8);
				for(int i=0; i<8; i++) {
					tryMove(directions[ri+i]);
				}
			}
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
		}
		return false;
	}

}
