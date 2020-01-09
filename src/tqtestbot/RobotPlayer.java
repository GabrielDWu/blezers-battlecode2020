package tqtestbot;
import battlecode.common.*;
import java.util.*;

public strictfp class RobotPlayer {
    static RobotController rc;
    static int turnCount;
    static int birthRound;  //What round was I born on?
    static boolean[] currMessage;
    static int messagePtr;  //What index in currMessage is my "cursor" at?
    static MapLocation locHQ;   //Where is my HQ?
    static ArrayList<MapLocation> locRecord = new ArrayList<MapLocation>();
    static MapLocation dest = new MapLocation(-1, -1);
    static int destStartTime = -1;
    public static void run(RobotController rc) throws GameActionException {
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
    }static Direction[] directions = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
    static int[] PADS = {-1016996230, -110260579, -1608604611, 1994246809, 1665065435, 422836453, 325111185};
    static void runMiner() throws GameActionException {
        locRecord.add(rc.getLocation());
        RobotInfo[] near = rc.senseNearbyRobots(rc.getCurrentSensorRadiusSquared(), Team.A);
        boolean nearHQ = false;
        for(RobotInfo x: near){
            if(x.getType() == RobotType.HQ && rc.getLocation().distanceSquaredTo(x.getLocation())<10){
                nearHQ = true;
                break;
            }
        }
        if(dest.x == -1 && nearHQ == false){
            setDest(new MapLocation(7, 6));
        }
        if(dest.x != -1 && nearHQ == false){
            Direction dir = moveDest();
            if(dir == Direction.CENTER){
                setDest(new MapLocation(-1, -1));
            }
            else{
                rc.move(dir);
            }
        }
        for (Direction dir : directions)
            if (tryDeliver(dir))
                System.out.println("I delivered soup!");

        for (Direction dir : directions)
            if (tryMine(dir))
                System.out.println("I mined soup! " + rc.getSoupCarrying());
    }
    /**
     * Attempts to mine soup in a given direction.
     *
     * @param dir The intended direction of mining
     * @return true if a move was performed
     * @throws GameActionException
     */
    void conv(Action a) throws GameActionException{
        if(a == null || a.type<0) return;
        if(a.type == 0 && a.dir == Direction.CENTER) return;
        if(a.type == 0) rc.move(a.dir);
        if(a.type == 1) rc.buildRobot(a.unit, a.dir);
        if(a.type == 2) rc.mineSoup(a.dir);
        if(a.type == 3) rc.depositSoup(a.dir, a.soup);
        if(a.type == 4) rc.digDirt(a.dir);
        if(a.type == 5) rc.depositDirt(a.dir);
        if(a.type == 6) rc.pickUpUnit(a.id);
        if(a.type == 7) rc.dropUnit(a.dir);
        if(a.type == 8) rc.shootUnit(a.id);

    }
    static Direction moveDest(){
        if(dest.x != -1 && !dest.equals(rc.getLocation())){
            MapLocation curLoc = rc.getLocation();
            int best = Integer.MAX_VALUE;
            Direction go = directions[0];
            for(Direction dir: directions){
                if(rc.canMove(dir)){
                    MapLocation nxt = curLoc.add(dir);
                    boolean ok = true;
                    for(int i = destStartTime; i<locRecord.size(); i++) {

                        if (locRecord.get(i).equals(nxt)) {
                            ok = false;
                            break;
                        }
                    }
                    
                    if(ok && nxt.distanceSquaredTo(dest) < best){
                        best = nxt.distanceSquaredTo(dest);
                        go = dir;
                    }
                }
            }
            if(rc.canMove(go)){
                return go;
            }
        }
        return Direction.CENTER;
    }
    static void setDest(MapLocation _dest){
        if(_dest.equals(rc.getLocation())) return;
        dest = _dest;
        destStartTime = locRecord.size();
    }
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
    }static void runLandscaper() throws GameActionException {

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
        if (builtMiners < 2) {
            for (Direction dir : directions) {
                if (rc.canBuildRobot(RobotType.MINER, dir)) {
                    rc.buildRobot(RobotType.MINER, dir);
                    builtMiners++;
                }
            }
        }
    }

}
