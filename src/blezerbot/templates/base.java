package blezerbot;
import battlecode.common.*;

public strictfp class RobotPlayer {
    static RobotController rc;
    static int turnCount;
    static int birthRound;  //What round was I born on?
    static boolean[] currMessage;
    static int messagePtr;  //What index in currMessage is my "cursor" at?
    static MapLocation locHQ;   //Where is my HQ?
    public static void run(RobotController rc) throws GameActionException {
        RobotPlayer.rc = rc;

        birthRound = rc.getRoundNum();
        resetMessage();
        while (true) {
            turnCount += 1;

            //process all messages for the previous round
            if(rc.getRoundNum() > 1) {
                for (Transaction t : rc.getBlock(rc.getRoundNum() - 1)) {
                    processMessage(t.getMessage());
                }
            }

            try {
                switch (rc.getType()) { /*{%SWITCH%}*/ }
                Clock.yield();
            } catch (Exception e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            }
        }
    }
    /*{%CODE%}*/
}
