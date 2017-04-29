package com.hs_augsburg_example.lightscatcher.singletons;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.hs_augsburg_example.lightscatcher.dataModels.User;

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
    public static UserInformation shared = new UserInformation();
    private static  final  String TAG = "UserInformation";

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private User usrSnapshot;
    private FirebaseUser firebaseUsr;
    private ValueEventListener currentUserListener;
    private DatabaseReference currentUserRef;

    private UserInformation() {
        this.mAuth = FirebaseAuth.getInstance();
        this.mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    /*
     * TODO: quirin: Is this method really necessary??
     */
    public void createNewUser(String uid, String name, String email) {
        this.usrSnapshot = new User(uid,name, email, 0);
        mDatabase.child("users").child(uid).setValue(this.usrSnapshot);
    }

    public void increaseUserPoints(int points) {
        if (this.usrSnapshot == null) {
            return;
        }

        this.usrSnapshot.addToPoints(points);
        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put("/users/" + getUid(), this.usrSnapshot.toMap());
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
        usrSnapshot = null;

        // detach listener from previos user
        if (this.currentUserRef != null && this.currentUserListener != null)
            currentUserRef.removeEventListener(this.currentUserListener);

        notifyObservers();
    }

    public User getUserSnapshot() {
        return usrSnapshot;
    }

    /**
     * Updates and {@field usrSnapshot} and notifies all Observers
     * @param usr
     */
    private void setUserSnapshot(User usr){
        this.usrSnapshot = usr;
        this.setChanged();
        UserInformation.shared.notifyObservers();
    }

    /**
     * Tries to authenticate with Firebase and fetches the user's meta-data from Firebase.
     * @return true if authentication succeeded.
     */
    public boolean tryAuthenticate() {
        //Log.d(TAG,"tryAuthenticate;" );
        FirebaseUser fbuser = mAuth.getCurrentUser();
        if (fbuser == null ) return false; // not logged in yet.

        switchUser(fbuser);
        return true;
    }

    private void switchUser(FirebaseUser newUsr) {
        // lets see if the user has changed
        if (this.firebaseUsr != null)
            if (this.firebaseUsr.equals(newUsr))
                return; // user is already logged in, no need to fetch userData
            else

        this.firebaseUsr = newUsr;
        this.startListenToUser(newUsr.getUid());
    }

    private void startListenToUser(final String uid){
        //Log.d(TAG,"startListenToUser; uid=" + uid);

        // detach listener from previos user
        if (this.currentUserRef != null && this.currentUserListener != null)
            currentUserRef.removeEventListener(this.currentUserListener);

        // attach listener to current user
        if (this.currentUserListener == null)
            this.currentUserListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    User user = dataSnapshot.getValue(User.class);
                    user.uid = uid;
                    UserInformation.shared.setUserSnapshot(user);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.e(TAG,"Failed to fetch user-info from Firebase.");
                    Log.e(TAG,databaseError.getDetails());
                    UserInformation.shared.setUserSnapshot(null); // this snapshot is no longer valid
                }
            };

        this.currentUserRef = FirebaseDatabase.getInstance().getReference("users").child(uid);
        this.currentUserRef.addValueEventListener(currentUserListener);
    }
}
