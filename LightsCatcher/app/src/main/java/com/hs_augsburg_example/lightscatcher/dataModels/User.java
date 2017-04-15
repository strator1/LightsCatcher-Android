package com.hs_augsburg_example.lightscatcher.dataModels;

import com.google.firebase.database.IgnoreExtraProperties;

/**
 * Created by patrickvalenta on 14.04.17.
 */

@IgnoreExtraProperties
public class User {

    public String name;
    public String email;
    public int points;

    public User() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public User(String name, String email, int points) {
        this.name = name;
        this.email = email;
        this.points = points;
    }

}

