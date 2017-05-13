package com.hs_augsburg_example.lightscatcher.persistenceTest;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;
import com.hs_augsburg_example.lightscatcher.singletons.PersistenceManager;

import org.junit.After;
import org.junit.Before;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Created by Quirin on 13.05.2017.
 */

class PersistenceManagerTest {
    public static final String TEST_USER = "lightscatcher2017@gmail.com";
    public static final String TEST_PWD = "ampelfehlalarm2017";
    private static final String TAG = "PersistenceManagerTest";

    protected Context appContext;
    protected final List<UploadTask> tasks = new ArrayList<>();

    @Before
    public void setup() throws InterruptedException, IOException {
        Log.d(TAG, "setup");
        appContext = InstrumentationRegistry.getTargetContext();
        PersistenceManager.shared.backupStorage.clean(appContext);
        FirebaseAuth.getInstance().signInWithEmailAndPassword(TEST_USER, TEST_PWD).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (!task.isSuccessful())
                    Log.e(TAG, "authentication with test-user failed");
            }
        });

        // wait for authentication
        Thread.sleep(1000);
    }

    protected void registerStorageTask(UploadTask task) {
        tasks.add(task);
    }

    public void registerStorageTask(UploadTask[] result) {
        tasks.addAll(Arrays.asList(result));
    }

    @After
    public void teardown() throws InterruptedException {
        Log.d(TAG, "teardown");
        PersistenceManager.shared.backupStorage.clean(appContext);
        for (UploadTask task : tasks) {
            task.addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                    if (task.isSuccessful()){
                        Log.d(TAG, "removing storage " + task.getResult().getStorage());
                        //task.getResult().getStorage().delete();
                    }
                    else{
                        Log.e(TAG, "upload task not successful: " + task.getResult().getStorage(), task.getException());
                    }
                }
            });
        }
        Thread.sleep(4000);
    }

}
