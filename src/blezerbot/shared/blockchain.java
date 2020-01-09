static void processMessage(Transaction t) {
        //Check if the message was made by my team
        //The way it works: xor all of them with PAD
        //Then convert into 224 bits and do a 0-checksum with 8 blocks of 28 bits.
        int[] message = t.getMessage();

        System.out.println("Message Being Processed");
        int[] m = new int[7];
        for(int i=0; i<7; i++){
            m[i] = message[i]^PADS[i];
        }

        boolean bits[] = new boolean[224];
        boolean checksum[] = new boolean[28];
        int ptr = 0;    //This is a local ptr for reading a message, different than messagePtr (which is for writing)
        for(int i=0; i<message.length; i++){
            for(int j=31; j>=0; j--){
                bits[ptr] = 1==((m[i] >> j)&1);
                checksum[ptr%28] ^= bits[ptr];
                ptr++;
            }
        }
        int res = 0;
        for(int i=0; i<28; i++){
            if(checksum[i]){
                res ++;
            }
        }

        if (res != 0) { //Checksum failed, message made for the enemy
            enemy_msg_cnt++;
            enemy_msg_sum += t.getCost();
            //May want to store enemy messages here to find patterns to spread misinformation... ;)
            return;
        }

        ptr = 0;
        while(ptr <= 191){   //195-4
            int id = getInt(bits, ptr, 4);
            ptr += 4;
            if(id==0){ //0000 Set our HQ
                if(ptr >= 184){ //Requires 2 6-bit integers
                    System.out.println("Message did not exit properly");
                    return;
                }
                int x = getInt(bits, ptr, 6);
                if(x==0)x=64;
                ptr += 6;
                int y = getInt(bits, ptr, 6);
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
                    int unit_type = getInt(bits, ptr, 4);
                    ptr += 4;
                    int unit_id = getInt(bits, ptr, 15);
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

static int getInt(boolean[] bits, int ptr, int size){
        /*Turns the next <size> bits into an integer from 0 to 2**size-1. Does not modify ptr.*/
        int x = 0;
        for(int i=0; i<size; i++){
            x *= 2;
            if(bits[ptr+i]) x++;
        }
        return x;
}

static void writeInt(int x, int size){
        /*Writes the next <size> bits of currMessage with an integer 0 to 2**size-1. Modifies messagePtr.*/
        for(int i=size-1; i>=0; i--){
            currMessage[messagePtr] = 1==((x>>i)&1);
            messagePtr++;
        }
        return;
}

static void resetMessage(){
    //Resets currMessage to all 0's, and messagePtr to 0.
        messagePtr = 0;
        currMessage = new boolean[224];
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
        Condenses it into 7 32-bit integers
        Applies the pad
        Adds transaction to messageQueue
        resetMessage();

        Returns true if successful
     */
        writeInt(15, 4);

        boolean checksum[] = new boolean[28];
        for(int i=0; i<196; i++){
            checksum[i%28] ^= currMessage[i];
        }
        for(int i=0; i<28; i++){
            currMessage[i+196] = checksum[i];
        }

        int[] words = new int[7];
        int ptr = 0;
        for(int i=0; i<7; i++){
            for(int j=0; j<32; j++){
                words[i] <<= 1;
                if(currMessage[ptr]){
                    words[i]++;
                }
                ptr++;
            }
        }

        for(int i=0; i<7; i++){
            words[i] ^= PADS[i];
        }
        messageQueue.add(new Transaction(wager, words));
        resetMessage();
        return;
}