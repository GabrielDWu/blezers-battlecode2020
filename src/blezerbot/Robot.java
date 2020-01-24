package blezerbot;

import battlecode.common.*;
import java.util.*;
import java.lang.Math;
import blezerbot.units.*;
import blezerbot.buildings.*;

import static blezerbot.Message.MessageType;

public abstract class Robot {

	public boolean debugging = true;	//Show debug messages?
	public RobotController rc;
	public int turnCount;
	public int birthRound;  //What round was I born on?
	public int[] currMessage;
	public LinkedList<Transaction> messageQueue = new LinkedList<Transaction>();
	public int messagePtr;  //What index in currMessage is my "cursor" at?
	public MapLocation locHQ;   //Where is my HQ?
	public MapLocation enemyHQ;   //Where is enemy HQ?
	public boolean sentInfo;    //Sent info upon spawn
	public boolean queuedInfo;
	public RobotType type;    //Integer from 0 to 8, index of robot_types
	public int base_wager = 2;
	public int enemy_msg_cnt;   //How many enemy messages went through last round?
	public int enemy_msg_sum;   //Total wagers of enemy messages last round.
	public Random r;
	public Direction facing;
	//public Direction[] directions = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.NORTHEAST, Direction.NORTHWEST, Direction.SOUTHEAST, Direction.SOUTHWEST};
	public Direction[] directions = {Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST, Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST};
	public Direction[] directionswcenter = {Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST, Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST,Direction.CENTER};
	public Direction[] orthogonalDirections = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
	public int[] PADS = {-1016996230, -110260579, -1608604611, 1994246809, 1665065435, 422836453, 325111185};
	public HashSet<Integer> nonces_seen;

	public RobotType[] robot_types = {RobotType.HQ, //0
	        RobotType.MINER, //1
	        RobotType.REFINERY, //2
	        RobotType.VAPORATOR, //3
	        RobotType.DESIGN_SCHOOL, //4
	        RobotType.FULFILLMENT_CENTER, //5
	        RobotType.LANDSCAPER, //6
	        RobotType.DELIVERY_DRONE, //7
	        RobotType.NET_GUN, //8
			RobotType.COW
	};

	public void debug(String s){
		if(debugging) System.out.println(s);
	}
	public int getDirectionValue(Direction dir){
		for(int i = 0; i<8; i++){
			if(directions[i] == dir) return i;
		}
		return -1;
	}
	public Direction directionClockwise(Direction dir){
		int val = getDirectionValue(dir);
		return directions[(val+1)%8];
	}
	public Direction directionCounterClockwise(Direction dir){
		int val = getDirectionValue(dir);
		return directions[(val+7)%8];
	}
	public Robot(RobotController rc) throws GameActionException {
		if (r == null) r = new Random(rc.getID());

		this.rc = rc;
		startLife();
		while (true) {
		    try {
				startTurn();
				if(rc.isReady()){run();}
		        endTurn();
		        Clock.yield();
		    } catch (Exception e) {
		        System.out.println(type + " Exception");
		        e.printStackTrace();
		    }
		}
	}


	public void run() throws GameActionException {
	}

	public void startLife() throws GameActionException{
		if(rc.getTeam() == Team.A) PADS[0] += 1;
	    debug("Got created.");

	    type = rc.getType();

	    birthRound = rc.getRoundNum();
	    nonces_seen = new HashSet<Integer>();
	    resetMessage();
	}

	public void startTurn() throws GameActionException{
	    //if(rc.getRoundNum() >= 20){rc.resign();}
	    turnCount = rc.getRoundNum()-birthRound+1;

	    //process all messages for the previous round
	    if(rc.getRoundNum() > 1) {
	        enemy_msg_cnt = 0;
	        enemy_msg_sum = 0;
	        for (Transaction t : rc.getBlock(rc.getRoundNum() - 1)){
	            processMessage(t);
	        }
	        if(enemy_msg_cnt > 0){
	            base_wager = (2*(enemy_msg_sum/(enemy_msg_cnt + 7-rc.getBlock(rc.getRoundNum() - 1).length)
						+ 1) + base_wager)/3 + 1;
	        }else{
	            base_wager *= .8;
	        }
	        base_wager = Math.max(base_wager, 1);
	    }

	    if(!sentInfo && !queuedInfo){
	    	writeMessage(Message.birthInfo(type, rc.getID(), rc.getLocation()));
	        addMessageToQueue();
	        queuedInfo = true;
	    }
	}

	public void endTurn() throws GameActionException{
	    /*submits stuff from messageQueue*/
	    while(messageQueue.size() > 0 && messageQueue.get(0).getCost() <= rc.getTeamSoup()){
			rc.submitTransaction(messageQueue.get(0).getMessage(), messageQueue.get(0).getCost());
	        messageQueue.remove(0);
	    }
	    if (queuedInfo && messageQueue.size() == 0){
	    	queuedInfo = false;
	    	sentInfo = true;	
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

	public Direction nextDir90(Direction dir, boolean cw) {
		/*90 degrees rotation*/
		if(cw){
			return dir.rotateRight().rotateRight();
		}else{
			return dir.rotateLeft().rotateLeft();
		}
	}

	public boolean tryBuild (RobotType r) throws GameActionException {
		for (Direction dir : directions) {
			if (rc.canBuildRobot(r, dir)) {
				rc.buildRobot(r, dir);
				return true;
			}
		}
		return false;
	}

	public boolean tileClear(Direction dir) throws GameActionException {
		/*True if the tile is within 3 elevation and not flooded. Ignores cooldown*/
		if (rc.isLocationOccupied(rc.adjacentLocation(dir)) && !rc.senseFlooding(rc.adjacentLocation(dir))) {
			return true;
		} else return false;
	}

	public boolean orthogonal(Direction dir){
		return (dir.equals(directions[0]) || dir.equals(directions[2]) || dir.equals(directions[4])
				|| dir.equals(directions[6]));
	}

	/* number of moves required for our units to get from a to b */
	public int kingDistance(MapLocation a, MapLocation b) {
		return Math.max(Math.abs(a.x - b.x), Math.abs(a.y - b.y));
	}

	/* horizontal dist + vertical dist */
	public int taxicabDistance(MapLocation a, MapLocation b) {
		return Math.abs(a.x - b.x) + Math.abs(a.y - b.y);
	}

	/* will we dig from this square when terraforming? */
	public boolean isLattice(MapLocation a) {
		return (a.x % 2 == locHQ.x % 2) && (a.y % 2 == locHQ.x % 2);
	}

	/* will we build on this square when terraforming? */
	public boolean isForBuilding(MapLocation a) {
		return !(a.x % 2 == locHQ.x % 2) && !(a.y % 2 == locHQ.x % 2);
	}

	/* will we move on this square when terraforming? */
	public boolean isForMovement(MapLocation a) {
		return (a.x % 2 == locHQ.x % 2) ^ (a.y % 2 == locHQ.x % 2);
	}

	/* can we use this spot to build the wall */
	/* this exists so if we have some wacko 1000 tile right next to HQ, we can 
	later make it so we don't try to power through it */
	public boolean isValidWall(MapLocation a) {
		return rc.onTheMap(a);
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
	    int[] tmessage = t.getMessage();

	    if(tmessage.length != 7){
	    	return;
		}

	    int[] m = new int[7];
	    for(int i=0; i<7; i++){
	        m[i] = tmessage[i]^PADS[i];
	    }

	    int res = (((m[0] >>> 4) ^ (m[0] << 24) ^ (m[1] >>> 8) ^ (m[1] << 20) ^ (m[2] >>> 12) ^ (m[2] << 16) ^
	        (m[3] >>> 16) ^ (m[3] << 12) ^ (m[4] >>> 20) ^ (m[4] << 8) ^ (m[5] >>> 24) ^ (m[5] << 4) ^
	        (m[6] >>> 28) ^ (m[6]))<<4)>>>4;
	    if(nonces_seen.contains(m[0]) || ((m[0]>>>18)+1 < birthRound)){
	    	System.out.println("REPLAY ATTACK DETECTED");
		}
	    if (res != 0 || nonces_seen.contains(m[0]) || ((m[0]>>>18) < birthRound)) { //Checksum or nonce failed, message made for the enemy
	        enemy_msg_cnt++;
	        enemy_msg_sum += t.getCost();
	        //May want to store enemy messages here to find patterns to spread misinformation... ;)
	        return;
	    }else{
	    	nonces_seen.add(m[0]);
		}

	    int ptr = 32;
	    while(ptr <= 191){   //195-4
	    	Message message = new Message(m, ptr);
	    	if (message.type == MessageType.TERMINATE) return;
            executeMessage(message);
            ptr = message.ptr;
	    }
	    debug("Message did not exit properly");  //Should've seen 1111.
	    return;
	}

	public boolean executeMessage(Message message){
	    /*Returns true if message applies to me*/

		//Messages applicable to all robots
		switch (message.type) {
			case HQ_LOC:
				locHQ = new MapLocation(message.data[0], message.data[1]);
				return true;
			case ENEMY_HQ_LOC:
				if (enemyHQ != null) return true;
				enemyHQ = new MapLocation(message.data[0], message.data[1]);
				return true;
		}
	    return false;
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
	    //Resets currMessage to all 0's, and messagePtr to 32.
	    messagePtr = 32;
	    currMessage = new int[7];
	    return;
	}

	public void writeMessage(Message message){
	/*Writes a command into currMessage. Will not do anything if it does not leave 4 bits for message end
	  and the 28 bit checksum. This means it can only write up to (but not including) bit 192 (index 191).
	 */
	  	int totalSize = 0;
	  	for (int i = 0; i < message.sizes.length; i++) totalSize += message.sizes[i];
	  	if (messagePtr >= 192 - totalSize) addMessageToQueue(base_wager);
	  	writeInt(message.type.ordinal(), 4);
	  	for (int i = 0; i < message.data.length; i++) {
	  		writeInt(message.data[i], message.sizes[i]);
	  	}
	}

	public void addMessageToQueue(){
	    addMessageToQueue(base_wager);
	}

	public void addMessageToQueue(int wager){
	    /*Does the following
	    Writes the 1111 message end
	    Adds nonce to currMessage[0]
	    Sets the last 28 bits to meet the checksum
	    Applies the pad
	    Adds transaction to messageQueue
	    resetMessage();
	    Returns true if successful
	 */
	    writeInt(MessageType.TERMINATE.ordinal(), 4);

		//Add nonce
		currMessage[0] += (rc.getRoundNum() << 18) + r.nextInt(1<<18);

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
