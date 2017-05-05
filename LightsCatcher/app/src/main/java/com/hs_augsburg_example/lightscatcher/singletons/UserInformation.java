package com.hs_augsburg_example.lightscatcher.singletons;

import android.database.sqlite.SQLiteBindOrColumnIndexOutOfRangeException;
import android.support.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.hs_augsburg_example.lightscatcher.dataModels.User;
import com.hs_augsburg_example.lightscatcher.utils.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.Observable;

/**
 * Created by patrickvalenta on 14.04.17.
 * Changed by quirin on 25.04.17.
 */

/**
 * Holds state of the current user.
 */
public class UserInformation extends Observable {
    private static final String TAG = "UserInformation";
    private static final boolean LOG = Log.ENABLED && true;

    public static UserInformation shared = new UserInformation();

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private User usrSnapshot;
    private FirebaseUser firebaseUsr;
    private ValueEventListener currentUserListener;
    private DatabaseReference currentUserRef;


    private UserInformation() {
        this.mAuth = FirebaseAuth.getInstance();
        mAuth.addAuthStateListener(new AuthStateListener());
        this.mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    public static void init() {
        // just to get the static contructor called in time
    }

    private class AuthStateListener implements FirebaseAuth.AuthStateListener {
        @Override
        public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
            FirebaseUser fbu = firebaseAuth.getCurrentUser();
            if (LOG) Log.d(TAG, "onAuthStateChanged");

            switchUser(fbu);
        }
    }

    public Task<Void> increaseUserPoints(int points) {
        if (this.usrSnapshot == null) {
            return null;
        }

        this.usrSnapshot.addToPoints(points);
        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put("/users/" + getUid(), this.usrSnapshot.toMap());
        return mDatabase.updateChildren(childUpdates);
    }

    public String getUid() {
        return isLoggedIn() ? mAuth.getCurrentUser().getUid() : null;
    }

    public boolean isLoggedIn() {
        return this.mAuth.getCurrentUser() != null;
    }

    public void logout() {
        this.mAuth.signOut();

// the auth listener will do the rest

    }

    public User getUserSnapshot() {
        return usrSnapshot;
    }

    /**
     * Updates and {@field usrSnapshot} and notifies all Observers
     *
     * @param usr
     */
    private void setUserSnapshot(User usr) {
        this.usrSnapshot = usr;
        this.setChanged();
        UserInformation.shared.notifyObservers();
    }

    /**
     * Returns true if user is authenticated
     */
    public boolean tryAuthenticate() {
        if (LOG) Log.d(TAG, "tryAuthenticate;");
        return mAuth.getCurrentUser() != null;
    }

    private void switchUser(FirebaseUser newUsr) {
        // lets see if the user has changed
        if (this.firebaseUsr != null && newUsr != null && this.firebaseUsr.getUid().equals(newUsr.getUid()))
            return; // user did not change

        if (this.firebaseUsr != null)
            stopListenToCurrentUser();

        this.firebaseUsr = newUsr;
        if (newUsr != null)
            this.startListenToUser(newUsr.getUid());
    }

    private void stopListenToCurrentUser() {
        if (this.currentUserRef != null && this.currentUserListener != null) {
            currentUserRef.removeEventListener(this.currentUserListener);
            // improves offline-mode
            this.currentUserRef.keepSynced(false);
        }
        usrSnapshot = null;
        currentUserRef = null;
    }

    private void startListenToUser(final String uid) {
        //Log.d(TAG,"startListenToUser; uid=" + uid);

        // detach listener from previos user
        stopListenToCurrentUser();

        // attach listener to current user
        if (this.currentUserListener == null)
            this.currentUserListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (LOG) Log.d(TAG, "fetch user-info from Firebase, onDataChanged");
                    User user = dataSnapshot.getValue(User.class);
                    user.uid = uid;
                    UserInformation.shared.setUserSnapshot(user);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    if (LOG)
                        Log.e(TAG, "Failed to fetch user-info from Firebase. " + databaseError.getMessage());

                    UserInformation.shared.setUserSnapshot(null); // this snapshot is no longer valid
                }
            };

        this.currentUserRef = FirebaseDatabase.getInstance().getReference("users").child(uid);
        // improves offline-mode
        this.currentUserRef.keepSynced(true);
        this.currentUserRef.addValueEventListener(currentUserListener);
    }
}
