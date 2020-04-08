package com.cryptekio.corridor.entity;

import android.util.Log;
import android.util.Pair;

import java.util.ArrayList;

enum State{
    CONNECTING,
    CONNECTED,
    DISCONNECTED,
    FOUND

}

public class BTPeripheral {

    private static final String TAG = "BTPeripheral";

    private String uuid;
    private State state;
    private String userId;
    private boolean isOurDevice = false;

    public ArrayList<Pair<Integer, Integer>> getRSSIs() {
        return RSSIs;
    }

    private ArrayList<Pair<Integer,Integer>> RSSIs = new ArrayList<>();
    private ArrayList<Integer> TXs = new ArrayList<>();

    public String getUserId() {
        return userId;
    }
    public void setUserId(String userId) {
        this.userId = userId;
    }

    public BTPeripheral(String uuid){
        this.state = State.FOUND;
        this.uuid = uuid;
    }

    public void addRSSIValue(Integer value){
        Integer timestamp = (int)((System.currentTimeMillis() / 1000));
        RSSIs.add(new Pair<>(timestamp,value));
    }

    public void addTXValue(Integer tx){
        TXs.add(tx);
    }
    public void clearRSSIValues(){
        RSSIs.clear();
    }

    public boolean isOurDevice() {
        return isOurDevice;
    }

    public void setOurDevice(boolean ourDevice) {
        isOurDevice = ourDevice;
    }

    public void printReport(){
        Log.d(TAG, "userId = " + userId);
        Log.d(TAG, "RSSI: ");

        for (Pair<Integer, Integer> rssi:RSSIs ) {
            Log.d(TAG,"" + rssi);
        }

        Log.d(TAG, "TX: ");
        for (int tx:TXs ) {
            Log.d(TAG,"" + tx);
        }


    }

}
