static int builtMiners;
static boolean hq_sentLoc;
static ArrayList<ArrayList<Unit> > units;

static void initHq() {
	units = new ArrayList<ArrayList<Unit> >(10);
	for(int i=0; i<10; i++){
		units.add(i,new ArrayList<Unit>());
	}
}

static void runHq() throws GameActionException {
	if(!hq_sentLoc){
		writeMessage(0, new int[]{rc.getLocation().x, rc.getLocation().y});
		addMessageToQueue();
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

static class Unit{
	/*HQ uses this class to keep track of all of our units.*/
	public int type;
	public int id;
	public MapLocation lastSent;

	public Unit(int t, int id){
		this.type = t;
		this.id = id;
	}

	public String toString(){
		return robot_types[type] + " (" + id + ")";
	}
}