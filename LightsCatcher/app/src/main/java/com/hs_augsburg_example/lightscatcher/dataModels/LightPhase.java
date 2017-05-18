package com.hs_augsburg_example.lightscatcher.dataModels;

/**
 * Created by quirin on 26.04.17.
 */

public enum LightPhase {

    RED(0), GREEN(1), OFF(2),;

    private static LightPhase[] array = new LightPhase[]{RED, GREEN, OFF};
    public final int value;

    LightPhase(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static LightPhase fromValue(int value) {
        return array[value];
    }
    public String   getGermanText(){
        switch (value) {
            case 0:
                return "Rot-Phase";
            case 1:
                return "Grün-Phase";
            default:
                return "Ampel";
        }
    }

    public static LightPhase toggle(LightPhase original) {
        switch (original) {
            case RED:
                return GREEN;
            case GREEN:
                return RED;
            default:
            case OFF:
                return RED;
        }
    }

}
