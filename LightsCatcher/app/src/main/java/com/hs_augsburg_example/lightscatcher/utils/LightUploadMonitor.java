package com.hs_augsburg_example.lightscatcher.utils;

import android.content.Context;
import android.support.annotation.NonNull;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by quirin on 30.04.17.
 */

public class LightUploadMonitor implements OnSuccessListener, OnFailureListener {
    private static final String TAG = "LightUploadMonitor";
    private static final boolean LOG = Log.ENABLED && true;

    private static LightUploadMonitor mostRecent;
    private static ArrayList<Tuple<String, Task<?>>> pendingTasks = new ArrayList<>();
    private final Context ctx;

    private LightUploadMonitor(Context ctx) {
        this.ctx = ctx;
    }

    public static LightUploadMonitor getMostRecent() {
        return mostRecent;
    }

    public static LightUploadMonitor newInstance(Context ctx) {
        LightUploadMonitor m = new LightUploadMonitor(ctx);
        mostRecent = m;
        return m;
    }

    public Task<?> addTask(String description, Task<?> t) {
        t.addOnSuccessListener(this);
        t.addOnFailureListener(this);
        pendingTasks.add(new Tuple<String, Task<?>>(description, t));
        return t;
    }

    public List<Tuple<String, Task<?>>> getAllTasks() {
        return pendingTasks;
    }

    void onSuccessAll() {
        Toast.makeText(ctx, "Upload erfolgreich :)", Toast.LENGTH_LONG).show();
    }


    @Override
    public void onSuccess(Object o) {
        synchronized (this) {
            boolean all = pendingTasks.size() > 0;
            for (Tuple<String, Task<?>> t : this.pendingTasks) {
                if(LOG) Log.d(TAG, System.currentTimeMillis() + ": " + t.v1 + ": " + t.v2.isSuccessful());
                if (!t.v2.isSuccessful()) {
                    all = false;
                    break;
                }
            }
            if (all)
                onSuccessAll();
        }
    }

    @Override
    public void onFailure(@NonNull Exception e) {
        Toast.makeText(ctx, "Beim Upload ist leider ein Fehler aufgetreten :(", Toast.LENGTH_LONG).show();
        if(LOG) Log.e(TAG,e.getMessage(),e);
    }

    public static class Tuple<T1, T2> {
        public final T1 v1;
        public final T2 v2;

        private Tuple(T1 v1, T2 v2) {
            this.v1 = v1;
            this.v2 = v2;
        }
    }
}
