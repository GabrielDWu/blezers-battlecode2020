package blezerbot;

import battlecode.common.*;

public class Message {

	static enum MessageType {
		HQ_LOC,
		ENEMY_HQ_LOC,
		BIRTH_INFO,
		BUILD_SPECUNIT_ANYLOC,
		TRANSPORT,
		STOP
	}

	MessageType type;

	int[] data;
	int[] sizes;

	public Message(int type, int[] sizes, int[] data) {
		this.sizes = sizes;
		this.data = data;
	}

	/*public static Message hqLocation(MapLocation l) {
		return new Message(new int[]{6, 6}, new int[]{l.x, l.y});
	}

	public static Message enemyHqLocation(MapLocation l) {
		return new Message(new int[]{6, 6}, new int[]{l.x, l.y});
	}

	public static void birthInfo(MapLocation l) {

	}*/

}
