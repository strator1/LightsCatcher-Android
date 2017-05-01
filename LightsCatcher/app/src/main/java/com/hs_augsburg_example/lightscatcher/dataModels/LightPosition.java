package com.hs_augsburg_example.lightscatcher.dataModels;

import com.google.firebase.database.IgnoreExtraProperties;
import com.hs_augsburg_example.lightscatcher.singletons.PhotoInformation;

@IgnoreExtraProperties
public class LightPosition {

    public int isMostRelevant;
    public int phase;
    public double x;
    public double y;

    public LightPosition() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public LightPosition(double x, double y, LightPhase phase, boolean isMostRelevant) {
        this.x = x;
        this.y = y;
        this.phase = phase.getValue();
        this.isMostRelevant = isMostRelevant ? 0 : 1;
    }


}

