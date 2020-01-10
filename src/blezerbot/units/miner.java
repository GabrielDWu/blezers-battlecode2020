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
    MapLocation l = rc.getLocation();
    for (Direction dir : directions) {
        newSeenList.add(newVisibleMiner(rc.getLocation(), dir));
        newSeenDirs.add(dir);
    }
    Direction maxl = null;
    while (maxl == null || !tryMove(maxl)) {
        ArrayList<Integer> newNewSeenList = (ArrayList<Integer>)newSeenList.clone();
        ArrayList<Direction> newNewSeenDirs = (ArrayList<Direction>)newSeenDirs.clone();
        int max = -1;
        while (newNewSeenList.size() > 0) {
            int ri = r.nextInt(newNewSeenList.size());
            int newv = newNewSeenList.remove(ri);
            Direction newl = newNewSeenDirs.remove(ri);
            if (newv > max && rc.canMove(newl) && !rc.senseFlooding(rc.adjacentLocation(newl))) {
                maxl = newl;
                max = newv;
            }
        }
    }
}


static int[][] aNewVisibleMiner = new int[][]{{6,0},{6,1},{6,-1},{6,2},{6,-2},{6,3},{6,-3},{5,4},{5,-4},{4,5},{4,-5}};
static int[][] aNewVisibleMinerDiag = new int[][]{{6,-2},{6,-1},{6,0},{6,1},{6,2},{6,3},{6,4},{5,4},{5,5},{4,5},{4,6},{3,6},{2,6},{1,6},{0,6},{-1,6},{-2,6}};
static int newVisibleMiner(MapLocation loc, Direction dir) throws GameActionException {
    int visible = 0;
    if (dir.dy == 0) {
        MapLocation nloc;
        int x = loc.x;
        int y = loc.y;
        for (int i = 0; i < aNewVisibleMiner.length; i++) {
            nloc = loc.translate(aNewVisibleMiner[i][0]*dir.dx, aNewVisibleMiner[i][1]);
            if (onMap(nloc) && !seen.contains(nloc)) visible++;
        }
    } else if (dir.dx == 0) {
        MapLocation nloc;
        for (int i = 0; i < aNewVisibleMiner.length; i++) {
            nloc = loc.translate(aNewVisibleMiner[i][1], aNewVisibleMiner[i][0]*dir.dy);
            if (onMap(nloc) && !seen.contains(nloc)) visible++;
        }
    } else {
        MapLocation nloc;
        for (int i = 0; i < aNewVisibleMinerDiag.length; i++) {
            nloc = loc.translate(aNewVisibleMinerDiag[i][0]*dir.dx, aNewVisibleMinerDiag[i][1]*dir.dy);
            if (onMap(nloc) && !seen.contains(nloc)) visible++;
        }
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
