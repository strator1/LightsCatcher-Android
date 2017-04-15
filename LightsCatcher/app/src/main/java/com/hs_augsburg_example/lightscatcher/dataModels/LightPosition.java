package com.hs_augsburg_example.lightscatcher.dataModels;

import com.google.firebase.database.IgnoreExtraProperties;

/**
 * Created by patrickvalenta on 15.04.17.
 */

@IgnoreExtraProperties
public class LightPosition {

    public int isMostRelevant;
    public int phase;
    public int x;
    public int y;

    public LightPosition() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public LightPosition(int x, int y, int phase, boolean isMostRelevant) {
        this.x = x;
        this.y = y;
        this.phase = phase;
        this.isMostRelevant = isMostRelevant ? 0 : 1;
    }

}
