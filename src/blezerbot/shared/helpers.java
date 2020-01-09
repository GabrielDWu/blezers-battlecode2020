static Direction randomDirection() {
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
