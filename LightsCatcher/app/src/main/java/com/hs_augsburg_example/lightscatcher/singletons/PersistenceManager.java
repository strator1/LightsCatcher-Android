package com.hs_augsburg_example.lightscatcher.singletons;

import android.graphics.Bitmap;
import android.media.Image;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.hs_augsburg_example.lightscatcher.dataModels.User;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Observable;
import java.util.UUID;

/**
 * Created by quirin on 26.04.17.
 */

public class PersistenceManager {
    public static final String DATA_MODEL_VERSION = "v1_0";
    public static final PersistenceManager shared = new PersistenceManager();

    public final ConnectedListener connectedListener;

    private final DatabaseReference root;
    private final DatabaseReference users;
    private final DatabaseReference lights;

    private final StorageReference lights_images;

    private PersistenceManager() {
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        root = FirebaseDatabase.getInstance().getReference();
        users = root.child("users");
        lights = root.child("lights");
        lights_images = FirebaseStorage.getInstance().getReference("lights_images");
        connectedListener = new ConnectedListener();
        FirebaseDatabase.getInstance().getReference(".info/connected").addValueEventListener(connectedListener);
    }

    public Task persist(User usr) {
        if (usr.uid == null) throw new IllegalArgumentException("User.uid was null.");
        return users.child(usr.uid).setValue(usr);
    }
/*
    public Task persist(Record rec) {
        if (rec.id == null) throw new IllegalArgumentException("Record.id was null.");
        return lights.child(rec.id).setValue(rec);
    }
*/
    public UploadTask persistStorage(String id, Bitmap bmp) {

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 40, baos);

        return lights_images.child("lights_images").child(id).putBytes(baos.toByteArray());
    }

    public class ConnectedListener extends Observable implements ValueEventListener {
        private boolean isConnected;

        public boolean isConnected(){
            return isConnected;
        }
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            this.isConnected = dataSnapshot.getValue(Boolean.class);
//            Log.d("APP", ".info/connected changed: " + isConnected);
            this.setChanged();
            this.notifyObservers();
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
            this.isConnected = false;
//            Log.d("APP", ".info/connected hanged: " + isConnected);
            this.setChanged();
            this.notifyObservers();
        }
    }

}
