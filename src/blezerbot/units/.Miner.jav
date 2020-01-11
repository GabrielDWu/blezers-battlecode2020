package blezerbot;

import battlecode.common.*;
import java.util.*;
import java.lang.*;

public class Miner extends Unit {
	int pathState = 0;
	boolean searching = true;
	boolean mining = false;
	boolean returning = false;
	boolean depositing = false;
	MapLocation soupLoc = null;

	boolean hugging = false;
	boolean clockwise = false;
	int lastDist = -1;

	public Miner(RobotController rc) throws GameActionException {

		super(rc);

	}

	public void run() throws GameActionException {
		System.out.println("START "+Clock.getBytecodesLeft());
		if (!(searching || mining || returning || depositing)) searching = true;
		setVisitedAndSeen();
		MapLocation nloc = null;
		MapLocation mloc = rc.getLocation();
		if (searching) {
			System.out.println("SEARCHING");
			for (int x = -5; x <= 5; x++) {
				for (int y = -5; y <= 5; y++) {
					nloc = mloc.translate(x, y);
					if (rc.canSenseLocation(nloc) && rc.senseSoup(nloc) > 0) {
						mining = true;
						searching = false;
						soupLoc = nloc;
						break;
					}
				}
				if (searching == false) break;
			}
			if (searching) findSoup();
			else {
				int[] col;
				boolean[] col2;
				for (int x = 0; x < visited.length; x++) {
					col = visited[x];
					col2 = seen[x];
					for (int y = 0; y < col.length; y++) {
						col[y] = 0;
						col2[y] = false;
					}
				}
			}
		} else if (mining) {
			if (!mloc.isAdjacentTo(soupLoc)) goTo(soupLoc);
			else {
				hugging = false;
				boolean mined = tryMine(mloc.directionTo(soupLoc));
				if (!mined) {
					mining = false;
					searching = true;
					soupLoc = null;
				}
				if (rc.getSoupCarrying() >= 100) {
					mining = false;
					searching = false;
					returning = true;
				}
				if (soupLoc != null && rc.canSenseLocation(soupLoc) && rc.senseSoup(soupLoc) == 0) {
					soupLoc = null;
					mining = false;
					if (!returning) searching = true;
				}
			}
		} else if (returning) {
			goTo(locHQ);
			if (nearHQ()) {
				hugging = false;
				returning = false;
				depositing = true;
			}
		} else if (depositing) {
			if (rc.canDepositSoup(mloc.directionTo(locHQ))) rc.depositSoup(mloc.directionTo(locHQ), rc.getSoupCarrying());
			if (rc.getSoupCarrying() == 0) {
				depositing = false;
				searching = true;
			}
		}
		System.out.println("END "+Clock.getBytecodesLeft());
	}

	public boolean nearHQ() {
		RobotInfo[] near = rc.senseNearbyRobots(rc.getCurrentSensorRadiusSquared(), rc.getTeam());
		for(RobotInfo x: near){
			if(x.getType() == RobotType.HQ && rc.getLocation().distanceSquaredTo(x.getLocation())<=2){
				return true;
			}
		}
		return false;
	}

	public Direction nextDir(Direction dir) {
		switch (dir) {
			case NORTH: return Direction.NORTHEAST;
			case NORTHEAST: return Direction.EAST;
			case EAST: return Direction.SOUTHEAST;
			case SOUTHEAST: return Direction.SOUTH;
			case SOUTH: return Direction.SOUTHWEST;
			case SOUTHWEST: return Direction.WEST;
			case WEST: return Direction.NORTHWEST;
			case NORTHWEST: return Direction.NORTH;
		}
		return null;
	}

	public void goTo(MapLocation loc) throws GameActionException {
		MapLocation mloc = rc.getLocation();
		Direction dir = mloc.directionTo(loc);
		if (!hugging) {
			if (tryMove(dir)) return;
			lastDist = mloc.distanceSquaredTo(loc);
			hugging = true;
			dir = nextDir(dir);
		}
		if (mloc.distanceSquaredTo(loc) < lastDist) {
			hugging = false;
			goTo(loc);
			return;
		}
		while (!tryMove(dir)) {
			dir = nextDir(dir);
		}
	}

	public void setVisitedAndSeen() throws GameActionException {
		MapLocation myloc = rc.getLocation();
		visited[myloc.x][myloc.y]++;
		int w = rc.getMapWidth();
		int h = rc.getMapHeight();
		int x = myloc.x;
		int y = myloc.y;
		int nx;
		int ny;
		for (int i = -5; i <= 5; i++) {
		    nx = x+i;
		    if (nx < 0 || nx >= w) continue;
		    boolean[] s = seen[nx];
		    for (int j = -3; j <= 3; j++) {
		        ny = y+j;
		        if (ny >= 0 && ny < h) s[ny] = true;
		    }
		    if (i >= -4 && i <= 4) {
		        ny = y+4;
		        if (ny >= 0 && ny < h) s[ny] = true;
		        ny = y-4;
		        if (ny >= 0 && ny < h) s[ny] = true;
		        if (i >= -3 && i <= 3) {
		            ny = y+5;
		            if (ny >= 0 && ny < h) s[ny] = true;
		            ny = y-5;
		            if (ny >= 0 && ny < h) s[ny] = true;
		        }
		    }
		}
	}

	void findSoup() throws GameActionException {
	    ArrayList<Integer> newSeenList = new ArrayList<Integer>();
	    ArrayList<Direction> newSeenDirs = new ArrayList<Direction>();
	    MapLocation l = rc.getLocation();
	    MapLocation ln;
	    for (Direction dir : directions) {
	        ln = l.add(dir);
	        if (onMap(ln)) {
	            newSeenList.add(visited[ln.x][ln.y] > 0 ? -visited[ln.x][ln.y] : newVisibleMiner(l, dir));
	            newSeenDirs.add(dir);
	        }
	    }
	    System.out.println(newSeenDirs);
	    System.out.println(newSeenList);
	    Direction maxl = null;
	    while (maxl == null || !tryMove(maxl)) {
	        ArrayList<Integer> newNewSeenList = (ArrayList<Integer>)newSeenList.clone();
	        ArrayList<Direction> newNewSeenDirs = (ArrayList<Direction>)newSeenDirs.clone();
	        int max = -2;
	        while (newNewSeenList.size() > 0) {
	            int ri = r.nextInt(newNewSeenList.size());
	            int newv = newNewSeenList.remove(ri);
	            Direction newl = newNewSeenDirs.remove(ri);
	            if (newv > max && rc.canMove(newl) && !rc.senseFlooding(rc.adjacentLocation(newl))) {
	                maxl = newl;
	                max = newv;
	            }
	        }
	    }
	}

	static int[][] aNewVisibleMiner = new int[][]{{6,0},{6,1},{6,-1},{6,2},{6,-2},{6,3},{6,-3},{5,4},{5,-4},{4,5},{4,-5}};
	static int[][] aNewVisibleMinerDiag = new int[][]{{6,-2},{6,-1},{6,0},{6,1},{6,2},{6,3},{6,4},{5,4},{5,5},{4,5},{4,6},{3,6},{2,6},{1,6},{0,6},{-1,6},{-2,6}};
	int newVisibleMiner(MapLocation loc, Direction dir) throws GameActionException {
	    int x = loc.x;
	    int y = loc.y;
	    int nx;
	    int ny;
	    int visible = 0;
	    int w = rc.getMapWidth();
	    int h = rc.getMapHeight();
	    boolean within = false;
	    boolean[] snx;
	    if (dir.dy == 0) {
	        MapLocation nloc;
	        nx = x+6*dir.dx;
	        if (nx >= 0 && nx < w) {
	        	snx = seen[nx];
	            within = true;
	            for (int d1 = -3; d1 <= 3; d1++) {
	                ny = y+d1;
	                if (ny >= 0 && ny < h && !snx[ny]) visible++;
	            }
	        }
            nx = x+5*dir.dx;
	        if (within || nx >= 0 && nx < w) {
	        	snx = seen[nx];
	            ny = y+4;
	            if (ny >= 0 && ny < h && !snx[ny]) visible++;
	            ny = y-4;
	            if (ny >= 0 && ny < h && !snx[ny]) visible++;
	        }
            nx = x+4*dir.dx;
	        if (within || nx >= 0 && nx < w) {
	        	snx = seen[nx];
	            ny = y+5;
	            if (ny >= 0 && ny < h && !snx[ny]) visible++;
	            ny = y-5;
	            if (ny >= 0 && ny < h && !snx[ny]) visible++;
	        }
	    } else if (dir.dx == 0) {
	        MapLocation nloc;
	        ny = y+6*dir.dy;
	        if (ny >= 0 && ny < h) {
	            within = true;
	            for (int d1 = -3; d1 <= 3; d1++) {
	                nx = x+d1;
	                if (nx >= 0 && nx < w && !seen[nx][ny]) visible++;
	            }
	        }
	        ny = y+5*dir.dy;
	        if (within || ny >= 0 && ny < h) {
	            nx = x+4;
	            if (nx >= 0 && nx < w && !seen[nx][ny]) visible++;
	            nx = x-4;
	            if (nx >= 0 && nx < w && !seen[nx][ny]) visible++;
	        }
	        ny = y+4*dir.dy;
	        if (within || ny >= 0 && ny < h) {
	            nx = x+5;
	            if (nx >= 0 && nx < w && !seen[nx][ny]) visible++;
	            nx = x-5;
	            if (nx >= 0 && nx < w && !seen[nx][ny]) visible++;
	        }
	    } else {
	        MapLocation nloc;
	        nx = x+6*dir.dx;
	        if (nx >= 0 && nx < w) {
	        	snx = seen[nx];
	            within = true;
	            for (int d1 = -2; d1 <= 4; d1++) {
	                ny = y+d1*dir.dy;
	                if (ny >= 0 && ny < h && !snx[ny]) visible++;
	            }
	        }
	        nx = x+5*dir.dx;
	        if (within || nx >= 0 && nx < w) {
	        	snx = seen[nx];
	            ny = y+4*dir.dy;
	            if (ny >= 0 && ny < h && !snx[ny]) visible++;
	            ny = y+5*dir.dy;
	            if (ny >= 0 && ny < h && !snx[ny]) visible++;
	        }
	        nx = x+4*dir.dx;
	        ny = y+5*dir.dy;
	        if (ny >= 0 && ny < h && !seen[nx][ny]) visible++;
	        ny = y+6*dir.dy;
	        if (ny >= 0 && ny < h) {
	            within = true;
	            for (int d1 = -2; d1 <= 4; d1++) {
	                nx = x+d1*dir.dx;
	                if (nx >= 0 && nx < w && !seen[nx][ny]) visible++;
	            }
	        }
	        /*for (int i = 0; i < aNewVisibleMinerDiag.length; i++) {
	            int[] t = aNewVisibleMinerDiag[i];
	            nx = x+t[0]*dir.dx;
	            ny = y+t[1]*dir.dy;
	            if (nx >= 0 && nx < w && ny >= 0 && ny < h && !seen[nx][ny]) visible++;
	        }*/
	    }
	    return visible;
	}

	boolean tryMine(Direction dir) throws GameActionException {
	    if (rc.isReady() && rc.canMineSoup(dir)) {
	        rc.mineSoup(dir);
	        return true;
	    } else return false;
	}

}
