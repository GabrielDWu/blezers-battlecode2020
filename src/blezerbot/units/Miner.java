package blezerbot.units;

import battlecode.common.*;
import java.util.*;
import java.lang.*;
import blezerbot.*;

public class Miner extends Unit {

	enum MinerStatus {
		SEARCHING,
		MINING,
		RETURNING,
		DEPOSITING,
		BUILDING,
		NOTHING
	}

	MinerStatus status = null;
	MinerStatus prevStatus = null;

	MapLocation soupLoc = null;
	int[][] soupTries;

	RobotType buildingType = null;
	int buildingTries = 0;
	public ArrayList<MapLocation> locREFINERY;
	MapLocation chosenRefinery;
	MapLocation buildLocation = null;

	MapLocation locDS;
	MapLocation locOpposite;

	int buildableTiles;

	public Miner(RobotController rc) throws GameActionException {
		super(rc);
	}

	public void startLife() throws GameActionException{
		super.startLife();
		locREFINERY = new ArrayList<MapLocation>();
		buildableTiles = -1;
	}

	public void run() throws GameActionException {
		super.run();
		if (soupTries == null && sentInfo) soupTries = new int[rc.getMapWidth()][rc.getMapHeight()];
		if (sentInfo) {
			if (status == MinerStatus.NOTHING) {
				return;
			}
			if (status == null) status = MinerStatus.SEARCHING;
			setVisitedAndSeen();
			MapLocation nloc = null;
			MapLocation mloc = rc.getLocation();
			if (!(status == MinerStatus.DEPOSITING && chosenRefinery != null && chosenRefinery.equals(locHQ)) && prevStatus != MinerStatus.NOTHING && locHQ != null && mloc.isAdjacentTo(locHQ)) {
				goTo(mloc.add(mloc.directionTo(locHQ).opposite()));
			}
			int h = rc.getMapHeight();
			int w = rc.getMapWidth();
			switch (status) {
				case BUILDING:
					if(buildLocation == null){	// can build anywhere far from hq
						if (buildingTries++ > 3){
							status = prevStatus == null ? MinerStatus.MINING : prevStatus;
						}
						for (Direction dir : directions) {
							if (rc.getLocation().add(dir).distanceSquaredTo(locHQ) >= 9 &&
									rc.canBuildRobot(buildingType, dir)) {
								rc.buildRobot(buildingType, dir);
								status = prevStatus == null ? MinerStatus.MINING : prevStatus;
							}
						}
						if(status != (prevStatus == null ? MinerStatus.MINING : prevStatus)){	//Still trying to build... move away from HQ
							tryMove(rc.getLocation().directionTo(locHQ).opposite());
							tryMove(rc.getLocation().directionTo(locHQ).opposite().rotateLeft());
							tryMove(rc.getLocation().directionTo(locHQ).opposite().rotateRight());
						}
					}else{	//Specific place I want to build
						/*if (buildingTries++ > 50){
							status = MinerStatus.MINING;
						}*/
						// we will verify it can be done before giving the message, so infinite tries
						// (for e.g. waiting for accumulation of soup)
						if(rc.getLocation().equals(buildLocation)){
							//Move in a random direction
							int ri = r.nextInt(8);
							for(int i=0; i<8; i++) {
								tryMove(directions[(ri+i)%8]);
							}
						}else if(rc.getLocation().isAdjacentTo(buildLocation)){
							if(rc.canBuildRobot(buildingType, rc.getLocation().directionTo(buildLocation))){
								rc.buildRobot(buildingType, rc.getLocation().directionTo(buildLocation));
								status = prevStatus == null ? MinerStatus.MINING : prevStatus;
							}
						}else{
							goTo(buildLocation);
						}

					}
					break;
				case SEARCHING:
					for (int x = -5; x <= 5; x++) {
						if ((mloc.x+x) < 0 || (mloc.x+x) >= w) break;
						for (int y = -5; y <= 5; y++) {
							nloc = mloc.translate(x, y);
							if (!(rc.canSenseLocation(nloc) && rc.senseSoup(nloc) > 0)) continue;
							if (nloc.y >= 0 && nloc.y < h && soupTries[nloc.x][nloc.y] > 6) {
								continue;
							}
							status = MinerStatus.MINING;
							soupLoc = nloc;
							break;
						}
						if (status != MinerStatus.SEARCHING) break;
					}
					if (status == MinerStatus.SEARCHING) findSoup();
					break;
				case MINING:
					if (soupLoc != null && !mloc.isAdjacentTo(soupLoc)) {
						for (Direction dir : directions) {
							if (tryMine(dir)) { 
								soupLoc = mloc.add(dir);
								soupTries[soupLoc.x][soupLoc.y] = 0;
								break;
							}
						}
						if (soupTries[soupLoc.x][soupLoc.y] <= 6) {
							goTo(soupLoc);
							if(rc.getLocation().distanceSquaredTo(soupLoc) <= 35) soupTries[soupLoc.x][soupLoc.y]++;
						}
						else {
							soupLoc = null;
							status = MinerStatus.SEARCHING;
						}
					}
					else {
						boolean mined;
						if (soupLoc != null) mined = tryMine(mloc.directionTo(soupLoc));
						if (soupLoc == null || rc.senseSoup(soupLoc) == 0) {
							status = MinerStatus.SEARCHING;
							soupLoc = null;
						}
						if (rc.getSoupCarrying() >= 100) {
							status = MinerStatus.RETURNING;
						}
						else if (soupLoc != null && rc.canSenseLocation(soupLoc) && rc.senseSoup(soupLoc) == 0) {
							soupLoc = null;
							if (status != MinerStatus.RETURNING) status = MinerStatus.SEARCHING;
						}
					}
					break;
				case RETURNING:
					if(chosenRefinery == null){
						chosenRefinery = locHQ;
						if(locREFINERY.size() > 0){
							chosenRefinery = locREFINERY.get(0);
						}
						//later can add this
						/*
						int best = rc.getLocation().distanceSquaredTo(locHQ);
						for(MapLocation loc: locREFINERY){
							if(rc.getLocation().distanceSquaredTo(loc) < best){
								chosenRefinery = loc;
							}
						}*/
					}
					if (chosenRefinery.equals(locHQ) && locREFINERY.size() > 0) {
						chosenRefinery = locREFINERY.get(0);
					}
					goTo(chosenRefinery);
					if (rc.getLocation().isAdjacentTo(chosenRefinery)) {
						status = MinerStatus.DEPOSITING;
					}
					break;
				case DEPOSITING:
					if (rc.canDepositSoup(mloc.directionTo(chosenRefinery))) rc.depositSoup(mloc.directionTo(chosenRefinery), rc.getSoupCarrying());
					if (rc.getSoupCarrying() == 0) {
						chosenRefinery = null;
						if (soupLoc !=  null) {
							status = MinerStatus.MINING;
						} else status = MinerStatus.SEARCHING;
					}
					break;
			}
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
	    int[] newSeenList = new int[directions.length];
	    Direction[] newSeenDirs = new Direction[directions.length];
	    int numDir = 0;
	    MapLocation l = rc.getLocation();
	    MapLocation ln;
	    int di = 0;
	    for (Direction dir : directions) {
	        ln = l.add(dir);
	        if (onMap(ln)) {
	            newSeenList[di] = visited[ln.x][ln.y] > 0 ? -visited[ln.x][ln.y] : newVisibleMiner(l, dir);
	            newSeenDirs[di++] = dir;
	            numDir++;
	        }
	    }
	    Direction maxl = null;
	    while (maxl == null || !tryMove(maxl)) {
	        int max = Integer.MIN_VALUE;

	        //I'm pretty sure this is random enough and preserves the bytecode
			int ri = r.nextInt(numDir);
	        for(int i=0; i<numDir; i++) {
	            int newv = newSeenList[(i+ri)%numDir];
	            Direction newl = newSeenDirs[(i+ri)%numDir];
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
	        if ((within || nx >= 0 && nx < w) && ny >= 0 && ny < h && !seen[nx][ny]) visible++;
	        ny = y+6*dir.dy;
	        if (ny >= 0 && ny < h) {
	            within = true;
	            for (int d1 = -2; d1 <= 4; d1++) {
	                nx = x+d1*dir.dx;
	                if (nx >= 0 && nx < w && !seen[nx][ny]) visible++;
	            }
	        }
	    }
	    return visible;
	}

	public boolean executeMessage(Message message){
		/*Returns true if message applies to me*/
		if(super.executeMessage(message)){
			return true;
		}
		switch (message.type) {
			case BIRTH_INFO:
				//Miners want to store refinery locations
				RobotType unit_type = robot_types[message.data[0]];
				if(unit_type != RobotType.REFINERY){
					return true;
				}
				locREFINERY.add(new MapLocation(message.data[2], message.data[3]));
				return true;
			case BUILD_SPECUNIT_ANYLOC:
				if (message.data[1] != rc.getID()) return false;
				buildingType = robot_types[message.data[0]];
				buildingTries = 0;
				prevStatus = status == MinerStatus.BUILDING ? prevStatus : status;
				status = MinerStatus.BUILDING;
				buildLocation = null;
				return true;
			case BUILD_SPECUNIT_LOC:
				if (message.data[1] != rc.getID()) return false;
				buildingType = robot_types[message.data[0]];
				buildingTries = 0;
				prevStatus = status == MinerStatus.BUILDING ? prevStatus : status;
				status = MinerStatus.BUILDING;
				buildLocation = new MapLocation(message.data[2], message.data[3]);
				return true;
			case WAIT:
				if (message.data[0] != rc.getID()) return false;
				prevStatus = status;
				status = MinerStatus.NOTHING;
				return true;
			case UNWAIT:
				if (message.data[0] != rc.getID()) return false;
				prevStatus = null;
				status = MinerStatus.MINING;
				return true;
		}
		return false;
	}

	boolean tryMine(Direction dir) throws GameActionException {
	    if (rc.isReady() && rc.canMineSoup(dir)) {
	        rc.mineSoup(dir);
	        return true;
	    } else return false;
	}

	public boolean canMove(Direction dir) throws GameActionException {
		return super.canMove(dir) && !(locHQ != null && rc.getLocation().add(dir).isAdjacentTo(locHQ) && !((status == MinerStatus.DEPOSITING || status == MinerStatus.RETURNING) && locHQ.equals(chosenRefinery)) && !rc.getLocation().isAdjacentTo(locHQ));
	}

}
