package blezerbot.buildings;

import battlecode.common.*;
import java.util.*;
import blezerbot.*;

import static blezerbot.Message.MessageType;

public class HQ extends Building {

	int builtMiners;
	boolean hq_sentLoc;
	public ArrayList<InternalUnit>[] units;
	int waitingForBuilding;

	public HQ(RobotController rc)throws GameActionException {
		super(rc);
	}

	public void startLife() throws GameActionException{
		super.startLife();
		waitingForBuilding = Integer.MAX_VALUE;
		units = new ArrayList[10];
		for(int i=0; i<10; i++){
			units[i] = new ArrayList<InternalUnit>();
		}
	}

	public void run() throws GameActionException {
		super.run();
		if (waitingForBuilding < Integer.MAX_VALUE) waitingForBuilding++;
		if(!hq_sentLoc){
			writeMessage(Message.hqLocation(rc.getLocation()));
			addMessageToQueue();
			hq_sentLoc = true;
		}

		if (units[2 /*refinery*/].size() == 0 && units[1].size() > 0 /*miner*/ &&
				waitingForBuilding > 4 && rc.getTeamSoup() > 150) {
			waitingForBuilding = 1;
			ArrayList<InternalUnit> miners = units[1];
			writeMessage(Message.build(RobotType.REFINERY, miners.get(r.nextInt(miners.size())).id));
			addMessageToQueue();
		}

		if (units[5 /*fulfillment center*/].size() == 0 && units[1].size() > 0 /*miner*/ &&
				waitingForBuilding > 51 && rc.getTeamSoup() > 150) {
			waitingForBuilding = 1;
			ArrayList<InternalUnit> miners = units[1];
			//Currently assumes HQ is not on edge/corner
			writeMessage(Message.build(RobotType.FULFILLMENT_CENTER, miners.get(r.nextInt(miners.size())).id, rc.getLocation().translate(1, 1)));
			addMessageToQueue();
		}

		if (units[4 /*design school*/].size() == 0 &&  units[1].size() > 0 /*miner*/ &&
				waitingForBuilding > 51 && rc.getTeamSoup() > 150) {
			waitingForBuilding = 1;
			ArrayList<InternalUnit> miners = units[1];
			//Currently assumes HQ is not on edge/corner
			writeMessage(Message.build(RobotType.DESIGN_SCHOOL, miners.get(r.nextInt(miners.size())).id, rc.getLocation().translate(-1, -1)));
			addMessageToQueue();
		}

		//Shoot enemy drones
		for(RobotInfo enemy: rc.senseNearbyRobots(-1, (rc.getTeam() == Team.B)?Team.A:Team.B)){
			if(enemy.type == RobotType.DELIVERY_DRONE){
				if(rc.canShootUnit(enemy.ID)){
					rc.shootUnit(enemy.ID);
					break;
				}
			}
		}
		if (builtMiners < 4) {
			for (Direction dir : directions) {
				if (rc.canBuildRobot(RobotType.MINER, dir)) {
					rc.buildRobot(RobotType.MINER, dir);
					builtMiners++;
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