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
