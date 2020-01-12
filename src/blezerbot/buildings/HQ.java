package blezerbot.buildings;

import battlecode.common.*;
import java.util.*;
import blezerbot.*;

public class HQ extends Building {

	int builtMiners;
	boolean hq_sentLoc;
	public ArrayList<ArrayList<InternalUnit>> units;

	public HQ(RobotController rc)throws GameActionException {
		super(rc);
	}

	public void startLife() throws GameActionException{
		super.startLife();
		units = new ArrayList<ArrayList<InternalUnit> >(10);
		for(int i=0; i<10; i++){
			units.add(i,new ArrayList<InternalUnit>());
		}
	}

	public void run() throws GameActionException {
		super.run();
		if(!hq_sentLoc){
			writeMessage(0, new int[]{rc.getLocation().x, rc.getLocation().y});
			addMessageToQueue();
			hq_sentLoc = true;
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
				addMessageToQueue();
			}
		}

	}

	public boolean executeMessage(int id, int[] m, int ptr){
		/*Returns true if message applies to me*/
		if(super.executeMessage(id, m, ptr)){
			return true;
		}
		//HQ specific methods:
		if(id==1){
			int unit_type = getInt(m, ptr, 4);
			ptr += 4;
			int unit_id = getInt(m, ptr, 15);
			units.get(unit_type).add(new InternalUnit(unit_type, unit_id));
			System.out.println("Added unit" + new InternalUnit(unit_type,unit_id));
			return true;
		}
		return false;
	}

	public class InternalUnit{
		/*HQ uses this class to keep track of all of our units.*/
		public int type;
		public int id;
		public MapLocation lastSent;

		public InternalUnit(int t, int id){
			this.type = t;
			this.id = id;
		}

		public String toString(){
			return robot_types[type] + " (" + id + ")";
		}
	}

}