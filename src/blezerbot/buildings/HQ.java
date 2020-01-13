package blezerbot.buildings;

import battlecode.common.*;
import java.util.*;
import blezerbot.*;

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
			writeMessage(0, new int[]{rc.getLocation().x, rc.getLocation().y});
			addMessageToQueue();
			hq_sentLoc = true;
		}

		if (units[2 /*refinery*/].size() == 0 && units[1].size() > 0 /*miner*/ &&
				waitingForBuilding > 4 && rc.getTeamSoup() > 150) {
			waitingForBuilding = 1;
			ArrayList<InternalUnit> miners = units[1];
			writeMessage(3, new int[]{2, miners.get(r.nextInt(miners.size())).id});
			addMessageToQueue();
			System.out.println("SENT");
		}

		if (units[4 /*design school*/].size() == 0 &&  units[1].size() > 0 /*miner*/ &&
				waitingForBuilding > 4 && rc.getTeamSoup() > 150) {
			waitingForBuilding = 1;
			ArrayList<InternalUnit> miners = units[1];
			writeMessage(3, new int[]{4, miners.get(r.nextInt(miners.size())).id});
			addMessageToQueue();
		}

		if (units[5 /*fulfillment center*/].size() == 0 && units[1].size() > 0 /*miner*/ &&
				waitingForBuilding > 4 && rc.getTeamSoup() > 150) {
			waitingForBuilding = 1;
			ArrayList<InternalUnit> miners = units[1];
			writeMessage(3, new int[]{5, miners.get(r.nextInt(miners.size())).id});
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
				writeMessage(2, new int[]{enemyHQ.x, enemyHQ.y});
			}
			writeMessage(0, new int[]{rc.getLocation().x, rc.getLocation().y});
			addMessageToQueue();
		}

	}

	public boolean executeMessage(int id, int[] m, int ptr){
		/*Returns true if message applies to me*/
		if(super.executeMessage(id, m, ptr)){
			return true;
		}
		//HQ specific methods:
		if(id==1){
			RobotType unit_type = robot_types[getInt(m, ptr, 4)];
			ptr += 4;
			MapLocation loc = new MapLocation(getInt(m, ptr, 6), getInt(m, ptr+6, 6));
			ptr += 12;
			int unit_id = getInt(m, ptr, 15);
			units[unit_type.ordinal()].add(new InternalUnit(unit_type, unit_id, loc));
			System.out.println("Added unit " + new InternalUnit(unit_type,unit_id, loc));
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