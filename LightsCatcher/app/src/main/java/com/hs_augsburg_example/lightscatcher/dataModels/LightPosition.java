package com.hs_augsburg_example.lightscatcher.dataModels;

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
    public LightPhase phase;
    public double x;
    public double y;

    public LightPosition() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public LightPosition(double x, double y, LightPhase phase, boolean isMostRelevant) {
        this.x = x;
        this.y = y;
        this.phase = phase;
        this.isMostRelevant = isMostRelevant;
    }


    public void validate() {
        if (x < 0 || x > 1.0)
            throw new IllegalArgumentException("LightPosition.x was out of valid range. 0.0 <= x <= 1.0");
        if (y < 0 || y > 1.0)
            throw new IllegalArgumentException("LightPosition.x was out of valid range. 0.0 <= x <= 1.0");
    }

    /**
     * NOTE: element at index 0 is always the most relevant light
     */
    public static class List implements Iterable<LightPosition> {
        final java.util.List<LightPosition> list = new ArrayList<>(2);

        public LightPosition getMostRelevant() {
            if (list.size() > 0)
                return list.get(0);
            else
                return null;
        }

        @Override
        public Iterator<LightPosition> iterator() {
            return list.iterator();
        }

        public void add(LightPhase phase, double x, double y) {
            boolean mostRelevant = list.size() == 0;
            list.add(new LightPosition(x, y, phase, mostRelevant));
        }

        public void add(LightPosition position) {
            if (position.isMostRelevant) {
                if (size() > 0 && list.get(0).isMostRelevant)
                    throw new IllegalArgumentException("only one light can be the most relevant");
                list.add(0, position);
            } else
                list.add(position);
        }

        public int size() {
            return list.size();
        }
    }
}

