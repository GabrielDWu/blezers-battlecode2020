package buildawall2;
import battlecode.common.*;
import java.util.HashMap;

public strictfp class RobotPlayer {
    static RobotController rc;

    static Direction[] directions = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
    static RobotType[] spawnedByMiner = {RobotType.REFINERY, RobotType.VAPORATOR, RobotType.DESIGN_SCHOOL,
            RobotType.FULFILLMENT_CENTER, RobotType.NET_GUN};

    static int turnCount;
	static Pair<Integer, Integer> state = new Pair<Integer, Integer>(3, 0);
	final static int length = 4;
	static int progress;

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its current status.
        RobotPlayer.rc = rc;

        turnCount = 0;
		progress = 0;

        while (true) {
            turnCount += 1;
            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
                // Here, we've separated the controls into a different method for each RobotType.
                // You can add the missing ones or rewrite this into your own control structure.
                switch (rc.getType()) {
                    case HQ:                 runHQ();                break;
                    case MINER:              runMiner();             break;
                    case DESIGN_SCHOOL:      runDesignSchool();      break;
                    case LANDSCAPER:         runLandscaper();        break;
                }

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            }
        }
    }

    static void runHQ() throws GameActionException {
        if (progress == 0) {
			if (tryBuild(RobotType.MINER, Direction.EAST)) ++progress;
		}
    }

    static void runMiner() throws GameActionException {
        if (progress == 0) {
			if (tryMove(Direction.SOUTHEAST)) ++progress;
		} else if (progress == 1) {
			if (tryBuild(RobotType.DESIGN_SCHOOL, Direction.SOUTHEAST)) ++progress;
		} else if (progress == 2) {
			if (tryMove(Direction.EAST)) ++progress;
		}
    }

    static void runDesignSchool() throws GameActionException {
		tryBuild(RobotType.LANDSCAPER, Direction.WEST);
    }
	
    static void runLandscaper() throws GameActionException {
		int move = turnCount % 3;
		if (move == 0) {
			if (tryMove(directions[state.f])) {
				++state.s;
				if (state.s == length) {
					state.f = (state.f + 1) % 4;
					state.s = 0;
				}
			}
		} else if (move == 1) {
			tryDig(directions[(state.f + 3) % 4]);
		} else {
			tryDeposit(Direction.CENTER);
		}
    }

    static boolean tryMove(Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canMove(dir)) {
            rc.move(dir);
            return true;
        } else return false;
    }

    static boolean tryBuild(RobotType type, Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canBuildRobot(type, dir)) {
            rc.buildRobot(type, dir);
            return true;
        } else return false;
    }
	
	static boolean tryDig(Direction dir) throws GameActionException {
		if (rc.isReady() && rc.canDigDirt(dir)) {
			rc.digDirt(dir);
			return true;
		} else return false;
	}
	
	static boolean tryDeposit(Direction dir) throws GameActionException {
		if (rc.isReady() && rc.canDepositDirt(dir)) {
			rc.depositDirt(dir);
			return true;
		} else return false;
	}
	
	static class Pair<A extends Comparable<A>, B extends Comparable<B>> implements Comparable<Pair<A, B>> {
    public A f;
    public B s;
 
    public Pair(A a, B b) {
      f = a;
      s = b;
    }
 
    public int compareTo(Pair<A, B> other) {
      int v = f.compareTo(other.f);
      if (v != 0) return v;
      return s.compareTo(other.s);
    }
 
    public String toString() {
      return "(" + f.toString() + ", " + s.toString() + ")";
    }
  }
}
