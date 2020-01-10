package blezerbot;

import battlecode.common.*;
import java.util.*;
import java.lang.Math;

public class Miner extends Unit {

	boolean soupSearching = false;
	boolean returning = false;

	public Miner(RobotController rc) throws GameActionException {
		super(rc);
	}

	public void run() throws GameActionException {
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
		boolean mined = false;
		for (Direction dir : directions)
		        if (tryMine(dir)) {
		            mined = true;
		            returning = true;
		            soupSearching = false;
		        }
		if (!mined && !returning) {
		    findSoup();
		}
	}

	void findSoup() throws GameActionException {
	    if (!soupSearching) {
	        soupSearching = true;
	    }
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
	    if (dir.dy == 0) {
	        MapLocation nloc;
	        nx = x+6*dir.dx;
	        if (nx >= 0 && nx < w) {
	            within = true;
	            for (int d1 = -3; d1 <= 3; d1++) {
	                ny = y+d1;
	                if (ny >= 0 && ny < h && !seen[nx][ny]) visible++;
	            }
	        }
	        if (!within) {
	            nx = x+5*dir.dx;
	        }
	        if (within || nx >= 0 && nx < w) {
	            ny = y+4;
	            if (ny >= 0 && ny < h && !seen[nx][ny]) visible++;
	            ny = y-4;
	            if (ny >= 0 && ny < h && !seen[nx][ny]) visible++;
	        }
	        if (!within) {
	            nx = x+4*dir.dx;
	        }
	        if (within || nx >= 0 && nx < w) {
	            ny = y+5;
	            if (ny >= 0 && ny < h && !seen[nx][ny]) visible++;
	            ny = y-5;
	            if (ny >= 0 && ny < h && !seen[nx][ny]) visible++;
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
	        if (!within) ny = y+5*dir.dy;
	        if (within || ny >= 0 && ny < h) {
	            nx = x+4;
	            if (nx >= 0 && nx < w && !seen[nx][ny]) visible++;
	            nx = x-4;
	            if (nx >= 0 && nx < w && !seen[nx][ny]) visible++;
	        }
	        if (!within) ny = y+4*dir.dy;
	        if (within || ny >= 0 && ny < h) {
	            nx = x+5;
	            if (nx >= 0 && nx < w && !seen[nx][ny]) visible++;
	            nx = x-5;
	            if (nx >= 0 && nx < w && !seen[nx][ny]) visible++;
	        }
	    } else {
	        MapLocation nloc;
	        for (int i = 0; i < aNewVisibleMinerDiag.length; i++) {
	            int[] t = aNewVisibleMinerDiag[i];
	            nx = x+t[0]*dir.dx;
	            ny = y+t[1]*dir.dy;
	            if (nx >= 0 && nx < w && ny >= 0 && ny < h && !seen[nx][ny]) visible++;
	        }
	    }
	    return visible;
	}

	/**
	 * Attempts to mine soup in a given direction.
	 *
	 * @param dir The intended direction of mining
	 * @return true if a move was performed
	 * @throws GameActionException
	 */
	boolean tryMine(Direction dir) throws GameActionException {
	    if (rc.isReady() && rc.canMineSoup(dir)) {
	        rc.mineSoup(dir);
	        return true;
	    } else return false;
	}

}
