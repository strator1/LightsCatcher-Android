package com.hs_augsburg_example.lightscatcher.dataModels;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.util.HashMap;
import java.util.Map;

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

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("name", name);
        result.put("email", email);
        result.put("points", points);

        return result;
    }

    @Exclude
    public void addToPoints(int points) {
        this.points += points;
    }

}
