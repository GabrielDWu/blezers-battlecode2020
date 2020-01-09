static RobotController rc;
static int turnCount;
static int birthRound;  //What round was I born on?
static int[] currMessage;
static LinkedList<Transaction> messageQueue = new LinkedList<Transaction>();
static int messagePtr;  //What index in currMessage is my "cursor" at?
static MapLocation locHQ;   //Where is my HQ?
static boolean sentInfo;    //Sent info upon spawn
static int type;    //Integer from 0 to 8, index of robot_types
static int base_wager = 2;
static int enemy_msg_cnt;   //How many enemy messages went through last round?
static int enemy_msg_sum;   //Total wagers of enemy messages last round.


static void startLife() throws GameActionException{
    System.out.println("Got created.");

    switch (rc.getType()) {
        case HQ:                 type=0;initHq();    break;
        case MINER:              type=1;    break;
        case REFINERY:           type=2;    break;
        case VAPORATOR:          type=3;    break;
        case DESIGN_SCHOOL:      type=4;    break;
        case FULFILLMENT_CENTER: type=5;    break;
        case LANDSCAPER:         type=6;    break;
        case DELIVERY_DRONE:     type=7;    break;
        case NET_GUN:            type=8;    break;
    }

    //process all messages from beginning of game until you find hq location
    int checkRound = 1;
    while (checkRound < rc.getRoundNum()-1 && locHQ == null) {
        for (Transaction t : rc.getBlock(checkRound)){
            processMessage(t);
            if(locHQ != null){
                break;
            }
        }
        checkRound++;
    }
    birthRound = rc.getRoundNum();
    resetMessage();
}

static void startTurn() throws GameActionException{
    //if(rc.getRoundNum() >= 10){rc.resign();}
    turnCount = rc.getRoundNum()-birthRound+1;

    //process all messages for the previous round
    if(rc.getRoundNum() > 1) {
        enemy_msg_cnt = 0;
        enemy_msg_sum = 0;
        for (Transaction t : rc.getBlock(rc.getRoundNum() - 1)){
            processMessage(t);
        }
        if(enemy_msg_cnt > 0){
            base_wager = ((enemy_msg_sum/enemy_msg_cnt + 1) + base_wager)/2;
        }else{
            base_wager *= .8;
        }
        base_wager = Math.max(base_wager, 1);
    }

    if(!sentInfo){
        writeMessage(1, new int[]{type, rc.getID()});
        addMessageToQueue();
        sentInfo = true;
    }
}

static void endTurn() throws GameActionException{
    /*submits stuff from messageQueue*/
    while(messageQueue.size() > 0 && messageQueue.get(0).getCost() <= rc.getTeamSoup()){
        rc.submitTransaction(messageQueue.get(0).getMessage(), messageQueue.get(0).getCost());
        messageQueue.remove(0);
    }
}