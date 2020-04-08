package com.cryptekio.corridor.entity;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class BTValue extends RealmObject {

    @PrimaryKey
    private long id;
    private int timestamp;
    private int rssiValue;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    public int getRssiValue() {
        return rssiValue;
    }

    public void setRssiValue(int rssiValue) {
        this.rssiValue = rssiValue;
    }
}
