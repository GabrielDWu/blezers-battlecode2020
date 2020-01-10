package blezerbot;
import battlecode.common.*;
import java.util.*;

public strictfp class RobotPlayer {


    public static void run(RobotController rc) throws GameActionException {
        if (seen == null) seen = new HashSet<MapLocation>();
        RobotPlayer.rc = rc;
        startLife();

        while (true) {
            startTurn();
            try {
                switch (robot_types[type]) { /*{%SWITCH%}*/ }
                endTurn();
                Clock.yield();
            } catch (Exception e) {
                System.out.println(robot_types[type] + " Exception");
                e.printStackTrace();
            }
        }
    }
    /*{%CODE%}*/
}