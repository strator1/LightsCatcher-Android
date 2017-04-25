package com.hs_augsburg_example.lightscatcher.singletons;

import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.hs_augsburg_example.lightscatcher.HomeActivity;
import com.hs_augsburg_example.lightscatcher.R;
import com.hs_augsburg_example.lightscatcher.dataModels.User;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by patrickvalenta on 14.04.17.
 */

public class UserInformation {

    private User current;

    public static UserInformation shared = new UserInformation();

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    private UserInformation() {
        this.mAuth = FirebaseAuth.getInstance();
        this.mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    public void createNewUser(String uid, String name, String email) {
        this.current = new User(name, email, 0);
        mDatabase.child("users").child(uid).setValue(this.current);
    }

    public void increaseUserPoints(int points) {
        if (this.current == null) {
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

    /**
     * Tries to authenticate with Firebase.
     * @return true if authentication succeeded.
     */
    public boolean tryAuthenticate() {
        FirebaseUser firebaseUsr = mAuth.getCurrentUser();
        if (firebaseUsr == null ) return false;

        return true;
    }


}
