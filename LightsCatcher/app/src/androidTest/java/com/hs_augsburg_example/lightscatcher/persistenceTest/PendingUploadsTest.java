package com.hs_augsburg_example.lightscatcher.persistenceTest;

import android.content.Context;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.google.firebase.storage.UploadTask;
import com.hs_augsburg_example.lightscatcher.singletons.PersistenceManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.UUID;

import static junit.framework.Assert.*;

/**
 * Created by Quirin on 12.05.2017.
 */

@RunWith(AndroidJUnit4.class)
public class PendingUploadsTest extends PersistenceManagerTest {
    private static final String TAG = "PendingUploadsTest";

    private String testfile;

    @Before
    public void setup() throws InterruptedException, IOException {
        Log.d(TAG, "setup");
        super.setup();

        // create dummy file to simulate that there are backup images
        testfile = UUID.randomUUID().toString().toUpperCase();
        Log.d(TAG, "writing dummy-file: " + testfile);

        File dir = appContext.getDir(PersistenceManager.INTERNAL_IMG_PATH, Context.MODE_PRIVATE);
        File file = new File(dir, testfile);
        file.createNewFile();
        FileOutputStream stream = new FileOutputStream(file);
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(stream);
        outputStreamWriter.write("Congratulations! You have found a useless test-file. :)");
        outputStreamWriter.close();

        Thread.sleep(1000);
    }

    @Test
    public void resumePendingUploads() throws InterruptedException {

        UploadTask[] result = PersistenceManager.shared.retryPendingUploads(appContext);
        super.registerFirebaseTask(result);

        // wait until upload finished
        Thread.sleep(5000);

        assertEquals("no tasks have been started!",1, result.length);
    }

    @Test
    public void resumePendingUploadsMulti() throws InterruptedException {
        UploadTask[] result = PersistenceManager.shared.retryPendingUploads(appContext);
        super.registerFirebaseTask(result);
        assertTrue(result.length == 1);

        // should not start a second time
        result = PersistenceManager.shared.retryPendingUploads(appContext);
        super.registerFirebaseTask(result);

        assertEquals(0,result.length);

        //call many many times
        for (int i = 0; i < 500; i++) {
            Thread.sleep(10);
            result = PersistenceManager.shared.retryPendingUploads(appContext);
            super.registerFirebaseTask(result);
            assertEquals(0,result.length);
        }
    }

    @After
    @Override
    public void teardown() throws InterruptedException {
        super.teardown();
    }
}
