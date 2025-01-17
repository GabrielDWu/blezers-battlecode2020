package turtleblezer;

import battlecode.common.*;
import turtleblezer.*;
import turtleblezer.buildings.*;
import turtleblezer.units.*;

public strictfp class RobotPlayer {
    public static void run(RobotController rc) throws GameActionException {
        switch (rc.getType()) {
            case HQ:                 new HQ(rc);                break;
            case MINER:              new Miner(rc);             break;
            case LANDSCAPER:         new Landscaper(rc);        break;
            case DESIGN_SCHOOL:      new DesignSchool(rc);      break;
            case DELIVERY_DRONE:     new DeliveryDrone(rc);     break;
            case FULFILLMENT_CENTER: new FulfillmentCenter(rc); break;
            case REFINERY:           new Refinery(rc);          break;
            /*
            case VAPORATOR:          new Vaporator(rc);         break;
            case NET_GUN:            new NetGun(rc);            break;*/
        }
    }
}
