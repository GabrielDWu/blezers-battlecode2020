package blezerbot.units;

import battlecode.common.*;
import java.util.*;
import blezerbot.*;

public class Landscaper extends Unit {

	public Landscaper(RobotController rc) throws GameActionException {
		super(rc);
	}

	public void run() throws GameActionException {
		super.run();
		if (locHQ != null && distHQ() > 5) {
			for (Direction dir : directions) {
				if (rc.canDigDirt(dir)) rc.digDirt(dir);
			}
			goTo(locHQ);
		} else {
			for (Direction dir : directions) {
				if (rc.canDepositDirt(dir)) rc.depositDirt(dir);
			}
		}
	}

}
