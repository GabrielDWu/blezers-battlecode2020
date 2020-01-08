static void runMiner() throws GameActionException {
    for (Direction dir : directions)
        if (tryDeliver(dir))
            System.out.println("I delivered soup!");

    for (Direction dir : directions)
        if (tryMine(dir))
            System.out.println("I mined soup! " + rc.getSoupCarrying());
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

/**
 * Attempts to deliver soup in a given direction.
 *
 * @param dir The intended direction of refining
 * @return true if a move was performed
 * @throws GameActionException
 */
static boolean tryDeliver(Direction dir) throws GameActionException {
    if (rc.isReady() && rc.canDepositSoup(dir)) {
        rc.depositSoup(dir, rc.getSoupCarrying());
        return true;
    } else return false;
}