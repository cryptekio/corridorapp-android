package com.cryptekio.corridor.entity;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class Emitter extends RealmObject {

    @PrimaryKey
    private long id;
    private String uuid;
    private RealmList<BTValue> values;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public RealmList<BTValue> getValues() {
        return values;
    }

    public void setValues(RealmList<BTValue> values) {
        this.values = values;
    }


}
