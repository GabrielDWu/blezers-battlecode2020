//DO NOT EDIT THIS FILE: YOUR CHANGES WILL SIMPLY BE OVERWRITTEN
//DO NOT EDIT THIS FILE: YOUR CHANGES WILL SIMPLY BE OVERWRITTEN
//DO NOT EDIT THIS FILE: YOUR CHANGES WILL SIMPLY BE OVERWRITTEN
//DO NOT EDIT THIS FILE: YOUR CHANGES WILL SIMPLY BE OVERWRITTEN
//DO NOT EDIT THIS FILE: YOUR CHANGES WILL SIMPLY BE OVERWRITTEN
//DO NOT EDIT THIS FILE: YOUR CHANGES WILL SIMPLY BE OVERWRITTEN
//DO NOT EDIT THIS FILE: YOUR CHANGES WILL SIMPLY BE OVERWRITTEN
//DO NOT EDIT THIS FILE: YOUR CHANGES WILL SIMPLY BE OVERWRITTEN
//DO NOT EDIT THIS FILE: YOUR CHANGES WILL SIMPLY BE OVERWRITTEN
//DO NOT EDIT THIS FILE: YOUR CHANGES WILL SIMPLY BE OVERWRITTEN
//DO NOT EDIT THIS FILE: YOUR CHANGES WILL SIMPLY BE OVERWRITTEN
//DO NOT EDIT THIS FILE: YOUR CHANGES WILL SIMPLY BE OVERWRITTEN
//DO NOT EDIT THIS FILE: YOUR CHANGES WILL SIMPLY BE OVERWRITTEN
//DO NOT EDIT THIS FILE: YOUR CHANGES WILL SIMPLY BE OVERWRITTEN
//DO NOT EDIT THIS FILE: YOUR CHANGES WILL SIMPLY BE OVERWRITTEN
//DO NOT EDIT THIS FILE: YOUR CHANGES WILL SIMPLY BE OVERWRITTEN
//DO NOT EDIT THIS FILE: YOUR CHANGES WILL SIMPLY BE OVERWRITTEN
//DO NOT EDIT THIS FILE: YOUR CHANGES WILL SIMPLY BE OVERWRITTEN
//DO NOT EDIT THIS FILE: YOUR CHANGES WILL SIMPLY BE OVERWRITTEN
//DO NOT EDIT THIS FILE: YOUR CHANGES WILL SIMPLY BE OVERWRITTEN
package blezerbot;
import battlecode.common.*;
import java.util.*;

public strictfp class RobotPlayer {
    static RobotController rc;
    static int turnCount;
    static int birthRound;  //What round was I born on?
    static boolean[] currMessage;
    static int messagePtr;  //What index in currMessage is my "cursor" at?
    static MapLocation locHQ;   //Where is my HQ?
    public static void run(RobotController rc) throws GameActionException {
        if (seen == null) seen = new HashSet<MapLocation>();
        RobotPlayer.rc = rc;

        birthRound = rc.getRoundNum();
        resetMessage();
        while (true) {
            turnCount += 1;

            //process all messages for the previous round
            if(rc.getRoundNum() > 1) {
                for (Transaction t : rc.getBlock(rc.getRoundNum() - 1)) {
                    processMessage(t.getMessage());
                }
            }

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
    static void processMessage(int[] message) {
        //Check if the message was made by my team
        //The way it works: xor all of them with PAD
        //Then convert into 224 bits and do a 0-checksum with 8 blocks of 28 bits.
        System.out.println("Message Being Processed");
        int[] m = new int[7];
        for(int i=0; i<7; i++){
            m[i] = message[i]^PADS[i];
        }

        boolean bits[] = new boolean[224];
        boolean checksum[] = new boolean[28];
        int ptr = 0;    //This is a local ptr for reading a message, different than messagePtr (which is for writing)
        for(int i=0; i<message.length; i++){
            for(int j=31; j>=0; j--){
                bits[ptr] = 1==((m[i] >> j)&1);
                checksum[ptr%28] ^= bits[ptr];
                ptr++;
            }
        }
        int res = 0;
        for(int i=0; i<28; i++){
            if(checksum[i]){
                res ++;
            }
        }

        if (res != 0) { //Checksum failed, message made for the enemy
            //May want to store enemy messages here to find patterns to spread misinformation... ;)
            return;
        }

        ptr = 0;
        while(ptr <= 191){   //195-4
            int id = getInt(bits, ptr, 4);
            ptr += 4;
            if(id==0){ //0000 Set our HQ
                if(ptr >= 184){ //Requires 2 6-bit integers
                    System.out.println("Message did not exit properly");
                    return;
                }
                int x = getInt(bits, ptr, 6);
                if(x==0)x=64;
                ptr += 6;
                int y = getInt(bits, ptr, 6);
                if(y==0)x=64;
                ptr += 6;
                locHQ = new MapLocation(x,y);
                System.out.println("Now I know that my HQ is at" + locHQ);
            }else if(id==15){    //1111 Message terminate
                return;
            }
        }
        System.out.println("Message did not exit properly");  //Should've seen 1111.
        return;
}

static int getInt(boolean[] bits, int ptr, int size){
        /*Turns the next <size> bits into an integer from 0 to 2**size-1. Does not modify ptr.*/
        int x = 0;
        for(int i=0; i<size; i++){
            x *= 2;
            if(bits[ptr+i]) x++;
        }
        return x;
}

static void writeInt(int x, int size){
        /*Writes the next <size> bits of currMessage with an integer 0 to 2**size-1. Modifies messagePtr.*/
        for(int i=size-1; i>=0; i--){
            currMessage[messagePtr] = 1==((x>>i)&1);
            messagePtr++;
        }
        return;
}

static void resetMessage(){
    //Resets currMessage to all 0's, and messagePtr to 0.
        messagePtr = 0;
        currMessage = new boolean[224];
        return;
}

static void writeMessage(int id, int[] params){
    /*Writes a command into currMessage. Will not do anything if it does not leave 4 bits for message end
      and the 28 bit checksum. This means it can only write up to (but not including) bit 192 (index 191).
     */
        if(id==0){ //0000 Set our HQ
            if(messagePtr >= 176){ //Requires id + 2 6-bit integers
                System.out.println("Message Overflow");
                return;
            }
            writeInt(id, 4);
            writeInt(params[0], 6);
            writeInt(params[1], 6);
        }
        return;
}

static void sendMessage(int wager) throws GameActionException{
    /*Does the following
        Writes the 1111 message end
        Sets the last 28 bits to meet the checksum
        Condenses it into 7 32-bit integers
        Applies the pad
        Sends the transaction
        resetMessage();
     */
        writeInt(15, 4);

        boolean checksum[] = new boolean[28];
        for(int i=0; i<196; i++){
            checksum[i%28] ^= currMessage[i];
        }
        for(int i=0; i<28; i++){
            currMessage[i+196] = checksum[i];
        }

        int[] words = new int[7];
        int ptr = 0;
        for(int i=0; i<7; i++){
            for(int j=0; j<32; j++){
                words[i] <<= 1;
                if(currMessage[ptr]){
                    words[i]++;
                }
                ptr++;
            }
        }

        for(int i=0; i<7; i++){
            words[i] ^= PADS[i];
        }
        rc.submitTransaction(words, wager);
        resetMessage();
        return;
}static Direction[] directions = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.NORTHEAST, Direction.NORTHWEST, Direction.SOUTHEAST, Direction.SOUTHWEST};
static int[] PADS = {-1016996230, -110260579, -1608604611, 1994246809, 1665065435, 422836453, 325111185};static Direction randomDirection() {
    return directions[(int) (Math.random() * directions.length)];
}

static Direction nextDir(Direction dir) {
	if (dir.equals(directions[0])) return directions[1];
	if (dir.equals(directions[1])) return directions[2];
	if (dir.equals(directions[2])) return directions[3];
	if (dir.equals(directions[3])) return directions[0];
	return null;
}

static boolean tryMove(Direction dir) throws GameActionException {
    if (rc.isReady() && !rc.senseFlooding(rc.adjacentLocation(dir)) && rc.canMove(dir)) {
        rc.move(dir);
        return true;
    } else return false;
}

static HashSet<MapLocation> seen;
static boolean soupSearching = false;
static boolean returning = false;

static void runMiner() throws GameActionException {
    MapLocation myloc = rc.getLocation();
    for (int i = -3; i <= 3; i++) {
        seen.add(new MapLocation(myloc.x+i, myloc.y+5));
        seen.add(new MapLocation(myloc.x+i, myloc.y-5));
    }
    for (int i = -4; i <= 4; i++) {
        seen.add(new MapLocation(myloc.x+i, myloc.y+4));
        seen.add(new MapLocation(myloc.x+i, myloc.y-4));
    }
    for (int j = -3; j <= 3; j++) {
        for (int i = -5; i <= 5; i++) {
            seen.add(new MapLocation(myloc.x+i, myloc.y+j));
        }
    }
    boolean mined = false;
    for (Direction dir : directions)
            if (tryMine(dir)) {
                System.out.println("I mined soup! " + rc.getSoupCarrying());
                mined = true;
                returning = true;
                soupSearching = false;
            }
    if (!mined && !returning) {
        findSoup();
    }
}

static void findSoup() throws GameActionException {
    if (!soupSearching) {
        soupSearching = true;
    }
    ArrayList<Integer> newSeenList = new ArrayList<Integer>();
    ArrayList<Direction> newSeenDirs = new ArrayList<Direction>();
    for (Direction dir : directions) {
        newSeenList.add(newVisibleMiner(rc.getLocation(), dir));
        newSeenDirs.add(dir);
    }
    Random r = new Random();
    Direction maxl = null;
    while (maxl == null || !tryMove(maxl)) {
        ArrayList<Integer> newNewSeenList = (ArrayList<Integer>)newSeenList.clone();
        ArrayList<Direction> newNewSeenDirs = (ArrayList<Direction>)newSeenDirs.clone();
        int max = -1;
        while (newNewSeenList.size() > 0) {
            int ri = r.nextInt(newNewSeenList.size());
            if (newNewSeenList.remove(ri) > max) maxl = newNewSeenDirs.remove(ri); 
        }
    }
}


static int[][] aNewVisibleMiner= new int[][]{{6,0},{6,1},{6,-1},{6,2},{6,-2},{6,3},{6,-3},{5,4},{5,-4}};
static int newVisibleMiner(MapLocation loc, Direction dir) {
    int visible = 0;
    for (int i = 0; i < aNewVisibleMiner.length; i++) {
        int x = loc.x;
        int y = loc.y;
        if (dir.dx != 0) {
            x += aNewVisibleMiner[i][0]*dir.dx;
            y += aNewVisibleMiner[i][1];
        }
        if (dir.dy != 0) {
            x += aNewVisibleMiner[i][0]*dir.dy;
            y += aNewVisibleMiner[i][1];
        }
        if (!seen.contains(new MapLocation(x, y))) visible++;
    }
    return visible;
}

/**
 * Attempts to mine soup in a given direction.
 *
 * @param dir The intended direction of mining
 * @return true if a move was performed
 * @throws GameActionException
 */
static boolean tryMine(Direction dir) throws GameActionException {
    if (rc.isReady() && rc.canMineSoup(dir)) {
        rc.mineSoup(dir);
        return true;
    } else return false;
}

/**
 * Attempts to deliver soup in a given direction.
 *
 * @param dir The intended direction of refining
 * @return true if a move was performed
 * @throws GameActionException
 */
static boolean tryDeliver(Direction dir) throws GameActionException {
    if (rc.isReady() && rc.canDepositSoup(dir)) {
        rc.depositSoup(dir, rc.getSoupCarrying());
        return true;
    } else return false;
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
static boolean hq_sentLoc;

static void runHq() throws GameActionException {
	if(!hq_sentLoc){
		writeMessage(0, new int[]{rc.getLocation().x, rc.getLocation().y});
		sendMessage(5);
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
