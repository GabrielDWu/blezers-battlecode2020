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
	boolean goingCW;
	public boolean[][] seen;
	public int[][] visited;
	public boolean[] safeFromFlood;
	public boolean[][] _notFlooded;
	final static int DRONE_RUN_RADIUS = 9;
	boolean reducedRunRadius;
	boolean noRunRadius;
	public final static int terraformDist = 1; /* how far should I be from the hq before starting? */
	public final static int terraformTries = 20; /* how many random moves away from hq to try? */
	public final static int terraformHeight = 15; /* how high should I make the land? */

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
			if (rc.getType() != RobotType.DELIVERY_DRONE) {
				// run away from drones
				RobotInfo[] info = rc.senseNearbyRobots((noRunRadius ? 0 : (reducedRunRadius ? 2 : DRONE_RUN_RADIUS)), rc.getTeam() == Team.A ? Team.B : Team.A);
				boolean moved = false;
				for (RobotInfo r : info) {
					if (r.getType() == RobotType.DELIVERY_DRONE) {
						for (Direction dir : directions) {
							int newDist = rc.getLocation().add(dir).distanceSquaredTo(r.getLocation());
							if (newDist > rc.getLocation().distanceSquaredTo(r.getLocation()) && newDist > 2) {
								if (tryMove(dir)) {
									moved = true;
									break;
								}
							}
						}
						if (moved) break;
					}
				}
				// which tiles will flood next turn?
				if (_notFlooded == null) _notFlooded = new boolean[5][5];
				MapLocation mloc = rc.getLocation();
				boolean[] ni = _notFlooded[0];
				MapLocation t = mloc.translate(-2, -2);
				ni[0] = !(rc.canSenseLocation(t) && rc.senseFlooding(t));
				t = mloc.translate(-2, -1);
				ni[1] = !(rc.canSenseLocation(t) && rc.senseFlooding(t));
				t = mloc.translate(-2, 0);
				ni[2] = !(rc.canSenseLocation(t) && rc.senseFlooding(t));
				t = mloc.translate(-2, 1);
				ni[3] = !(rc.canSenseLocation(t) && rc.senseFlooding(t));
				t = mloc.translate(-2, 2);
				ni[4] = !(rc.canSenseLocation(t) && rc.senseFlooding(t));
				ni = _notFlooded[1];
				t = mloc.translate(-1, -2);
				ni[0] = !(rc.canSenseLocation(t) && rc.senseFlooding(t));
				t = mloc.translate(-1, -1);
				ni[1] = !(rc.canSenseLocation(t) && rc.senseFlooding(t));
				t = mloc.translate(-1, 0);
				ni[2] = !(rc.canSenseLocation(t) && rc.senseFlooding(t));
				t = mloc.translate(-1, 1);
				ni[3] = !(rc.canSenseLocation(t) && rc.senseFlooding(t));
				t = mloc.translate(-1, 2);
				ni[4] = !(rc.canSenseLocation(t) && rc.senseFlooding(t));
				ni = _notFlooded[2];
				t = mloc.translate(0, -2);
				ni[0] = !(rc.canSenseLocation(t) && rc.senseFlooding(t));
				t = mloc.translate(0, -1);
				ni[1] = !(rc.canSenseLocation(t) && rc.senseFlooding(t));
				t = mloc.translate(0, 0);
				ni[2] = !(rc.canSenseLocation(t) && rc.senseFlooding(t));
				t = mloc.translate(0, 1);
				ni[3] = !(rc.canSenseLocation(t) && rc.senseFlooding(t));
				t = mloc.translate(0, 2);
				ni[4] = !(rc.canSenseLocation(t) && rc.senseFlooding(t));
				ni = _notFlooded[3];
				t = mloc.translate(1, -2);
				ni[0] = !(rc.canSenseLocation(t) && rc.senseFlooding(t));
				t = mloc.translate(1, -1);
				ni[1] = !(rc.canSenseLocation(t) && rc.senseFlooding(t));
				t = mloc.translate(1, 0);
				ni[2] = !(rc.canSenseLocation(t) && rc.senseFlooding(t));
				t = mloc.translate(1, 1);
				ni[3] = !(rc.canSenseLocation(t) && rc.senseFlooding(t));
				t = mloc.translate(1, 2);
				ni[4] = !(rc.canSenseLocation(t) && rc.senseFlooding(t));
				ni = _notFlooded[4];
				t = mloc.translate(2, -2);
				ni[0] = !(rc.canSenseLocation(t) && rc.senseFlooding(t));
				t = mloc.translate(2, -1);
				ni[1] = !(rc.canSenseLocation(t) && rc.senseFlooding(t));
				t = mloc.translate(2, 0);
				ni[2] = !(rc.canSenseLocation(t) && rc.senseFlooding(t));
				t = mloc.translate(2, 1);
				ni[3] = !(rc.canSenseLocation(t) && rc.senseFlooding(t));
				t = mloc.translate(2, 2);
				ni[4] = !(rc.canSenseLocation(t) && rc.senseFlooding(t));
				float w = GameConstants.getWaterLevel(rc.getRoundNum()+1);
				int x;
				int y;
				for (Direction dir : directionswcenter) {
					x = dir.dx + 2;
					y = dir.dy + 2;
					safeFromFlood[dir.ordinal()] = !rc.canSenseLocation(rc.adjacentLocation(dir)) || rc.senseElevation(rc.adjacentLocation(dir)) > w || (_notFlooded[x+1][y]&&_notFlooded[x+1][y+1]&&_notFlooded[x+1][y-1]&&_notFlooded[x-1][y]&&_notFlooded[x-1][y+1]&&_notFlooded[x-1][y-1]&&_notFlooded[x][y+1]&&_notFlooded[x][y-1]&&_notFlooded[x][y]);
				}
			}
		}
	}

	//optimistic surrounded detection for land troops
	public boolean surroundedLocation(MapLocation a) throws GameActionException {
		if(rc.getLocation().isAdjacentTo(a)) return false;
		if(rc.canSenseLocation(a) == false) return false;
		for(Direction dir: directions){
			MapLocation nloc = a.add(dir);
			if(rc.canSenseLocation(nloc)){
				if(!(rc.senseFlooding(nloc)  ||rc.senseRobotAtLocation(nloc) != null)) return false;
			}
		}
		return true;
	}
	public void startLife() throws GameActionException {
		super.startLife();
		returnGetLocationInRadius = new ArrayList<MapLocation>();
		safeFromFlood = new boolean[9];
	}
	ArrayList<MapLocation> returnGetLocationInRadius;
	void getLocationInRadiusHelper(MapLocation center, int dx, int dy){
		int x = dx + center.x;
		int y = dy  +center.y;
		if(onMap(x, y)) returnGetLocationInRadius.add(new MapLocation(x, y));
	}
	public ArrayList<MapLocation> getLocationsInRadius(MapLocation center, int radiusSquared){
		returnGetLocationInRadius.clear();
		for(int dx = 0; dx*dx<=radiusSquared; dx++) {
			for (int dy = 0; dx * dx + dy * dy <= radiusSquared; dy++) {
				if (dx == 0 && dy == 0) {
					getLocationInRadiusHelper(center, dx, dy);
				} else if (dx == 0) {
					getLocationInRadiusHelper(center, dx, -dy);
					getLocationInRadiusHelper(center, dx, -dy);
				} else if (dy == 0) {
					getLocationInRadiusHelper(center, -dx, dy);
					getLocationInRadiusHelper(center, dx, dy);

				} else {
					getLocationInRadiusHelper(center, dx, -dy);
					getLocationInRadiusHelper(center, dx, -dy);
					getLocationInRadiusHelper(center, -dx, dy);
					getLocationInRadiusHelper(center, dx, dy);
				}
			}
		}
		return returnGetLocationInRadius;
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
	public boolean isBuilding(RobotType r){
		if(r == RobotType.REFINERY || r == RobotType.DESIGN_SCHOOL || r == RobotType.VAPORATOR || r == RobotType.FULFILLMENT_CENTER || r == RobotType.HQ || r == RobotType.NET_GUN) return true;
		return false;
	}

	public void goTo(MapLocation loc) throws GameActionException {
		if(!rc.isReady() || rc.getLocation().equals(loc)) return;
		if (unitVisited == null) return;
		MapLocation mloc = rc.getLocation();
		if (!loc.equals(dest)) {
			dest = loc;
			lastDist = 0;
			hugging = false;
			unitVisitedIndex++;
			if (unitVisitedIndex > 15) {
				unitVisited = new long[rc.getMapWidth()][rc.getMapHeight()];
				unitVisitedIndex = 0;
			}
			if (mloc.add(mloc.directionTo(loc).rotateRight()).distanceSquaredTo(loc) <= mloc.add(mloc.directionTo(loc).rotateLeft()).distanceSquaredTo(loc)) {
				goingCW = true;
			} else goingCW = false;
		}
		incUnitVisited(mloc);
		if (getUnitVisited(mloc) > 4) {
			randomMove();
			return;
		}
		if (goingCW) goToCW(loc);
		else goToCCW(loc);
	}

	public void goToCW(MapLocation loc) throws GameActionException {
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
				facing = facing.rotateRight().rotateRight();
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
		Direction dir = facing.rotateLeft().rotateLeft();
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

	public void goToCCW(MapLocation loc) throws GameActionException {
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
			facing = (orthogonal(dir) ? dir : dir.rotateLeft());
			int cnt = 0;
			while(!canMove(facing)){
				facing = facing.rotateLeft().rotateLeft();
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
		Direction dir = facing.rotateRight().rotateRight();
		//Left turn
		if(tryMove(dir)){
			facing=dir;
			return;
		}
		dir = dir.rotateLeft();

		//Left forward diagonal turn
		if (canMove(dir)) {
			Direction fdir = dir.rotateLeft();
			if (canMove(fdir)) {
				//forward if it gets us closer
				int fdist = mloc.add(fdir).distanceSquaredTo(loc);	
				if (fdist < lastDist && fdist < mloc.add(dir).distanceSquaredTo(loc) && tryMove(fdir)) return;
			}
			if(tryMove(dir)){
				facing = dir.rotateRight();
				return;
			}
		}
		dir = dir.rotateLeft();

		//Forward
		if(tryMove(dir))return;
		dir = dir.rotateLeft();

		//Right forward diagonal turn
		if (canMove(dir)) {
			Direction fdir = dir.rotateLeft();
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
		dir = dir.rotateLeft();

		//Right turn
		if(tryMove(dir)){
			facing = dir;
			return;
		}
		dir = dir.rotateLeft();

		//Right back diagonal turn
		if(tryMove(dir)){
			facing = dir.rotateRight();
			return;
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

	public boolean canMove(Direction dir) throws GameActionException {
		return rc.canMove(dir) && !rc.senseFlooding(rc.adjacentLocation(dir)) && (!rc.isReady() || safeFromFlood[dir.ordinal()]);
	}

	public boolean tryMove(Direction dir) throws GameActionException {
	    if (rc.isReady() && canMove(dir)) {
	        rc.move(dir);
	        return true;
	    } else return false;
	}

	public void randomMove() throws GameActionException {
		int ri = r.nextInt(8);
		for(int i=0; i<8; i++) {
			if(tryMove(directions[(ri+i)%8])) return;
		}
	}

	public void randomOrthogonalMove() throws GameActionException {
		int ri = r.nextInt(4)*2;
		for(int i=0; i<8; i+=2) {
			if(tryMove(directions[(ri+i)%8])) return;
		}
	}

}