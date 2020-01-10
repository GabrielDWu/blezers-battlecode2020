package blezerbot;
import battlecode.common.*;
import java.util.*;
import java.lang.Math;

public strictfp class RobotPlayer {


    public static void run(RobotController rc) throws GameActionException {
        if (seen == null) seen = new boolean[rc.getMapWidth()][rc.getMapHeight()];
        if (visited == null) visited = new int[rc.getMapWidth()][rc.getMapHeight()];
        if (r == null) r = new Random(rc.getID());
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