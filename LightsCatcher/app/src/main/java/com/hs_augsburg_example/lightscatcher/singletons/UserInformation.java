package com.hs_augsburg_example.lightscatcher.singletons;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.widget.Toast;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.hs_augsburg_example.lightscatcher.App;
import com.hs_augsburg_example.lightscatcher.activities_minor.LoginActivity;
import com.hs_augsburg_example.lightscatcher.dataModels.User;
import com.hs_augsburg_example.lightscatcher.utils.ActivityRegistry;
import com.hs_augsburg_example.lightscatcher.utils.Log;

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
    private ValueEventListener currentUserBannedListener;
    private DatabaseReference currentUserRef;
    private DatabaseReference currentUserBannedRef;

    private UserInformation() {
        this.mAuth = FirebaseAuth.getInstance();
        mAuth.addAuthStateListener(new AuthStateListener());
        this.mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    public static void init() {
        // just to get the static constructor called in time
    }

    private class AuthStateListener implements FirebaseAuth.AuthStateListener {
        private boolean calledBefore = false;

        @Override
        public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
            if (LOG) Log.d(TAG, "onAuthStateChanged");
            FirebaseUser fbu = firebaseAuth.getCurrentUser();

            // skip first call because it is called right after AuthStateListener was registered
            if (calledBefore && firebaseUsr != null && fbu == null) {
                // user signed out
                App app = App.getCurrent();
                Intent intentLogout = new Intent(app, LoginActivity.class);
                intentLogout.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                app.startActivity(intentLogout);
                ActivityRegistry.finishAll();
            }
            calledBefore = true;
            switchUser(fbu);

        }
    }

    public String getUserId() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        return currentUser != null ? currentUser.getUid() : null;
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

        // detach listener from previous user
        stopListenToCurrentUser();

        if (newUsr != null) {
            this.startListenToUser(newUsr.getUid());
        }
        this.firebaseUsr = newUsr;
    }

    private void stopListenToCurrentUser() {
        if (this.currentUserRef != null && this.currentUserListener != null) {
            currentUserRef.removeEventListener(this.currentUserListener);
            // improves offline-mode
            this.currentUserRef.keepSynced(false);
        }
        if (currentUserBannedRef != null && currentUserBannedListener != null) {
            currentUserBannedRef.removeEventListener(currentUserBannedListener);
        }
        usrSnapshot = null;
        currentUserRef = null;
    }

    private void startListenToUser(final String uid) {
        startListenToUserData(uid);
        startListenToUserBanned(uid);

    }

    private void startListenToUserBanned(String uid) {
        if (currentUserBannedListener == null)
            currentUserBannedListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        // user was added to ban list
                        mAuth.signOut();
                        try {
                            App app = App.getCurrent();

                            Toast.makeText(app, "Dein Account wurde gesperrt!", Toast.LENGTH_LONG).show();
                        } catch (Exception ex) {
                            Log.e("App", ex);
                        }
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.e(TAG, databaseError.getMessage());
                }
            };

        currentUserBannedRef = FirebaseDatabase.getInstance().getReference("bannedUsers").child(uid);
        currentUserBannedRef.addValueEventListener(currentUserBannedListener);
    }

    private void startListenToUserData(final String uid) {
        // attach listener to current user
        if (this.currentUserListener == null)
            this.currentUserListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (LOG) Log.d(TAG, "fetch user-info from Firebase, onDataChanged");
                    User user = dataSnapshot.getValue(User.class);
                    if (user == null) {
                        // this happens after a new user was created, and the snapshot was not submitted to the database
                    } else {
                        user.uid = uid;
                        UserInformation.shared.setUserSnapshot(user);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
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
