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
	int wallSquares; /* should be 8, unless we're in a corner or edge */
	int specialMiner;
	boolean builtDesignSchool;
	Direction turtleDesignSchoolDir;
	boolean landscaperWalled;
	int buildingDesignSchool;
	int attackTimer;
	MapLocation buildingMinerLoc;
	MapLocation buildingDSLoc;
	int domesticScapers;
	boolean isFarFromEdge;
	HQstatus status;

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
		attackTimer = Integer.MAX_VALUE;
		waitingForBuilding = Integer.MAX_VALUE;
		units = new ArrayList[10];
		for(int i=0; i<10; i++){
			units[i] = new ArrayList<InternalUnit>();
		}

		wallSquares = 0;
		MapLocation loc = rc.getLocation();
		for (Direction dir: directions) {
			MapLocation nloc = loc.add(dir);

			if (isValidWall(nloc)) ++wallSquares;
		}
		isFarFromEdge = (loc.x >= 2 && loc.y >=2 && loc.x < rc.getMapWidth()-2 && loc.y < rc.getMapHeight()-2);
		status = HQstatus.FIRST_MINERS;
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

		switch(status){
			case FIRST_MINERS:
			//Build original miners
				if (builtMiners < 4) {
					for (Direction dir : directions) {
						if (builtMiners < 8 && rc.canBuildRobot(RobotType.MINER, dir)) {
							rc.buildRobot(RobotType.MINER, dir);
							builtMiners++;
						}
					}
					break;
				}else {
					status = HQstatus.FIRST_LANDSCAPERS;
					//No break here
				}
			case FIRST_LANDSCAPERS:


		}


		// build wall
		if (buildingMinerLoc != null) {
			writeMessage(Message.build(RobotType.DESIGN_SCHOOL, rc.senseRobotAtLocation(buildingMinerLoc).getID(), buildingDSLoc));
			buildingDesignSchool = 1;
			turtleDesignSchoolDir = rc.getLocation().directionTo(buildingMinerLoc);
			buildingMinerLoc = null;
		}
		if (!builtDesignSchool) {
			if (buildingDesignSchool-1 > 11) builtDesignSchool = true;
			else if (buildingDesignSchool > 0) buildingDesignSchool++;
		}
		if (domesticScapers >= wallSquares && !landscaperWalled) {
			landscaperWalled = true;
			writeMessage(Message.buildWall(rc.getLocation()));
		}
		if (buildingDesignSchool == 0 && units[2 /*refinery*/].size() > 0 && rc.getTeamSoup() > 70 && rc.isReady()) {
			for (Direction dir : orthogonalDirections) {
				MapLocation minerLoc = rc.getLocation().add(dir);
				MapLocation buildLoc = minerLoc.add(dir.rotateRight());
				if (rc.onTheMap(buildLoc) && Math.abs(rc.senseElevation(minerLoc)-rc.senseElevation(buildLoc)) <= GameConstants.MAX_DIRT_DIFFERENCE && !rc.senseFlooding(buildLoc) && !rc.isLocationOccupied(minerLoc) && !rc.isLocationOccupied(buildLoc)) {
					buildingMinerLoc = minerLoc;
					buildingDSLoc = buildLoc;
					rc.buildRobot(RobotType.MINER, dir);
					break;
				}
				buildLoc = minerLoc.add(dir.rotateLeft());
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
            ArrayList<InternalUnit> miners = units[RobotType.MINER.ordinal()];
            InternalUnit miner = miners.get(r.nextInt(miners.size()));
            writeMessage(Message.build(RobotType.FULFILLMENT_CENTER, miner.id));
        }




		attackTimer--;
		if(attackTimer > 200 && units[7 /*drone*/].size() >= 7){
			attackTimer = 200;
		}
		if(attackTimer <= 0){
			attackTimer = 200;
			writeMessage(Message.droneAttack());
			System.out.println("Called attack");
		}


		//Broadcast important info every 9 rounds
		if(rc.getRoundNum() %9 == 0){
			if(enemyHQ != null){
				writeMessage(Message.enemyHqLocation(enemyHQ));
			}
			writeMessage(Message.hqLocation(rc.getLocation()));
		}

	}

	public boolean executeMessage(Message message){
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
						}
						break;
					case LANDSCAPER:
						if(enemyHQ == null || location.distanceSquaredTo(enemyHQ) > 18){
							domesticScapers++;
							if(domesticScapers <= wallSquares){
								writeMessage(Message.doSomething(unitID, 0));	//Defend
							}else if(isFarFromEdge && domesticScapers <= wallSquares+12){
								writeMessage(Message.doSomething(unitID, 3));	//Corner
							}else{
								writeMessage(Message.doSomething(unitID, 1));	//Terraform
							}
						}
						break;
					case DELIVERY_DRONE:
						if(units[RobotType.DELIVERY_DRONE.ordinal()].size() >= 2  && (units[RobotType.DELIVERY_DRONE.ordinal()].size() <= 4
							|| enemyHQ == null)){
							//Harass own hq location for defense
							writeMessage(Message.tellHarass(unitID, rc.getLocation()));
						}else if(units[RobotType.DELIVERY_DRONE.ordinal()].size() >= 5){
							//Harass opponent's hq to be annoying
							writeMessage(Message.tellHarass(unitID, enemyHQ));
						}
						break;
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