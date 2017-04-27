package com.hs_augsburg_example.lightscatcher.dataModels;

/**
 * Created by quirin on 26.04.17.
 */

public enum LightPhase {
    RED(0), GREEN(1);

    private final int value;

    LightPhase(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
