package blezerbot;

import battlecode.common.*;

public class Message {

	// don't reorder (code relies on it)
	public static enum MessageType {
		HQ_LOC,
		ENEMY_HQ_LOC,
		BIRTH_INFO,
		BUILD_SPECUNIT_ANYLOC,
		TRANSPORT,
		WAIT,
		BUILD_SPECUNIT_LOC,
		TERMINATE,
		UNWAIT,
		BUILD_WALL,
		DRONE_ATTACK,
		REFINERY_LOC
	}

	static int[][] typeData = new int[][]{
		{6 /*x*/, 6 /*y*/}, // hq loc
		{6 /*x*/, 6 /*y*/}, // enemy hq loc
		{4 /*type*/, 15 /*id*/, 6 /*x*/, 6 /*y*/}, // birth info
		{4 /*type*/, 15 /*builder id*/}, // tell specific unit to build type at any location
		{15 /*drone id*/, 15 /*target id*/, 6 /*start x*/, 6 /*start y*/, 6 /*end x*/, 6 /*end y*/}, // transport
		{15 /*id*/}, // do nothing (wait)
		{4 /*type*/, 15 /*builder id*/, 6 /*x*/, 6 /*y*/}, // tell specific unit to build type at specific location
		{}, // terminate message
		{15 /*id*/, 4 /*instruction*/}, // do something (unwait)
		{6, 6 /*x, y*/}, // build wall at location
		{},	//	drone attack
		{6, 6 /*x, y*/, 15 /*miner id*/}, // tell miner about refinery
	};

	static MessageType[] messageTypes;

	public MessageType type;

	public int[] data;
	int[] sizes;

	int[] message;
	public int ptr;

	public Message(MessageType type, int[] sizes, int[] data) {
		this.type = type;
		this.sizes = sizes;
		this.data = data;
	}

	public Message(int[] message, int ptr) {
		if (messageTypes == null) messageTypes = MessageType.values();
		this.ptr = ptr;
		this.message = message;
		int typeId = getInt(4);
		sizes = typeData[typeId];
		type = messageTypes[typeId];
		data = new int[sizes.length];
		for (int i = 0; i < sizes.length; i++) {
			data[i] = getInt(sizes[i]);
		}
	}

	public static Message hqLocation(MapLocation l) {
		return new Message(MessageType.HQ_LOC, typeData[0], new int[]{l.x, l.y});
	}

	public static Message enemyHqLocation(MapLocation l) {
		return new Message(MessageType.ENEMY_HQ_LOC, typeData[1], new int[]{l.x, l.y});
	}

	public static Message birthInfo(RobotType robotType, int robotID, MapLocation loc) {
		return new Message(MessageType.BIRTH_INFO, typeData[2], new int[]{robotType.ordinal(), robotID, loc.x, loc.y});
	}

	public static Message build(RobotType type, int robotID) {
		return new Message(MessageType.BUILD_SPECUNIT_ANYLOC, typeData[3], new int[]{type.ordinal(), robotID});
	}

	public static Message build(RobotType type, int robotID, MapLocation loc) {
		return new Message(MessageType.BUILD_SPECUNIT_LOC, typeData[6], new int[]{type.ordinal(), robotID, loc.x, loc.y});	
	}

	public static Message transport(int droneID, int targetID, MapLocation pickUp, MapLocation dropOff) {
		return new Message(MessageType.TRANSPORT, typeData[4], new int[]{droneID, targetID, pickUp.x, pickUp.y, dropOff.x, dropOff.y});
	}

	public static Message doNothing(int robotID) {
		return new Message(MessageType.WAIT, typeData[5], new int[]{robotID});
	}

	public static Message doSomething(int robotID, int instruction) {
	    /*
	    0 - DEFENDING (landscaper)
	    1 - TERRAFORMING (landscaper)
	     */
		return new Message(MessageType.UNWAIT, typeData[8], new int[]{robotID, instruction});
	}

	public static Message buildWall(MapLocation loc) {
		return new Message(MessageType.BUILD_WALL, typeData[9], new int[]{loc.x, loc.y});
	}

	public static Message droneAttack(){
		return new Message(MessageType.DRONE_ATTACK, typeData[10], new int[]{});
	}

    public static Message refineryLocation(MapLocation loc, int robotID) {
        return new Message(MessageType.REFINERY_LOC, typeData[11], new int[]{loc.x, loc.y, robotID});
    }

	public int getInt(int size){
	    /*Turns the next <size> bits into an integer from 0 to 2**size-1. Does not modify ptr.*/
	    int r = 0;
	    assert(size <= 32);
	    if(32-(ptr%32) < size){
	        int result = ((message[ptr/32]<<(size-(32-(ptr%32)))) + (message[ptr/32+1]>>>(64-size-(ptr%32))))%(1<<size);
	        r = (result < 0) ? result + (1<<size) : result;
	    }else{
	        r = (message[ptr/32]>>>(32-(ptr%32)-size))%(1<<size);
	    }
	    ptr += size;
	    return r;
	}

}
