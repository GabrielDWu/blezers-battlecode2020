package blezerbot.units;

import battlecode.common.*;
import blezerbot.*;

public abstract class Unit extends Robot {
	
	MapLocation dest;
	boolean hugging = false;
	int lastDist = -1;

	public Unit(RobotController rc) throws GameActionException {
		super(rc);
	};

	public int distHQ() {

		if(locHQ == null) return Integer.MAX_VALUE;
		return rc.getLocation().distanceSquaredTo(locHQ);

	}

	public void goTo(MapLocation loc) throws GameActionException {
		if(!rc.isReady()) return;
		if (loc != dest) {
			dest = loc;
			lastDist = 0;
			hugging = false;
		}
		MapLocation mloc = rc.getLocation();

		//Check if still should be hugging (if nothing around you, hugging=false)
		if(hugging){
			hugging = false;
			for(Direction dir : directions){
				if(!canMove(dir)){
					hugging = true;
					break;
				}
			}
		}
		if (!hugging) {
			Direction dir = mloc.directionTo(loc);
			if (tryMove(dir)) return;

			//Turn right until you see an empty space
			facing = (orthogonal(dir) ? dir : dir.rotateRight());
			int cnt = 0;
			while(!canMove(facing)){
				facing = nextDir90(facing, true);
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
		Direction dir = nextDir90(facing, false);
		//Left turn
		if(tryMove(dir)){
			facing=dir;
			return;
		}
		dir = dir.rotateRight();

		//Left forward diagonal turn
		if(tryMove(dir)){
			facing = dir.rotateLeft();

			return;
		}
		dir = dir.rotateRight();

		//Forward
		if(tryMove(dir))return;
		dir = dir.rotateRight();

		//Right forward diagonal turn
		if(tryMove(dir))return;
		dir = dir.rotateRight();

		//Right turn
		if(tryMove(dir)){
			facing = dir;
			return;
		}
		dir = dir.rotateRight();

		//Right back diagonal turn
		if(tryMove(dir)){
			facing = dir.rotateLeft();
			return;
		}
	}

}