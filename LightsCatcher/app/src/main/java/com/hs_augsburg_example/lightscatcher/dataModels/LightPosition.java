package com.hs_augsburg_example.lightscatcher.dataModels;

import android.graphics.Point;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class LightPosition {

    public int isMostRelevant;
    public int phase;
    public int x;
    public int y;

    public LightPosition() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public LightPosition(Point xy, LightPhase phase, boolean isMostRelevant) {
        this.x = xy.x;
        this.y = xy.y;
        this.phase = phase.getValue();
        this.isMostRelevant = isMostRelevant ? 0 : 1;
    }


}

