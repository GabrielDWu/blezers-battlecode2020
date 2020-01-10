static void processMessage(Transaction t) {
    //Check if the message was made by my team
    //The way it works: xor all of them with PAD
    //Then convert into 224 bits and do a 0-checksum with 8 blocks of 28 bits.
    int[] message = t.getMessage();

    int[] m = new int[7];
    for(int i=0; i<7; i++){
        m[i] = message[i]^PADS[i];
    }

    int res = (((m[0] >>> 4) ^ (m[0] << 24) ^ (m[1] >>> 8) ^ (m[1] << 20) ^ (m[2] >>> 12) ^ (m[2] << 16) ^
        (m[3] >>> 16) ^ (m[3] << 12) ^ (m[4] >>> 20) ^ (m[4] << 8) ^ (m[5] >>> 24) ^ (m[5] << 4) ^
        (m[6] >>> 28) ^ (m[6]))<<4)>>>4;

    if (res != 0) { //Checksum failed, message made for the enemy
        enemy_msg_cnt++;
        enemy_msg_sum += t.getCost();
        //May want to store enemy messages here to find patterns to spread misinformation... ;)
        return;
    }

    int ptr = 0;
    while(ptr <= 191){   //195-4
        int id = getInt(m, ptr, 4);
        ptr += 4;
        if(id==0){ //0000 Set our HQ
            if(ptr >= 184){ //Requires 2 6-bit integers
                System.out.println("Message did not exit properly");
                return;
            }
            int x = getInt(m, ptr, 6);
            if(x==0)x=64;
            ptr += 6;
            int y = getInt(m, ptr, 6);
            if(y==0)x=64;
            ptr += 6;
            locHQ = new MapLocation(x,y);
            System.out.println("Now I know that my HQ is at" + locHQ);
        }else if(id==1){
            if(ptr >= 177){
                System.out.println("Message did not exit properly");
                return;
            }
            if(type == 0){//Only HQ keeps track of other units
                int unit_type = getInt(m, ptr, 4);
                ptr += 4;
                int unit_id = getInt(m, ptr, 15);
                ptr += 15;
                units.get(unit_type).add(new Unit(unit_type, unit_id));
                System.out.println("Added unit" + new Unit(unit_type,unit_id));
            }else{
                ptr += 19;
            }
        }
        else if(id==15){    //1111 Message terminate
            return;
        }
    }
    System.out.println("Message did not exit properly");  //Should've seen 1111.
    return;
}

static int getInt(int[] m, int ptr, int size){
    /*Turns the next <size> bits into an integer from 0 to 2**size-1. Does not modify ptr.*/
    assert(size <= 32);
    if(32-(ptr%32) < size){
        return ((m[ptr/32]<<(size-(32-(ptr%32)))) + (m[ptr/32+1]>>>(64-size-(ptr%32))))%(1<<size);
    }else{
        return (m[ptr/32]>>>(32-(ptr%32)-size))%(1<<size);
    }
}

static void writeInt(int x, int size){
    /*Writes the next <size> bits of currMessage with an integer 0 to 2**size-1. Modifies messagePtr.*/
    assert(size <= 32);
    if(32-(messagePtr%32) < size){
        currMessage[messagePtr/32] += x >>> (size-(32-(messagePtr%32)));
        currMessage[messagePtr/32+1] += (x%(1<<(size-(32-(messagePtr%32)))))<<(64-size-(messagePtr%32));
    }else{
        currMessage[messagePtr/32] += x << (32-(messagePtr%32)-size);
    }
    messagePtr += size;
    return;
}

static void resetMessage(){
    //Resets currMessage to all 0's, and messagePtr to 0.
    messagePtr = 0;
    currMessage = new int[7];;
    return;
}

static void writeMessage(int id, int[] params){
/*Writes a command into currMessage. Will not do anything if it does not leave 4 bits for message end
  and the 28 bit checksum. This means it can only write up to (but not including) bit 192 (index 191).
 */
    if(id==0){ //0000 Set our HQ
        if(messagePtr >= 176){ //Requires id + 2 6-bit integers
            addMessageToQueue(base_wager);
        }
        writeInt(id, 4);
        writeInt(params[0], 6);
        writeInt(params[1], 6);
    }if(id==1){ //0000 Set our HQ
        if(messagePtr >= 169){ //Requires id + 4-bit int + 15-bit int
            addMessageToQueue(base_wager);
        }
        writeInt(id, 4);
        writeInt(params[0], 4);
        writeInt(params[1], 15);
    }
    return;
}

static void addMessageToQueue(){
    addMessageToQueue(base_wager);
}

static void addMessageToQueue(int wager){
    /*Does the following
    Writes the 1111 message end
    Sets the last 28 bits to meet the checksum
    Applies the pad
    Adds transaction to messageQueue
    resetMessage();
    Returns true if successful
 */
    writeInt(15, 4);

    int res = (((currMessage[0] >>> 4) ^ (currMessage[0] << 24) ^ (currMessage[1] >>> 8) ^ (currMessage[1] << 20) ^
        (currMessage[2] >>> 12) ^ (currMessage[2] << 16) ^ (currMessage[3] >>> 16) ^ (currMessage[3] << 12) ^
        (currMessage[4] >>> 20) ^ (currMessage[4] << 8) ^ (currMessage[5] >>> 24) ^ (currMessage[5] << 4) ^
        (currMessage[6] >>> 28))<<4)>>>4;
    currMessage[6] += res;

    for(int i=0; i<7; i++){
        currMessage[i] ^= PADS[i];
    }
    messageQueue.add(new Transaction(wager, currMessage, 1));
    resetMessage();
    return;
}