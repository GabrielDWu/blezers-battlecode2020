/*
DO NOT EDIT THIS FILE DO NOT EDIT THIS FILE
DO NOT EDIT THIS FILE DO NOT EDIT THIS FILE
DO NOT EDIT THIS FILE DO NOT EDIT THIS FILE
DO NOT EDIT THIS FILE DO NOT EDIT THIS FILE
DO NOT EDIT THIS FILE DO NOT EDIT THIS FILE
DO NOT EDIT THIS FILE DO NOT EDIT THIS FILE
DO NOT EDIT THIS FILE DO NOT EDIT THIS FILE
DO NOT EDIT THIS FILE DO NOT EDIT THIS FILE
DO NOT EDIT THIS FILE DO NOT EDIT THIS FILE
DO NOT EDIT THIS FILE DO NOT EDIT THIS FILE
DO NOT EDIT THIS FILE DO NOT EDIT THIS FILE
DO NOT EDIT THIS FILE DO NOT EDIT THIS FILE
DO NOT EDIT THIS FILE DO NOT EDIT THIS FILE
DO NOT EDIT THIS FILE DO NOT EDIT THIS FILE
DO NOT EDIT THIS FILE DO NOT EDIT THIS FILE
DO NOT EDIT THIS FILE DO NOT EDIT THIS FILE
DO NOT EDIT THIS FILE DO NOT EDIT THIS FILE
*/

package blezerbot;
import battlecode.common.*;

public strictfp class RobotPlayer {
    static RobotController rc;
    static int turnCount;
    public static void run(RobotController rc) throws GameActionException {
        RobotPlayer.rc = rc;
        while (true) {
            turnCount += 1;
            try {
                switch (rc.getType()) { case MINER: runMiner();break;
case LANDSCAPER: runLandscaper();break;
case DELIVERY_DRONE: runDeliveryDrone();break;
case REFINERY: runRefinery();break;
case VAPORATOR: runVaporator();break;
case DESIGN_SCHOOL: runDesignSchool();break;
case FULFILLMENT_CENTER: runFulfillmentCenter();break;
case NET_GUN: runNetGun();break;
case HQ: runHq();break;
 }
                Clock.yield();
            } catch (Exception e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            }
        }
    }
    static Direction[] directions = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
static void runMiner() throws GameActionException {

}
static void runLandscaper() throws GameActionException {

}
static void runDeliveryDrone() throws GameActionException {

}
static void runRefinery() throws GameActionException {

}
static void runVaporator() throws GameActionException {

}
static void runDesignSchool() throws GameActionException {

}
static void runFulfillmentCenter() throws GameActionException {

}
static void runNetGun() throws GameActionException {

}
static int builtMiners;

static void runHq() throws GameActionException {
	if (builtMiners < 1) {
		for (Direction dir : directions) {
			if (rc.canBuildRobot(RobotType.MINER, dir)) {
				rc.buildRobot(RobotType.MINER, dir);
				builtMiners++;
			}
		}
	}
}

}
