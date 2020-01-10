package blezerbot;

import battlecode.common.*;
import java.util.*;
import java.lang.Math;

public abstract class Robot {

	public RobotController rc;
	public int turnCount;
	public int birthRound;  //What round was I born on?
	public int[] currMessage;
	public LinkedList<Transaction> messageQueue = new LinkedList<Transaction>();
	public int messagePtr;  //What index in currMessage is my "cursor" at?
	public MapLocation locHQ;   //Where is my HQ?
	public boolean sentInfo;    //Sent info upon spawn
	public int type;    //Integer from 0 to 8, index of robot_types
	public int base_wager = 2;
	public int enemy_msg_cnt;   //How many enemy messages went through last round?
	public int enemy_msg_sum;   //Total wagers of enemy messages last round.
	public boolean[][] seen;
	public int[][] visited;
	public Random r;
	public Direction[] directions = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.NORTHEAST, Direction.NORTHWEST, Direction.SOUTHEAST, Direction.SOUTHWEST};
	public int[] PADS = {-1016996230, -110260579, -1608604611, 1994246809, 1665065435, 422836453, 325111185};
	public RobotType[] robot_types = {RobotType.HQ, //0
	        RobotType.MINER, //1
	        RobotType.REFINERY, //2
	        RobotType.VAPORATOR, //3
	        RobotType.DESIGN_SCHOOL, //4
	        RobotType.FULFILLMENT_CENTER, //5
	        RobotType.LANDSCAPER, //6
	        RobotType.DELIVERY_DRONE, //7
	        RobotType.NET_GUN //8
	};
	ArrayList<ArrayList<InternalUnit>> units;


	public Robot(RobotController rc) throws GameActionException {
		if (seen == null) seen = new boolean[rc.getMapWidth()][rc.getMapHeight()];
		if (visited == null) visited = new int[rc.getMapWidth()][rc.getMapHeight()];
		if (r == null) r = new Random(rc.getID());
		this.rc = rc;
		startLife();
		while (true) {
		    startTurn();
		    try {
		    	run();
		        endTurn();
		        Clock.yield();
		    } catch (Exception e) {
		        System.out.println(robot_types[type] + " Exception");
		        e.printStackTrace();
		    }
		}
	}

	public abstract void run() throws GameActionException;

	public void init() throws GameActionException {}

	public void startLife() throws GameActionException{
	    System.out.println("Got created.");

	    switch (rc.getType()) {
	        case HQ:                 type=0;init();    break;
	        case MINER:              type=1;    break;
	        case REFINERY:           type=2;    break;
	        case VAPORATOR:          type=3;    break;
	        case DESIGN_SCHOOL:      type=4;    break;
	        case FULFILLMENT_CENTER: type=5;    break;
	        case LANDSCAPER:         type=6;    break;
	        case DELIVERY_DRONE:     type=7;    break;
	        case NET_GUN:            type=8;    break;
	    }

	    //process all messages from beginning of game until you find hq location
	    int checkRound = 1;
	    while (checkRound < rc.getRoundNum()-1 && locHQ == null) {
	        for (Transaction t : rc.getBlock(checkRound)){
	            processMessage(t);
	            if(locHQ != null){
	                break;
	            }
	        }
	        checkRound++;
	    }
	    birthRound = rc.getRoundNum();
	    resetMessage();
	}

	public void startTurn() throws GameActionException{
	    //if(rc.getRoundNum() >= 10){rc.resign();}
	    turnCount = rc.getRoundNum()-birthRound+1;

	    //process all messages for the previous round
	    if(rc.getRoundNum() > 1) {
	        enemy_msg_cnt = 0;
	        enemy_msg_sum = 0;
	        for (Transaction t : rc.getBlock(rc.getRoundNum() - 1)){
	            processMessage(t);
	        }
	        if(enemy_msg_cnt > 0){
	            base_wager = ((enemy_msg_sum/enemy_msg_cnt + 1) + base_wager)/2;
	        }else{
	            base_wager *= .8;
	        }
	        base_wager = Math.max(base_wager, 1);
	    }

	    if(!sentInfo){
	        writeMessage(1, new int[]{type, rc.getID()});
	        addMessageToQueue();
	        sentInfo = true;
	    }
	}

	public void endTurn() throws GameActionException{
	    /*submits stuff from messageQueue*/
	    while(messageQueue.size() > 0 && messageQueue.get(0).getCost() <= rc.getTeamSoup()){
	        rc.submitTransaction(messageQueue.get(0).getMessage(), messageQueue.get(0).getCost());
	        messageQueue.remove(0);
	    }
	}

	public boolean onMap(MapLocation l) {
		return !(l.x < 0 || l.x >= rc.getMapWidth() || l.y < 0 || l.y >= rc.getMapHeight());
	}

	public boolean onMap(int x, int y) {
		return x >= 0 && x < rc.getMapWidth() && y >= 0 && y < rc.getMapHeight();
	}

	public Direction randomDirection() {
	    return directions[(int) (Math.random() * directions.length)];
	}

	public Direction nextDir(Direction dir) {
		if (dir.equals(directions[0])) return directions[1];
		if (dir.equals(directions[1])) return directions[2];
		if (dir.equals(directions[2])) return directions[3];
		if (dir.equals(directions[3])) return directions[0];
		return null;
	}

	public boolean tryMove(Direction dir) throws GameActionException {
	    if (rc.isReady() && rc.canMove(dir) && !rc.senseFlooding(rc.adjacentLocation(dir))) {
	        rc.move(dir);
	        return true;
	    } else return false;
	}

	/***** BLOCKCHAIN ******/
	/***** BLOCKCHAIN ******/
	/***** BLOCKCHAIN ******/
	/***** BLOCKCHAIN ******/
	/***** BLOCKCHAIN ******/
	public void processMessage(Transaction t) {
	    //Check if the message was made by my team
	    //The way it works: xor all of them with PAD
	    //Then convert into 224 bits and do a 0-checksum with 8 blocks of 28 bits.
	    int[] message = t.getMessage();

	    int[] m = new int[7];
	    for(int i=0; i<7; i++){
	        m[i] = message[i]^PADS[i];
	    }

	    int res = (((m[0] >>> 4) ^ (m[0] << 24) ^ (m[1] >>> 8) ^ (m[1] << 20) ^ (m[2] >>> 12) ^ (m[2] << 16) ^
	        (m[3] >>> 16) ^ (m[3] << 12) ^ (m[4] >>> 20) ^ (m[4] << 8) ^ (m[5] >>> 24) ^ (m[5] << 4) ^
	        (m[6] >>> 28) ^ (m[6]))<<4)>>>4;

	    if (res != 0) { //Checksum failed, message made for the enemy
	        enemy_msg_cnt++;
	        enemy_msg_sum += t.getCost();
	        //May want to store enemy messages here to find patterns to spread misinformation... ;)
	        return;
	    }

	    int ptr = 0;
	    while(ptr <= 191){   //195-4
	        int id = getInt(m, ptr, 4);
	        ptr += 4;
	        if(id==0){ //0000 Set our HQ
	            if(ptr >= 184){ //Requires 2 6-bit integers
	                System.out.println("Message did not exit properly");
	                return;
	            }
	            int x = getInt(m, ptr, 6);
	            if(x==0)x=64;
	            ptr += 6;
	            int y = getInt(m, ptr, 6);
	            if(y==0)x=64;
	            ptr += 6;
	            locHQ = new MapLocation(x,y);
	            System.out.println("Now I know that my HQ is at" + locHQ);
	        }else if(id==1){
	            if(ptr >= 177){
	                System.out.println("Message did not exit properly");
	                return;
	            }
	            if(type == 0){//Only HQ keeps track of other units
	                int unit_type = getInt(m, ptr, 4);
	                ptr += 4;
	                int unit_id = getInt(m, ptr, 15);
	                ptr += 15;
	                units.get(unit_type).add(new InternalUnit(unit_type, unit_id));
	                System.out.println("Added unit" + new InternalUnit(unit_type,unit_id));
	            }else{
	                ptr += 19;
	            }
	        }
	        else if(id==15){    //1111 Message terminate
	            return;
	        }
	    }
	    System.out.println("Message did not exit properly");  //Should've seen 1111.
	    return;
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

	public int getInt(int[] m, int ptr, int size){
	    /*Turns the next <size> bits into an integer from 0 to 2**size-1. Does not modify ptr.*/
	    assert(size <= 32);
	    if(32-(ptr%32) < size){
	        return ((m[ptr/32]<<(size-(32-(ptr%32)))) + (m[ptr/32+1]>>>(64-size-(ptr%32))))%(1<<size);
	    }else{
	        return (m[ptr/32]>>>(32-(ptr%32)-size))%(1<<size);
	    }
	}

	public void writeInt(int x, int size){
	    /*Writes the next <size> bits of currMessage with an integer 0 to 2**size-1. Modifies messagePtr.*/
	    assert(size <= 32);
	    if(32-(messagePtr%32) < size){
	        currMessage[messagePtr/32] += x >>> (size-(32-(messagePtr%32)));
	        currMessage[messagePtr/32+1] += (x%(1<<(size-(32-(messagePtr%32)))))<<(64-size-(messagePtr%32));
	    }else{
	        currMessage[messagePtr/32] += x << (32-(messagePtr%32)-size);
	    }
	    messagePtr += size;
	    return;
	}

	public void resetMessage(){
	    //Resets currMessage to all 0's, and messagePtr to 0.
	    messagePtr = 0;
	    currMessage = new int[7];;
	    return;
	}

	public void writeMessage(int id, int[] params){
	/*Writes a command into currMessage. Will not do anything if it does not leave 4 bits for message end
	  and the 28 bit checksum. This means it can only write up to (but not including) bit 192 (index 191).
	 */
	    if(id==0){ //0000 Set our HQ
	        if(messagePtr >= 176){ //Requires id + 2 6-bit integers
	            addMessageToQueue(base_wager);
	        }
	        writeInt(id, 4);
	        writeInt(params[0], 6);
	        writeInt(params[1], 6);
	    }if(id==1){ //0000 Set our HQ
	        if(messagePtr >= 169){ //Requires id + 4-bit int + 15-bit int
	            addMessageToQueue(base_wager);
	        }
	        writeInt(id, 4);
	        writeInt(params[0], 4);
	        writeInt(params[1], 15);
	    }
	    return;
	}

	public void addMessageToQueue(){
	    addMessageToQueue(base_wager);
	}

	public void addMessageToQueue(int wager){
	    /*Does the following
	    Writes the 1111 message end
	    Sets the last 28 bits to meet the checksum
	    Applies the pad
	    Adds transaction to messageQueue
	    resetMessage();
	    Returns true if successful
	 */
	    writeInt(15, 4);

	    int res = (((currMessage[0] >>> 4) ^ (currMessage[0] << 24) ^ (currMessage[1] >>> 8) ^ (currMessage[1] << 20) ^
	        (currMessage[2] >>> 12) ^ (currMessage[2] << 16) ^ (currMessage[3] >>> 16) ^ (currMessage[3] << 12) ^
	        (currMessage[4] >>> 20) ^ (currMessage[4] << 8) ^ (currMessage[5] >>> 24) ^ (currMessage[5] << 4) ^
	        (currMessage[6] >>> 28))<<4)>>>4;
	    currMessage[6] += res;

	    for(int i=0; i<7; i++){
	        currMessage[i] ^= PADS[i];
	    }
	    messageQueue.add(new Transaction(wager, currMessage, 1));
	    resetMessage();
	    return;
	}

}