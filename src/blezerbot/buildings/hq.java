static int builtMiners;

static void runHq() throws GameActionException {
	if (builtMiners < 2) {
		for (Direction dir : directions) {
			if (rc.canBuildRobot(RobotType.MINER, dir)) {
				rc.buildRobot(RobotType.MINER, dir);
				builtMiners++;
			}
		}
	}
}
