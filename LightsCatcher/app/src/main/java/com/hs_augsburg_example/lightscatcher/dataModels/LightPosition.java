package com.hs_augsburg_example.lightscatcher.dataModels;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;
import com.hs_augsburg_example.lightscatcher.singletons.PhotoInformation;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;

@IgnoreExtraProperties
public class LightPosition {

    public boolean isMostRelevant;
    public int phase;
    public double x;
    public double y;

    public LightPosition() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public LightPosition(double x, double y, LightPhase phase, boolean isMostRelevant) {
        this.x = x;
        this.y = y;
        this.phase = phase.value;
        this.isMostRelevant = isMostRelevant;
    }

    @Exclude
    public void validate() {
        if (x < 0 || x > 1.0)
            throw new IllegalArgumentException("LightPosition.x was out of valid range. 0.0 <= x <= 1.0");
        if (y < 0 || y > 1.0)
            throw new IllegalArgumentException("LightPosition.y was out of valid range. 0.0 <= y <= 1.0");
    }

    @Exclude
    public LightPhase getPhase() {
        return LightPhase.fromValue(this.phase);
    }

    public void setPhase(LightPhase phase) {
        this.phase = phase.value;
    }

    /**
     * This List makes sure that the most relevant {@link LightPosition} is always at index 0
     */
    public static class List extends ArrayList<LightPosition> {
        //final java.util.List<LightPosition> list = new ArrayList<>(2);

        @Exclude
        public LightPosition getMostRelevant() {
            if (size() > 0)
                return get(0);
            else
                return null;
        }

        public void setMostRelevant(LightPosition position) {
            this.add(0, position);
        }


        public void add(LightPhase phase, double x, double y) {
            boolean mostRelevant = size() == 0;
            super.add(new LightPosition(x, y, phase, mostRelevant));
        }

        @Override
        public void add(int index, LightPosition element) {
            if (index == 0) {
                LightPosition current;
                if (size() > 0 && (current = get(0)).isMostRelevant)
                // there can be only one most relevant element
                    current.isMostRelevant = false;
            }
            super.add(index, element);
        }

        @Override
        public boolean add(LightPosition position) {
            if (position.isMostRelevant) {
                setMostRelevant(position);
                return true;
            } else
                return super.add(position);
        }
    }
}

