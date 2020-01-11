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

	boolean hugging = false;
	//boolean clockwise = false;
	int lastDist = -1;

	public Miner(RobotController rc) throws GameActionException {

		super(rc);

	}

	public void run() throws GameActionException {
		if (!(searching || mining || returning || depositing)) searching = true;
		setVisitedAndSeen();
		MapLocation nloc = null;
		MapLocation mloc = rc.getLocation();
		if (searching) {
			findSoup();
			for (Direction dir : directions) {
				nloc = mloc.add(dir);
				if (rc.canSenseLocation(nloc) && rc.senseSoup(nloc) > 0) {
					mining = true;
					searching = false;
				}
			}
		} else if (mining) {
			boolean mined = false;
			for (Direction dir : directions) {
				if (tryMine(dir)) mined = true;
			}
			if (!mined) {
				mining = false;
				searching = true;
			}
			if (rc.getSoupCarrying() >= 100) {
				mining = false;
				searching = false;
				returning = true;
			}
		} else if (returning) {
			goTo(locHQ);
			if (nearHQ()) {
				returning = false;
				depositing = true;
			}
		} else if (depositing) {
			for (Direction dir : directions) {
				if (rc.canDepositSoup(dir)) rc.depositSoup(dir, rc.getSoupCarrying());
			}
		}
	}

	public boolean nearHQ() {
		RobotInfo[] near = rc.senseNearbyRobots(rc.getCurrentSensorRadiusSquared(), rc.getTeam());
		for(RobotInfo x: near){
			if(x.getType() == RobotType.HQ && rc.getLocation().distanceSquaredTo(x.getLocation())<2){
				System.out.println("nearhq");
				return true;
			}
		}
		return false;
	}


	public void goTo(MapLocation loc) throws GameActionException {
		if(!rc.isReady()) return;
		MapLocation mloc = rc.getLocation();

		//Check if still should be hugging (if nothing around you, hugging=false)
		if(hugging){
			System.out.println("Hugging");
			hugging = false;
			for(Direction dir: directions){
				if(!rc.canMove(dir)){
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
			while(!rc.canMove(dir)){
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
	    int numDir = newSeenList.size();
	    Direction maxl = null;
	    while (maxl == null || tryMove(maxl)) {
	        int max = -2;

	        //I'm pretty sure this is random enough and preserves teh bytecode
			int ri = r.nextInt(numDir);
	        for(int i=0; i<numDir; i++) {
	            int newv = newSeenList.get((i+ri)%numDir);
	            Direction newl = newSeenDirs.get((i+ri)%numDir);
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

	boolean tryMine(Direction dir) throws GameActionException {
	    if (rc.isReady() && rc.canMineSoup(dir)) {
	        rc.mineSoup(dir);
	        return true;
	    } else return false;
	}

}
