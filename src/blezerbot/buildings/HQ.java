package blezerbot.buildings;

import battlecode.common.*;
import java.util.*;
import blezerbot.*;

import static blezerbot.Message.MessageType;

public class HQ extends Building {

	int builtMiners;
	boolean hq_sentLoc;
	public ArrayList<InternalUnit>[] units;

	/* turtling stuff */
	int waitingForBuilding;
	int wallSquares, adjacentWallSquares; /* should be 8, unless we're in a corner or edge */
	int specialMiner;
	final static int lessDrones = 1300;
	boolean builtDesignSchool;
	boolean landscaperWalled;
	int buildingDesignSchool;
	int attackTimer;
	MapLocation buildingMinerLoc;
	MapLocation buildingDSLoc;
	int domesticScapers;
	boolean isFarFromEdge;
	HQstatus status;
	int wallLandscapers;
	int buildWallTurns;
	int wallDesignSchoolID;
	int rushTurns;
	public final static int wallMessageDelay = 10; /* short cooldown before sending build wall message */

	public static enum HQstatus{
		FIRST_MINERS,
		FIRST_LANDSCAPERS,
		INNER_TURTLE,
		VAPES_DRONES,
		CORNERS
	}

	/* post turtle stuff */

	public HQ(RobotController rc)throws GameActionException {
		super(rc);
	}

	public void startLife() throws GameActionException{
		super.startLife();
		wallDesignSchoolID = -1;
		attackTimer = 300;
		waitingForBuilding = Integer.MAX_VALUE;
		units = new ArrayList[10];
		for(int i=0; i<10; i++){
			units[i] = new ArrayList<InternalUnit>();
		}

		wallSquares = 0;
		adjacentWallSquares = 0;
		MapLocation loc = rc.getLocation();
		for (Direction dir: directions) {
			MapLocation nloc = loc.add(dir);

			if (isValidWall(nloc)) {
				++wallSquares;
				++adjacentWallSquares;
				if (!orthogonal(dir)) wallSquares += 3;
			}
		}
		isFarFromEdge = (loc.x >= 2 && loc.y >=2 && loc.x < rc.getMapWidth()-2 && loc.y < rc.getMapHeight()-2);
		status = HQstatus.FIRST_MINERS;
		buildWallTurns = 0;
	}

	public void run() throws GameActionException {
		super.run();
		//Shoot enemy drones
		int unitToShoot = -1;
		int dist = 100;
		for(RobotInfo enemy: rc.senseNearbyRobots(-1, (rc.getTeam() == Team.B)?Team.A:Team.B)){
			if(enemy.type == RobotType.DELIVERY_DRONE){
				if(rc.canShootUnit(enemy.ID) && rc.getLocation().distanceSquaredTo(enemy.getLocation()) < dist){
					unitToShoot = enemy.ID;
					dist = rc.getLocation().distanceSquaredTo(enemy.getLocation());
				}
			}
		}
		if(unitToShoot >= 0) rc.shootUnit(unitToShoot);
		if (enemyHQ != null && rushTurns == 0) {
			rushTurns = 1;
		}


		switch(status){
			case FIRST_MINERS:
			//Build original miners
				if (builtMiners < TOTAL_MINERS && (rushTurns == 0 || rushTurns++ > 40)) {
					int di = r.nextInt(directions.length);
					for (int i = 0; i < directions.length; i++) {
						Direction dir = directions[(di+i)%directions.length];
						if (builtMiners < TOTAL_MINERS && rc.canBuildRobot(RobotType.MINER, dir)) {
							rc.buildRobot(RobotType.MINER, dir);
							builtMiners++;
						}
					}
					break;
				} else {
					status = HQstatus.FIRST_LANDSCAPERS;
					//No break here
				}

		}

		// begin terraform
		if (buildingMinerLoc != null) {
			writeMessage(Message.build(RobotType.DESIGN_SCHOOL, rc.senseRobotAtLocation(buildingMinerLoc).getID(), buildingDSLoc));
			buildingDesignSchool = 1;
			buildingMinerLoc = null;
		}
		if (!builtDesignSchool) {
			if (buildingDesignSchool-1 > 11) builtDesignSchool = true;
			else if (buildingDesignSchool > 0) buildingDesignSchool++;
		}
		if (wallLandscapers >= wallSquares) {
			buildWallTurns = Math.max(buildWallTurns, wallMessageDelay - 10);
		}
		if (wallLandscapers >= adjacentWallSquares && !landscaperWalled) {
			++buildWallTurns;
			if (buildWallTurns >= wallMessageDelay) {
				landscaperWalled = true;
				writeMessage(Message.buildWall(rc.getLocation()));
			}
		}
		if (buildingDesignSchool == 0 && units[2 /*refinery*/].size() > 0 && rc.getTeamSoup() > 70 && rc.isReady()) {
			for (Direction dir : orthogonalDirections) {
				MapLocation minerLoc = rc.getLocation().add(dir);
				MapLocation buildLoc = minerLoc.add(dir);
				if (rc.onTheMap(buildLoc) && Math.abs(rc.senseElevation(minerLoc)-rc.senseElevation(buildLoc)) <= GameConstants.MAX_DIRT_DIFFERENCE && !rc.senseFlooding(buildLoc) && !rc.isLocationOccupied(minerLoc) && !rc.isLocationOccupied(buildLoc)) {
					buildingMinerLoc = minerLoc;
					buildingDSLoc = buildLoc;
					rc.buildRobot(RobotType.MINER, dir);
					break;
				}
			}
		}

		MapLocation mloc = rc.getLocation();
		if (waitingForBuilding < Integer.MAX_VALUE) waitingForBuilding++;
		if(!hq_sentLoc){
			writeMessage(Message.hqLocation(rc.getLocation()));
			addMessageToQueue(base_wager*3);
			hq_sentLoc = true;
		}

        if (landscaperWalled && units[5 /*fc*/].size() == 0 && units[1].size() > 0 /*miner*/ && waitingForBuilding > 10 && rc.getTeamSoup() > 200) {
            waitingForBuilding = 1;
            writeMessage(Message.build(RobotType.FULFILLMENT_CENTER));
        }



		attackTimer--;
        if(attackTimer == 0 && numDrones>=18) {
        	attackTimer = 400;
        	writeMessage(Message.droneAttack());
		}
        if(attackTimer<= 0) attackTimer = 400;



		//Broadcast important info every 9 rounds
		if(rc.getRoundNum() %9 == 0){
			if(enemyHQ != null){
				writeMessage(Message.enemyHqLocation(enemyHQ));
			}
			writeMessage(Message.hqLocation(rc.getLocation()));
		}

	}

	public boolean executeMessage(Message message) throws GameActionException {
		/*Returns true if message applies to me*/
		if(super.executeMessage(message)){
			return true;
		}
		//HQ specific methods:
		switch (message.type) {
			case BIRTH_INFO:
				RobotType unitType = robot_types[message.data[0]];
				int unitID = message.data[1];
				MapLocation location = new MapLocation(message.data[2], message.data[3]);
				units[unitType.ordinal()].add(new InternalUnit(unitType,unitID, location));
				debug("Added unit " + units[unitType.ordinal()].get(units[unitType.ordinal()].size()-1));

				switch(unitType){
					case MINER:
						if(units[RobotType.MINER.ordinal()].size() == 1){
							writeMessage(Message.doSomething(unitID, 2));	//Rush
							addMessageToQueue();
						}else if (status==HQstatus.FIRST_LANDSCAPERS){
							specialMiner = unitID;
						} else if (units[RobotType.MINER.ordinal()].size() > 3) {
							writeMessage(Message.doSomething(unitID, 4)); // go to terraform
							addMessageToQueue();
						}
						break;
					case LANDSCAPER:
						if(enemyHQ == null || location.distanceSquaredTo(enemyHQ) > 18){
							domesticScapers++;
							if(domesticScapers <= TERRAFORM_LANDSCAPERS){
								writeMessage(Message.doSomething(unitID, 1));	//Terraform
							}else if(wallLandscapers >= adjacentWallSquares){
								writeMessage(Message.doSomething(unitID, 3));	//Corner
								wallLandscapers++;
							}else{
								writeMessage(Message.doSomething(unitID, 0));	//Defend
								wallLandscapers++;
							}
							if (wallLandscapers > wallSquares+2 /*2 extra because it tends to be missing a couple*/ && wallDesignSchoolID != -1) {
								writeMessage(Message.doSomething(wallDesignSchoolID, 12));
							}
							addMessageToQueue();
						}
						break;
					case DELIVERY_DRONE:
						int droneCount = units[RobotType.DELIVERY_DRONE.ordinal()].size();
						/*if(units[RobotType.DELIVERY_DRONE.ordinal()].size() >= 2  && (units[RobotType.DELIVERY_DRONE.ordinal()].size() <= 4
							|| enemyHQ == null)){
							//Harass own hq location for defense
							writeMessage(Message.tellHarass(unitID, rc.getLocation()));
						}
						else if(units[RobotType.DELIVERY_DRONE.ordinal()].size() >= 5){
							//Harass opponent's hq to be annoying
							writeMessage(Message.tellHarass(unitID, enemyHQ));
						}*/


						if(((rc.getRoundNum()<=lessDrones && numDrones%7<=1)||(rc.getRoundNum()>lessDrones&numDrones%7 == 0) )&& enemyHQ != null){
							System.out.println("ATTACK");

							writeMessage(Message.tellHarass(unitID, enemyHQ));
						}
						else{
							writeMessage(Message.tellHarass(unitID, rc.getLocation()));
						}
						break;
					case DESIGN_SCHOOL:
						if(enemyHQ != null && location.distanceSquaredTo(enemyHQ)<= 8){
							// rush design school
						} else {
							if (units[RobotType.DESIGN_SCHOOL.ordinal()].size() == 1) {
								writeMessage(Message.doSomething(unitID, 7)); // initial design school
							} else {
								wallDesignSchoolID = unitID;
							}
						}
				}
				return true;
		}
		return false;
	}

	public class InternalUnit{
		/*HQ uses this class to keep track of all of our units.*/
		public RobotType type;
		public int id;
		public MapLocation lastSent;

		public InternalUnit(RobotType t, int id, MapLocation loc){
			this.type = t;
			this.id = id;
			lastSent = loc;
		}

		public String toString(){
			return type + " (" + id + ") "+lastSent;
		}
	}

}