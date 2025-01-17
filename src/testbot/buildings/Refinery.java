package testbot.buildings;

import battlecode.common.*;
import java.util.*;
import testbot.*;

public class Refinery extends Building {


    public Refinery(RobotController rc) throws GameActionException {
        super(rc);
    }

    public void run() throws GameActionException {
        super.run();
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
    			if(unit_type != RobotType.MINER){
    				return false;
    			}
    			writeMessage(Message.refineryLocation(rc.getLocation(), message.data[1]));
    			return true;
    	}
    	return false;
    }

}