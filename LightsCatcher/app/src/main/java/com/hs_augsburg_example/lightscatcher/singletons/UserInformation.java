package com.hs_augsburg_example.lightscatcher.singletons;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.hs_augsburg_example.lightscatcher.dataModels.User;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by patrickvalenta on 14.04.17.
 */

public class UserInformation {

    public User current;

    public static UserInformation shared = new UserInformation();

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    private UserInformation() {
        this.mAuth = FirebaseAuth.getInstance();
        this.mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    public void createNewUser(String name, String email) {
        this.current = new User(name, email, 0);
        mDatabase.child("users").child(UUID.randomUUID().toString().toUpperCase()).setValue(this.current);
    }

    public void updateUserPoints(int points) {
        if (!isLoggedIn()) {
            return;
        }

        this.current.addToPoints(points);
        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put("/users/" + getUid(), this.current.toMap());
        mDatabase.updateChildren(childUpdates);
    }

    public String getUid() {
        return isLoggedIn() ? mAuth.getCurrentUser().getUid() : null;
    }

    public boolean isLoggedIn() {
        return this.mAuth.getCurrentUser() != null;
    }

    public void logout() {
        this.mAuth.signOut();
        current = null;
    }

    public User getCurrent() {
        return current;
    }

    public void setCurrent(User current) {
        this.current = current;
    }
}
