package blezerbot.buildings;

import battlecode.common.*;
import java.util.*;
import blezerbot.*;

public class HQ extends Building {

	int builtMiners;
	boolean hq_sentLoc;

	public HQ(RobotController rc)throws GameActionException {
		super(rc);
	}

	public void init() {
		units = new ArrayList<ArrayList<InternalUnit> >(10);
		for(int i=0; i<10; i++){
			units.add(i,new ArrayList<InternalUnit>());
		}
	}

	public void run() throws GameActionException {
		if(!hq_sentLoc){
			writeMessage(0, new int[]{rc.getLocation().x, rc.getLocation().y});
			addMessageToQueue();
			hq_sentLoc = true;
		}
		if (builtMiners < 4) {
			for (Direction dir : directions) {
				if (rc.canBuildRobot(RobotType.MINER, dir)) {
					rc.buildRobot(RobotType.MINER, dir);
					builtMiners++;
				}
			}
		}
	}

}