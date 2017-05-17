package com.hs_augsburg_example.lightscatcher.utils;

import android.content.Context;
import android.support.annotation.NonNull;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by quirin on 30.04.17.
 */

public class TaskMonitor implements OnSuccessListener, OnFailureListener {
    private static final String TAG = "TaskMonitor";
    private static final boolean LOG = Log.ENABLED && true;

    private static TaskMonitor mostRecent;
    private static ArrayList<Tuple<String, Task<?>>> pendingTasks = new ArrayList<>();
    private final Context ctx;

    private TaskMonitor(Context ctx) {
        this.ctx = ctx;
    }

    public static TaskMonitor getMostRecent() {
        return mostRecent;
    }

    public static TaskMonitor newInstance(Context ctx) {
        TaskMonitor m = new TaskMonitor(ctx);
        mostRecent = m;
        return m;
    }

    public Task<?> addTask(String description, Task<?> t) {
        if (t != null) {
            t.addOnSuccessListener(this);
            t.addOnFailureListener(this);
        }
        pendingTasks.add(new Tuple<String, Task<?>>(description, t));
        return t;
    }

    public List<Tuple<String, Task<?>>> getAllTasks() {
        return pendingTasks;
    }

    void onSuccessAll() {
        Toast.makeText(ctx, "Upload erfolgreich :)", Toast.LENGTH_LONG).show();
    }

    public Tuple<String, Task<?>>[] list() {
        Tuple<String, Task<?>>[] array = new Tuple[pendingTasks.size()];
        pendingTasks.toArray(array);
        return array;
    }

    @Override
    public void onSuccess(Object o) {
        synchronized (this) {
            boolean all = pendingTasks.size() > 0;
            for (Tuple<String, Task<?>> t : pendingTasks) {
                if (t.v2 != null && !t.v2.isSuccessful()) {
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
        if (LOG) Log.e(TAG, e.getMessage(), e);
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
