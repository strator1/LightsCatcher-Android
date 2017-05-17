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
import com.hs_augsburg_example.lightscatcher.singletons.UserInformation;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static junit.framework.Assert.fail;

/**
 * Created by Quirin on 13.05.2017.
 */

class PersistenceManagerTest {
    public static final String TEST_USER_MAIL = "lightscatcher2017@gmail.com";
    public static final String TEST_PWD = "ampelfehlalarm2017";
    private static final String TAG = "PersistenceManagerTest";

    protected Context appContext;
    protected final List<Task<?>> tasks = new ArrayList<>();

    @Before
    public void setup() throws InterruptedException, IOException {
        Log.d(TAG, "setup");
        appContext = InstrumentationRegistry.getTargetContext();
        PersistenceManager.init();
        PersistenceManager.shared.backupStorage.clean(appContext);
        FirebaseAuth.getInstance().signInWithEmailAndPassword(TEST_USER_MAIL, TEST_PWD).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (!task.isSuccessful())
                    Log.e(TAG, "authentication with test-user failed");
            }
        });

        // wait for firebase connection and authentication
        Thread.sleep(500);

        int i = 0;
        while (i < 10) {
            if (UserInformation.shared.isLoggedIn() && PersistenceManager.shared.connectedMonitor.isConnected())
                return;
            else {
                Thread.sleep(200);
                i++;
            }
        }
        fail("failed to login or connect to firebase");
    }

    protected void registerFirebaseTask(Task<?> task) {
        tasks.add(task);
    }

    public void registerFirebaseTask(Task<?>[] result) {
        tasks.addAll(Arrays.asList(result));
    }

    @After
    public void teardown() throws InterruptedException {
        Log.d(TAG, "teardown");
        PersistenceManager.shared.backupStorage.clean(appContext);
        for (Task<?> task : tasks) {
            if (task instanceof StorageTask<?>) {
                ((StorageTask<UploadTask.TaskSnapshot>) task).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        Log.d(TAG, "removing storage " + task.getResult().getStorage());
                        task.getResult().getStorage().delete();
                    }
                });
            }
        }
        // wait until all uploads complete
        Thread.sleep(5000);
    }
}
