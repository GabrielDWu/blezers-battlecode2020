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
	boolean builtDesignSchool;
	Direction turtleDesignSchoolDir;
	boolean landscaperWalled;
	int buildingDesignSchool;
	int attackTimer;
	MapLocation buildingMinerLoc;

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
	}

	public void run() throws GameActionException {
		super.run();
		//Shoot enemy drones
		for(RobotInfo enemy: rc.senseNearbyRobots(-1, (rc.getTeam() == Team.B)?Team.A:Team.B)){
			if(enemy.type == RobotType.DELIVERY_DRONE){
				if(rc.canShootUnit(enemy.ID)){
					rc.shootUnit(enemy.ID);
					break;
				}
			}
		}

		// build wall
		if (buildingMinerLoc != null) {
			System.out.println(rc.senseRobotAtLocation(buildingMinerLoc).getID());
			writeMessage(Message.build(RobotType.DESIGN_SCHOOL, rc.senseRobotAtLocation(buildingMinerLoc).getID(), buildingMinerLoc.add(buildingMinerLoc.directionTo(locHQ).opposite())));
			addMessageToQueue();
			buildingDesignSchool = 1;
			turtleDesignSchoolDir = rc.getLocation().directionTo(buildingMinerLoc);
			buildingMinerLoc = null;
		}
		if (!builtDesignSchool) {
			if (buildingDesignSchool-1 > 11) builtDesignSchool = true;
			else if (buildingDesignSchool > 0) buildingDesignSchool++;
		}
		if (units[RobotType.LANDSCAPER.ordinal()].size() >= 8 && !landscaperWalled) {
			landscaperWalled = true;
			writeMessage(Message.buildWall(rc.getLocation()));
			addMessageToQueue();
		}
		if (buildingDesignSchool == 0 && units[2 /*refinery*/].size() > 0 && rc.getTeamSoup() > 70 && rc.isReady()) {
			for (Direction dir : orthogonalDirections) {
				MapLocation minerLoc = rc.getLocation().add(dir);
				MapLocation buildLoc = minerLoc.add(dir);
				if (rc.onTheMap(buildLoc) && Math.abs(rc.senseElevation(minerLoc)-rc.senseElevation(buildLoc)) <= GameConstants.MAX_DIRT_DIFFERENCE && !rc.senseFlooding(buildLoc) && !rc.isLocationOccupied(minerLoc) && !rc.isLocationOccupied(buildLoc)) {
					buildingMinerLoc = minerLoc;
					rc.buildRobot(RobotType.MINER, dir);
					break;
				}
			}
		}

		MapLocation mloc = rc.getLocation();
		if (waitingForBuilding < Integer.MAX_VALUE) waitingForBuilding++;
		if(!hq_sentLoc){
			writeMessage(Message.hqLocation(rc.getLocation()));
			addMessageToQueue();
			hq_sentLoc = true;
		}

        if (units[2 /*refinery*/].size() == 0 && units[1].size() > 0 /*miner*/ && waitingForBuilding > 10 && rc.getTeamSoup() > 200) {
            waitingForBuilding = 1;
            ArrayList<InternalUnit> miners = units[RobotType.MINER.ordinal()];
            InternalUnit miner = miners.get(r.nextInt(miners.size()));
            writeMessage(Message.build(RobotType.REFINERY, miner.id));
            addMessageToQueue();
        }
        if (landscaperWalled && units[5 /*fc*/].size() == 0 && units[1].size() > 0 /*miner*/ && waitingForBuilding > 10 && rc.getTeamSoup() > 200) {
            waitingForBuilding = 1;
            ArrayList<InternalUnit> miners = units[RobotType.MINER.ordinal()];
            InternalUnit miner = miners.get(r.nextInt(miners.size()));
            writeMessage(Message.build(RobotType.FULFILLMENT_CENTER, miner.id));
            addMessageToQueue();
        }


		if (builtMiners < 4) {
			for (Direction dir : directions) {
				if (builtMiners < 8 && rc.canBuildRobot(RobotType.MINER, dir)) {
					rc.buildRobot(RobotType.MINER, dir);
					builtMiners++;
				}
			}
		}

		attackTimer--;
		if(attackTimer > 200 && units[7 /*drone*/].size() >= 7){
			attackTimer = 200;
		}
		if(attackTimer <= 0){
			attackTimer = 200;
			writeMessage(Message.droneAttack());
			addMessageToQueue();
			System.out.println("Called attack");
		}


		//Broadcast important info every 9 rounds
		if(rc.getRoundNum() %9 == 0){
			if(enemyHQ != null){
				writeMessage(Message.enemyHqLocation(enemyHQ));
			}
			writeMessage(Message.hqLocation(rc.getLocation()));
			addMessageToQueue();
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
				units[unitType.ordinal()].add(new InternalUnit(unitType, message.data[1], new MapLocation(message.data[2], message.data[3])));
				debug("Added unit " + units[unitType.ordinal()].get(units[unitType.ordinal()].size()-1));
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