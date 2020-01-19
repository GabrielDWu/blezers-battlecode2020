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
	Direction waitDir;
	int waitingForBuilding;
	int minerIdToRemove;
	boolean minerWalled;
	boolean builtDesignSchool;
	boolean builtRefinery;
	Direction turtleDesignSchoolDir;
	boolean landscaperWalled;
	int buildingDesignSchool;

	public HQ(RobotController rc)throws GameActionException {
		super(rc);
	}

	public void startLife() throws GameActionException{
		super.startLife();
		waitingForBuilding = Integer.MAX_VALUE;
		minerIdToRemove = -1;
		units = new ArrayList[10];
		for(int i=0; i<10; i++){
			units[i] = new ArrayList<InternalUnit>();
		}
	}

	public void run() throws GameActionException {
		super.run();
		if (!builtDesignSchool) {
			if (buildingDesignSchool > 11) builtDesignSchool = true;
			else if (buildingDesignSchool > 0) buildingDesignSchool++;
		}
		if (units[RobotType.LANDSCAPER.ordinal()].size() >= 8) {
			landscaperWalled = true;
			writeMessage(Message.buildWall(rc.getLocation()));
			addMessageToQueue();
		}
		if (minerIdToRemove != -1) {
			for(Iterator<InternalUnit> iterator = units[RobotType.MINER.ordinal()].iterator(); iterator.hasNext(); ) {
			    if(iterator.next().id == minerIdToRemove) {
			        iterator.remove();
			        break;
			    }
			}
			minerIdToRemove = -1;
		}
		if (minerWalled && buildingDesignSchool == 0 && builtRefinery) {
			for (Direction dir : new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
				MapLocation minerLoc = rc.getLocation().add(dir);
				MapLocation buildLoc = minerLoc.add(dir);
				if (rc.onTheMap(buildLoc) && Math.abs(rc.senseElevation(minerLoc)-rc.senseElevation(buildLoc)) <= GameConstants.MAX_DIRT_DIFFERENCE && !rc.senseFlooding(buildLoc)) {
					writeMessage(Message.build(RobotType.DESIGN_SCHOOL, rc.senseRobotAtLocation(minerLoc).getID(), buildLoc));
					addMessageToQueue();
					buildingDesignSchool = 1;
					turtleDesignSchoolDir = dir;
					break;
				}
			}
		}
		MapLocation mloc = rc.getLocation();
		if (builtDesignSchool && units[RobotType.DESIGN_SCHOOL.ordinal()].size() > 0 && !landscaperWalled && rc.isLocationOccupied(mloc.add(turtleDesignSchoolDir.rotateLeft())) && rc.isLocationOccupied(mloc.add(turtleDesignSchoolDir)) && rc.isLocationOccupied(mloc.add(turtleDesignSchoolDir.rotateRight()))) {
			if (rc.getTeamSoup() > 150) {
				MapLocation[] unwaitLocs = new MapLocation[]{mloc.add(turtleDesignSchoolDir.opposite().rotateRight()), mloc.add(turtleDesignSchoolDir.opposite().rotateLeft()), mloc.add(turtleDesignSchoolDir.opposite())};
				RobotInfo unwaitMiner;
				for (int i = 0; i < unwaitLocs.length; i++) {
					unwaitMiner = rc.senseRobotAtLocation(unwaitLocs[i]);
					if (unwaitMiner == null || unwaitMiner.getType() != RobotType.MINER) continue;
					writeMessage(Message.doSomething(unwaitMiner.getID()));
					addMessageToQueue();
					units[unwaitMiner.getType().ordinal()].add(new InternalUnit(unwaitMiner.getType(), unwaitMiner.getID(), unwaitMiner.getLocation()));
					break;
				}
			}
		}
		if (waitingForBuilding < Integer.MAX_VALUE) waitingForBuilding++;
		if(!hq_sentLoc){
			writeMessage(Message.hqLocation(rc.getLocation()));
			addMessageToQueue();
			hq_sentLoc = true;
		}

		if (minerWalled && units[2 /*refinery*/].size() == 0 && units[1].size() > 0 /*miner*/ && waitingForBuilding > 4 && rc.getTeamSoup() > 200) {
			waitingForBuilding = 1;
			ArrayList<InternalUnit> miners = units[1];
			InternalUnit miner = miners.get(r.nextInt(miners.size()));
			writeMessage(Message.build(RobotType.REFINERY, miner.id));
			addMessageToQueue();
			builtRefinery = true;
		}

		// if (units[5 /*fulfillment center*/].size() == 0 && units[1].size() > 0 /*miner*/ &&
		// 		waitingForBuilding > 51 && rc.getTeamSoup() > 150) {
		// 	waitingForBuilding = 1;
		// 	ArrayList<InternalUnit> miners = units[1];
		// 	//Currently assumes HQ is not on edge/corner
		// 	InternalUnit miner = miners.get(r.nextInt(miners.size()));
		// 	while (rc.canSenseRobot(miner.id)) miner = miners.get(r.nextInt(miners.size()));
		// 	writeMessage(Message.build(RobotType.FULFILLMENT_CENTER, miner.id));
		// 	addMessageToQueue();
		// }

		//Shoot enemy drones
		for(RobotInfo enemy: rc.senseNearbyRobots(-1, (rc.getTeam() == Team.B)?Team.A:Team.B)){
			if(enemy.type == RobotType.DELIVERY_DRONE){
				if(rc.canShootUnit(enemy.ID)){
					rc.shootUnit(enemy.ID);
					break;
				}
			}
		}

		if (waitDir != null) {
			int waitId = rc.senseRobotAtLocation(rc.getLocation().add(waitDir)).getID();
			minerIdToRemove = waitId;
			writeMessage(Message.doNothing(waitId));
			addMessageToQueue();
			waitDir = null;
		}

		if (builtMiners < 5) {
			for (Direction dir : directions) {
				if (builtMiners < 8 && rc.canBuildRobot(RobotType.MINER, dir)) {
					rc.buildRobot(RobotType.MINER, dir);
					builtMiners++;
				}
			}
		} else if (rc.getTeamSoup() >= 270 && !minerWalled) {
			minerWalled = true;
			for (Direction dir : directions) {
				MapLocation nloc = mloc.add(dir);
				if (rc.canSenseLocation(nloc) && !rc.isLocationOccupied(nloc) && Math.abs(rc.senseElevation(mloc)-rc.senseElevation(nloc)) <= GameConstants.MAX_DIRT_DIFFERENCE && !rc.senseFlooding(nloc)) {
					minerWalled = false;
					if (rc.canBuildRobot(RobotType.MINER, dir)) {
						rc.buildRobot(RobotType.MINER, dir);
						waitDir = dir;
						break;
					}
				}
			}
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