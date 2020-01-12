package blezerbot.units;

import battlecode.common.*;
import blezerbot.*;
import java.util.*;

public abstract class Unit extends Robot {
	
	MapLocation dest;
	boolean hugging = false;
	int lastDist = -1;
	long[][] unitVisited;
	int unitVisitedIndex;
	public boolean[][] seen;
	public int[][] visited;

	public Unit(RobotController rc) throws GameActionException {
		super(rc);
	};
	public void run() throws GameActionException {
		super.run();
		if (sentInfo) {
			if (unitVisited == null) {
				unitVisited = new long[rc.getMapWidth()][rc.getMapHeight()];
				unitVisitedIndex = -1;
			}
			if (seen == null) seen = new boolean[rc.getMapWidth()][rc.getMapHeight()];
			if (visited == null) visited = new int[rc.getMapWidth()][rc.getMapHeight()];
		}
	}
	public ArrayList<MapLocation> getLocationsInRadius(int radiusSquared){
		ArrayList<MapLocation> ret = new ArrayList<MapLocation>();
		for(int dx = 0; dx*dx<=radiusSquared; dx++){
			for(int dy = 0; dx*dx + dy*dy<=radiusSquared; dy++){
				if (dx == 0 && dy == 0) {
					ret.add(new MapLocation(dx, dy));
				}
				else if(dx == 0){
					ret.add(new MapLocation(dx, dy));
					ret.add(new MapLocation(dx, -dy));
				}
				else if(dy == 0){
					ret.add(new MapLocation(-dx, dy));
					ret.add(new MapLocation(dx, dy));
				}
				else{
					for(int s1 = 0; s1<2; s1++){
						for(int s2 = 0; s2<2; s2++){
							ret.add(new MapLocation(dx*(2*s1-1), dy*(2*s2-1)));
						}
					}
				}
			}
		}
		return ret;
	}
	public int distHQ() {

		if(locHQ == null) return Integer.MAX_VALUE;
		return rc.getLocation().distanceSquaredTo(locHQ);

	}

	public int getUnitVisited(MapLocation loc) {
		return (int)((unitVisited[loc.x][loc.y]>>(unitVisitedIndex*4))&0xf);
	}

	public void incUnitVisited(MapLocation loc) {
		unitVisited[loc.x][loc.y] = (Math.min(((unitVisited[loc.x][loc.y]>>(unitVisitedIndex*4))&0xf)+1,0xf)<<(unitVisitedIndex*4))|((unitVisited[loc.x][loc.y]&(~(long)(0xf<<(unitVisitedIndex*4))))>>unitVisitedIndex*4);
	}
	public boolean netGunRadius(MapLocation loc) throws GameActionException{
		if(rc.canSenseLocation(loc) == false) return false;
		RobotInfo [] robots = rc.senseNearbyRobots();
		for(RobotInfo r: robots){
			if(r.getType() == RobotType.NET_GUN){
				if(loc.distanceSquaredTo(r.getLocation()) <= GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED){
					return true;
				}
			}
		}
		return false;
	}
	public void goTo(MapLocation loc) throws GameActionException {
		if(!rc.isReady()) return;
		if (unitVisited == null) return;
		if (loc != dest) {
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
			Direction sdir = directions[r.nextInt(directions.length)];
			int dcount = 0;
			while (dcount++ < directions.length && !tryMove(sdir)) {
				sdir = sdir.rotateRight();
			}
			return;
		}

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
		if (canMove(dir)) {
			Direction fdir = dir.rotateRight();
			if (canMove(fdir)) {
				//forward if it gets us closer
				int fdist = mloc.add(fdir).distanceSquaredTo(loc);	
				if (fdist < lastDist && fdist < mloc.add(dir).distanceSquaredTo(loc) && tryMove(fdir)) return;
			}
			if(tryMove(dir)){
				facing = dir.rotateLeft();
				return;
			}
		}
		dir = dir.rotateRight();

		//Forward
		if(tryMove(dir))return;
		dir = dir.rotateRight();

		//Right forward diagonal turn
		if (canMove(dir)) {
			Direction fdir = dir.rotateRight();
			if (canMove(fdir)) {
				//right if it gets us closer
				int fdist = mloc.add(fdir).distanceSquaredTo(loc);	
				if (fdist < lastDist && fdist < mloc.add(dir).distanceSquaredTo(loc) && tryMove(fdir)) {
					facing = fdir;
					return;
				};
			}
			if(tryMove(dir)){
				return;
			}
		}
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