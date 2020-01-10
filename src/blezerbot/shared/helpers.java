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
    if (rc.isReady() && rc.canMove(dir) && !rc.senseFlooding(rc.adjacentLocation(dir))) {
        rc.move(dir);
        return true;
    } else return false;
}

static HashSet<MapLocation> seen;

static boolean onMap(MapLocation l) {
	return !(l.x < 0 || l.x >= rc.getMapWidth() || l.y < 0 || l.y >= rc.getMapHeight());
}

static Random r;