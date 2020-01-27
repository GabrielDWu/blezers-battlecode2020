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
		NOTHING,
		FIND_ENEMY_HQ,
		RUSH_ENEMY_HQ,
		BUILD_VAPORATOR,
		GO_TO_TERRAFORM,
		BUILDING_DS,
		BUILD_DEFENSIVE_NETGUN
	}

	boolean onTerraform;

	MinerStatus status = null;
	MinerStatus prevStatus = null;

	MapLocation soupLoc = null;
	int[][] soupTries;

	RobotType buildingType = null;
	int buildingTries = 0;
	public ArrayList<MapLocation> locREFINERY;
	public ArrayList<MapLocation> locNETGUN;
	MapLocation chosenRefinery;
	MapLocation buildLocation = null;
	boolean findingEnemyHQ;
	MapLocation[] enemyHQs;

	MapLocation newDS;
	boolean builtFirstDS;
	boolean builtDS;
	int buildingDSTries = 0;

	boolean fulfillmentCenterBuilt;

	int vaporatorHeight = 0;
	boolean sentFound = false;
	int enemyHQc;
	final static int minNetGunRadius = 36;
	final static int maxNetGunRadius = 100;
	final static int netGunModulus = 20;
	final static int netGunSpread = 6;
	final static int netGunCoolDown = 10;
	int lastBuiltNetGun = Integer.MIN_VALUE;
	int buildableTiles;

	int returnTries;

	static int minerTerraformDist = 3;

	public Miner(RobotController rc) throws GameActionException {
		super(rc);
	}

	public void startLife() throws GameActionException{
		super.startLife();
		locREFINERY = new ArrayList<MapLocation>();
		locNETGUN = new ArrayList<MapLocation>();
		lastBuiltNetGun =Integer.MIN_VALUE;
		buildableTiles = -1;
		enemyHQs = new MapLocation[3];
		enemyHQc = -1;
	}
	public boolean canBuildVaporator(MapLocation a) throws GameActionException {
		if(rc.canSenseLocation(a) == false) return false;
		if(rc.senseElevation(a) < vaporatorHeight) return false;
		boolean ok = false;
		if(isLattice(a) || a.distanceSquaredTo(locHQ) <= 16) return false;
		for(MapLocation refine: locREFINERY){
			if(refine.distanceSquaredTo(a) <= RobotType.VAPORATOR.pollutionRadiusSquared){
				ok = true;
				break;
			}
		}
		if(a.distanceSquaredTo(locHQ) <= RobotType.VAPORATOR.pollutionRadiusSquared) ok = true;
		if(!ok) return false;

		return true;
	}
	public boolean canBuildDefensiveNetGun(MapLocation a) throws GameActionException {
		if(rc.canSenseLocation(a) == false) return false;
		if(isLattice(a)) return false;
		if(rc.senseFlooding(a)) return false;
		if(rc.isLocationOccupied(a)) return false;
		int dist = a.distanceSquaredTo(locHQ);
		if(dist<minNetGunRadius) return false;
		if(dist>maxNetGunRadius) return false;
		//if(rc.getRoundNum()<lastBuiltNetGun + netGunCoolDown) return false;
	//	if(rc.senseElevation(a) <= terraformHeight && locNETGUN.size()>= 3) return false;
		if(locNETGUN.size() >= maxDefensiveNetGuns) return false;
		for(MapLocation n: locNETGUN){
			if(rc.getLocation().distanceSquaredTo(n)< netGunSpread) return false;
		}
		return true;
	}
	public Direction buildVaporator() throws GameActionException {
		MapLocation mloc = rc.getLocation();
		for(Direction dir: directions){
			MapLocation nloc= mloc.add(dir);
			if(canBuildRobot(RobotType.VAPORATOR, dir) && canBuildVaporator(nloc)){
				return dir;
			}
		}
		return null;
	}
	public void run() throws GameActionException {
		if (status == MinerStatus.GO_TO_TERRAFORM) reducedRunRadius = true;
		else reducedRunRadius = false;
		super.run();
		if (soupTries == null && sentInfo) soupTries = new int[rc.getMapWidth()][rc.getMapHeight()];
		if (sentInfo) {
			if (status == MinerStatus.NOTHING) {
				if (!safeFromFlood[Direction.CENTER.ordinal()]) {
					randomMove();
				}
				return;
			}
			if (status == null) status = MinerStatus.SEARCHING;
			setVisitedAndSeen();
			MapLocation nloc = null;
			MapLocation mloc = rc.getLocation();
			if (!(status == MinerStatus.DEPOSITING && chosenRefinery != null && chosenRefinery.equals(locHQ)) && prevStatus != MinerStatus.NOTHING && !(/*building design school by HQ*/ status == MinerStatus.BUILDING && buildingType == RobotType.DESIGN_SCHOOL && buildLocation != null && buildLocation.isAdjacentTo(mloc)) && locHQ != null && mloc.isAdjacentTo(locHQ)) {
				goTo(mloc.add(mloc.directionTo(locHQ).opposite()));
			}
			int h = rc.getMapHeight();
			int w = rc.getMapWidth();
			Direction buildVaporatorDirection = buildVaporator();
			if (onTerraform) {
				//if(numVaporators<= maxVaporators/2 && status == MinerStatus.BUILDING && (buildingType == RobotType.FULFILLMENT_CENTER ||buildingType==RobotType.REFINERY && locREFINERY.size() >2)){
				if(numVaporators<= maxVaporators/2 && status == MinerStatus.BUILDING && (buildingType == RobotType.FULFILLMENT_CENTER) && rc.getTeamSoup() < 500){
					status = MinerStatus.MINING;
				}
				if((status == MinerStatus.SEARCHING || status == MinerStatus.MINING || status == MinerStatus.DEPOSITING ||
						status == MinerStatus.RETURNING) && numVaporators<maxVaporators){
					if(buildVaporatorDirection != null){
						status = MinerStatus.BUILD_VAPORATOR;
					}
				}
				if (/*r.nextInt(20) == 0 && */numVaporators > 0 && !builtDS && (buildingDSTries == 0 || buildingDSTries > 50)) {
					buildingDSTries = 1;
					while(newDS == null || isLattice(newDS) || !rc.onTheMap(newDS) || (rc.canSenseLocation(newDS) && Math.abs(rc.senseElevation(newDS) - terraformHeight) > Landscaper.terraformThreshold)) {
						newDS = locHQ.translate((r.nextInt(3)-1)*3, (r.nextInt(3)-1)*3);
					}

					status = MinerStatus.BUILDING_DS;
				}
			}
			Direction buildDefensiveNetGunDirection = null;
			if (onTerraform) {
				//if(numVaporators<= maxVaporators/2 && status == MinerStatus.BUILDING && (buildingType == RobotType.FULFILLMENT_CENTER ||buildingType==RobotType.REFINERY && locREFINERY.size() >2)){
				RobotInfo[] robo = rc.senseNearbyRobots();
				int close = Integer.MAX_VALUE;

				if(!(numVaporators == 1 || numVaporators == 3 || rc.getRoundNum()%netGunModulus == 0)){
					// do nothing
				}
				else{
					for(Direction dir: directions){
							if(canBuildDefensiveNetGun(rc.getLocation().add(dir)) && rc.canBuildRobot(RobotType.NET_GUN, dir)) buildDefensiveNetGunDirection = dir;
					}
					for(RobotInfo r: robo){
						if(buildDefensiveNetGunDirection != null &&
								r.type == RobotType.NET_GUN && r.getTeam() == rc.getTeam() &&
								r.location.distanceSquaredTo(rc.getLocation().add(buildDefensiveNetGunDirection)) <= close){
							close = r.location.distanceSquaredTo(rc.getLocation());
						}
					}
					close = Integer.MAX_VALUE;
					if((status == MinerStatus.SEARCHING || status == MinerStatus.MINING || status == MinerStatus.DEPOSITING ||
							status == MinerStatus.RETURNING) && numDefensiveNetGuns<maxDefensiveNetGuns){
						if(buildDefensiveNetGunDirection != null && close>=netGunSpread){
							status = MinerStatus.BUILD_DEFENSIVE_NETGUN;
						}
					}
				}
			}

			switch (status) {
				case BUILD_DEFENSIVE_NETGUN:
					rc.buildRobot(RobotType.NET_GUN, buildDefensiveNetGunDirection);
					status = prevStatus == null ? MinerStatus.MINING : prevStatus;
					lastBuiltNetGun = rc.getRoundNum();
					break;

				case BUILDING_DS:
					if (builtDS) {
						status = MinerStatus.MINING;
						break;
					}
					if (!mloc.isAdjacentTo(newDS)) {
						goTo(newDS);
					} else {
						if (rc.canBuildRobot(RobotType.DESIGN_SCHOOL, mloc.directionTo(newDS))) {
							rc.buildRobot(RobotType.DESIGN_SCHOOL, mloc.directionTo(newDS));
							buildingDSTries = 0;
							builtDS = true;
						} else ++buildingDSTries;
					}
					break;
				case BUILD_VAPORATOR:
					rc.buildRobot(RobotType.VAPORATOR,buildVaporatorDirection);
					status = (prevStatus == null ? MinerStatus.MINING: prevStatus);
					break;
				case FIND_ENEMY_HQ:
					if(rc.getRoundNum()>=300) {
						status = MinerStatus.SEARCHING;
						break;
					}
					if (enemyHQ != null) {
						status = MinerStatus.RUSH_ENEMY_HQ;
						break;
					}
					MapLocation loc = findEnemyHQ();
					if (loc != null && !sentFound) {
						enemyHQ = loc;
						writeMessage(Message.enemyHqLocation(enemyHQ));
						sentFound = true;
					}
					break;
				case RUSH_ENEMY_HQ:
					if(rc.getLocation().isAdjacentTo(enemyHQ) == true){
						if(rc.getTeamSoup()<RobotType.DESIGN_SCHOOL.cost) break;
						Direction di = null;
						int best = Integer.MAX_VALUE;
						for(Direction d: directions){
							int dist = enemyHQ.distanceSquaredTo(rc.getLocation().add(d));
							if(canBuildRobot(RobotType.DESIGN_SCHOOL, d) && rushHQHelper(best, dist)) {
								best = enemyHQ.distanceSquaredTo(rc.getLocation().add(d));;
								di = d;
							}
						}
						if(di == null){
							status = MinerStatus.SEARCHING;
						}
						else{
							rc.buildRobot(RobotType.DESIGN_SCHOOL, di);
							status = MinerStatus.SEARCHING;
						}
					}
					else{
						goTo(enemyHQ);
					}
					break;
				case BUILDING:
					if(buildingType == null) status = MinerStatus.SEARCHING;
					if(buildLocation == null){	// can build anywhere far from hq
						if (buildingTries++ > 3){
							status = prevStatus == null ? MinerStatus.MINING : prevStatus;
							if (status == MinerStatus.RETURNING) returnTries = 0;
						}
						for (Direction dir : directions) {
							if (rc.getLocation().add(dir).distanceSquaredTo(locHQ) >= 9 && buildingType != null && !isLattice(rc.getLocation().add(dir)) &&
									canBuildRobot(buildingType, dir)) {
								rc.buildRobot(buildingType, dir);
								status = prevStatus == null ? MinerStatus.MINING : prevStatus;
								if (status == MinerStatus.RETURNING) returnTries = 0;
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
							randomMove();
						}else if(rc.getLocation().isAdjacentTo(buildLocation)){
							if(canBuildRobot(buildingType, rc.getLocation().directionTo(buildLocation))){
								rc.buildRobot(buildingType, rc.getLocation().directionTo(buildLocation));
								status = prevStatus == null ? MinerStatus.MINING : prevStatus;
								if (status == MinerStatus.RETURNING) returnTries = 0;
							}
						}else{
							goTo(buildLocation);
						}

					}
					break;
				case SEARCHING:
					MapLocation[] nsoup = rc.senseNearbySoup();
					if (nsoup.length > 0) {
						for (int i = 0; i < nsoup.length; i++) {
							if (soupTries[nsoup[i].x][nsoup[i].y] <= 6) {
								status = MinerStatus.MINING;
								soupLoc = nsoup[i];
								break;
							}
						}
					}

					if (status == MinerStatus.SEARCHING) {
						findSoup();
						break;
					}
					// start mining if it saw soup
				case MINING:
					if (!safeFromFlood[Direction.CENTER.ordinal()]) {
						randomMove();
					}
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
							setChosenRefinery();
							int refineryDist = rc.getLocation().distanceSquaredTo(chosenRefinery);
							if (refineryDist > 48 || refineryDist > 15 && locREFINERY.size() < 3 || chosenRefinery.equals(locHQ) && rc.getTeamSoup() >= 150) {
								boolean alreadyExists = false;
								RobotInfo[] nearby = rc.senseNearbyRobots(15, rc.getTeam());
								for (RobotInfo r : nearby) {
									if (r.getType() == RobotType.REFINERY) {
										alreadyExists = true;
									}
								}
								if (!alreadyExists) {
									prevStatus = MinerStatus.RETURNING;
									status = MinerStatus.BUILDING;
									buildingType = RobotType.REFINERY;
									buildLocation = null;
									buildingTries = 0;
								}
							}
							if (status != MinerStatus.BUILDING) {
								status = MinerStatus.RETURNING;
								returnTries = 0;
							}
						}
						else if (soupLoc != null && rc.canSenseLocation(soupLoc) && rc.senseSoup(soupLoc) == 0) {
							soupLoc = null;
							if (status != MinerStatus.RETURNING) status = MinerStatus.SEARCHING;
						}
					}
					break;
				case RETURNING:
					setChosenRefinery();
					goTo(chosenRefinery);
					if (rc.getLocation().isAdjacentTo(chosenRefinery)) {
						status = MinerStatus.DEPOSITING;
					}
					else if (++returnTries % 50 == 0) {
						prevStatus = MinerStatus.RETURNING;
						status = MinerStatus.BUILDING;
						buildingType = RobotType.REFINERY;
						buildLocation = null;
						buildingTries = 0;
					}
					break;
				case DEPOSITING:
					if (!safeFromFlood[Direction.CENTER.ordinal()]) {
						randomMove();
					}
					if (rc.canDepositSoup(mloc.directionTo(chosenRefinery))) rc.depositSoup(mloc.directionTo(chosenRefinery), rc.getSoupCarrying());
					if (rc.getSoupCarrying() == 0) {
						chosenRefinery = null;
						if (soupLoc !=  null) {
							status = MinerStatus.MINING;
						} else status = MinerStatus.SEARCHING;
					}
					break;
				case GO_TO_TERRAFORM:
					if (isLattice(mloc) || kingDistance(mloc, locHQ) != 2) {
						/* if we're too close to HQ, move */
						/* also if we're in a lattice square, move */
						if (!moveToward(mloc, locHQ)) moveAway(mloc, locHQ);
					}
					if (Math.abs(rc.senseElevation(rc.getLocation()) - terraformHeight) <= 1) {
						onTerraform = true;
						status = MinerStatus.MINING;
					}
					break;
			}
		}
	}
	public void setChosenRefinery() throws GameActionException {
		chosenRefinery = locHQ;
		int best = Integer.MAX_VALUE;
		for (MapLocation rloc : locREFINERY) {
			if (rc.getLocation().distanceSquaredTo(rloc) < best) {
				chosenRefinery = rloc;
				best = rc.getLocation().distanceSquaredTo(rloc);
			}
		}
	}
	public boolean rushHQHelper(int a, int b){
		if(a == 0) return true;
		if(a == 1||a==2) return false;
		if(b == 0) return false;
		if(b<= a) return true;
		return false;
	}
	public MapLocation findEnemyHQ() throws GameActionException {
		if (enemyHQc == -1) {
			findingEnemyHQ = true;
			MapLocation mloc = rc.getLocation();
			int h = rc.getMapHeight();
			int w = rc.getMapWidth();
			MapLocation horizontal = new MapLocation(((((w-1-locHQ.x)%w)+w)%w), locHQ.y);
			MapLocation vertical = new MapLocation(locHQ.x, ((((h-1-locHQ.y)%h)+h)%h));
			MapLocation diagonal = new MapLocation(horizontal.x, vertical.y);
			// check these two first since they are the closet
			if (mloc.distanceSquaredTo(horizontal) < mloc.distanceSquaredTo(vertical)) {
				enemyHQs[0] = horizontal;
				enemyHQs[1] = diagonal;
				enemyHQs[2] = vertical;
			} else {
				enemyHQs[0] = vertical;
				enemyHQs[1] = diagonal;
				enemyHQs[2] = horizontal;
			}
			enemyHQc = 0;
		}
		goTo(enemyHQs[enemyHQc]);
		MapLocation r = detectEnemyHQ();
		if (r != null) {
			findingEnemyHQ = false;
			return r;
		} else if (rc.canSenseLocation(enemyHQs[enemyHQc])) {
			enemyHQc++;
			if (enemyHQc >= enemyHQs.length) {
				findingEnemyHQ = false;
			}
		}
		return null;
	}
	public MapLocation detectEnemyHQ() {
		RobotInfo[] near = rc.senseNearbyRobots(rc.getCurrentSensorRadiusSquared(), (rc.getTeam() == Team.A ? Team.B : Team.A));
		for(RobotInfo x: near){
			if(x.getType() == RobotType.HQ){
				return x.location;
			}
		}
		return null;
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
        int max = Integer.MIN_VALUE;

        //I'm pretty sure this is random enough and preserves the bytecode
		int ri = r.nextInt(numDir);
        for(int i=0; i<numDir; i++) {
            int newv = newSeenList[(i+ri)%numDir];
            Direction newl = newSeenDirs[(i+ri)%numDir];
            if (newv > max && canMove(newl)) {
                maxl = newl;
                max = newv;
            }
        }
        tryMove(maxl);
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

	public boolean executeMessage(Message message) throws GameActionException {
		/*Returns true if message applies to me*/
		if(super.executeMessage(message)){
			return true;
		}
		switch (message.type) {
			case BIRTH_INFO:
				//Miners want to store refinery locations
				RobotType unit_type = robot_types[message.data[0]];
				if (unit_type == RobotType.FULFILLMENT_CENTER) {
					fulfillmentCenterBuilt = true;
					return true;
				}
				if (unit_type == RobotType.DESIGN_SCHOOL) {
					if(enemyHQ != null && (new MapLocation(message.data[2], message.data[3])).distanceSquaredTo(enemyHQ)<= 8){
						// rush design school
					} else {
						if (!builtFirstDS) builtFirstDS = true;
						else builtDS = true;
						return true;
					}
				}
				if(unit_type == RobotType.NET_GUN){
					locNETGUN.add(new MapLocation(message.data[2], message.data[3]));
					System.out.println(locNETGUN);
					return true;
				}
				if(unit_type != RobotType.REFINERY){
					return false;
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
				switch(message.data[1]){
					case 2:
						status = MinerStatus.FIND_ENEMY_HQ;
						return true;
					case 4:
						status = MinerStatus.GO_TO_TERRAFORM;
						return true;
				}
				return false;
			case REFINERY_LOC:
				if (message.data[2] != rc.getID()) return false;
				locREFINERY.add(new MapLocation(message.data[0], message.data[1]));
				return true;
			case DEATH:
				RobotType type = robot_types[message.data[1]];
				if (type == RobotType.REFINERY) {
					locREFINERY.remove(new MapLocation(message.data[2], message.data[3]));
					return true;
				}
				if(type == RobotType.VAPORATOR){
					numVaporators--;
				}
				if(type == RobotType.NET_GUN){
					locNETGUN.remove(new MapLocation(message.data[2], message.data[3]));
				}
				return false;
			case BUILD_ANY:
				if (onTerraform && !fulfillmentCenterBuilt && builtDS) {
					buildingType = robot_types[message.data[0]];
					buildingTries = 0;
					prevStatus = status == MinerStatus.BUILDING ? prevStatus : status;
					status = MinerStatus.BUILDING;
					buildLocation = null;
					return true;
				}
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
		return dir != null && super.canMove(dir)
				&& (!onTerraform || (rc.canSenseLocation(rc.getLocation().add(dir))
				&& Math.abs(rc.senseElevation(rc.getLocation().add(dir)) - terraformHeight) <= 1))
				&&  !(locHQ != null && rc.getLocation().add(dir).isAdjacentTo(locHQ)
					&& !((status == MinerStatus.DEPOSITING || status == MinerStatus.RETURNING)
						&& locHQ.equals(chosenRefinery))
					&& !rc.getLocation().isAdjacentTo(locHQ) && !(onTerraform && !builtDS));
	}

	public boolean canBuildRobot(RobotType type, Direction dir) {
		return rc.canBuildRobot(type, dir) && (!onTerraform || isForBuilding(rc.getLocation().add(dir)));
	}
}
