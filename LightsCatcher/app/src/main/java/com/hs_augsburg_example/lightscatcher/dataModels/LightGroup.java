package com.hs_augsburg_example.lightscatcher.dataModels;

import com.google.firebase.database.Exclude;

import java.util.HashMap;
import java.util.UUID;

/**
 * Created by Quirin on 21.05.2017.
 */

public class LightGroup extends HashMap<String, String> {
    @Exclude
    public String id;

    public LightGroup() {

    }

    public LightGroup(String pId) {
        id = pId;
    }


    public static LightGroup create(){
        return new LightGroup(UUID.randomUUID().toString());
    }

    public void put(Photo photo) {

        String key;
        LightPosition mostRelevant = photo.lightPositions.getMostRelevant();
        if (mostRelevant != null) {
            key = mostRelevant.getPhase().toString();
        }
        else
            key = "null";

        String value = photo.id;
        super.put(key, value);

        photo.group = this.id;
        photo.groupRef = this;
    }
}

