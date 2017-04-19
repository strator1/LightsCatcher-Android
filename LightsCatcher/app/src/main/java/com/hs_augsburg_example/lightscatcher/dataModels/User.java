package com.hs_augsburg_example.lightscatcher.dataModels;

/**
 * Created by quirin on 14.04.17.
 */

public class User {
    public String email;
    public String name;
    public int points;

    public User(){}


    @Override
    public String toString() {
        return name;
    }
}
