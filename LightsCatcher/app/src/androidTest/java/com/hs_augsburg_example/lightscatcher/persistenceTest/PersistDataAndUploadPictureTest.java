package com.hs_augsburg_example.lightscatcher.persistenceTest;

import android.graphics.Bitmap;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.hs_augsburg_example.lightscatcher.dataModels.LightPhase;
import com.hs_augsburg_example.lightscatcher.dataModels.LightPosition;
import com.hs_augsburg_example.lightscatcher.dataModels.Photo;
import com.hs_augsburg_example.lightscatcher.singletons.PersistenceManager;
import com.hs_augsburg_example.lightscatcher.singletons.UserInformation;
import com.hs_augsburg_example.lightscatcher.utils.TaskMonitor;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Observable;
import java.util.Observer;
import java.util.Random;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * Created by Quirin on 13.05.2017.
 */

@RunWith(AndroidJUnit4.class)
public class PersistDataAndUploadPictureTest extends PersistenceManagerTest {
    private static final String TAG = "UploadPictureTest";
    private Photo data;

    private static Bitmap randomBitmap(int w, int h) {
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ALPHA_8);
        Random rnd = new Random();
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                bmp.setPixel(x, y, rnd.nextInt());
            }
        }
        return bmp;
    }

    private static Photo testData() {
        return new Photo().buildNew()
                .setCurrentUser()
                .setBitmap(randomBitmap(32, 32))
                .setGyro(3.2f)
                .setLocation(10.4, 8.7)
                .addLightPos(new LightPosition(.4, .7, LightPhase.GREEN, true))
                .setCredits(3)
                .commit();
    }

    private String testFile;
    private Bitmap bmp;

    @Before
    @Override
    public void setup() throws InterruptedException, IOException {
        Log.d(TAG, "setup");
        super.setup();
        this.data = testData();
    }

    @Test
    public void persistDataAndUploadPicture() throws InterruptedException {
        Log.d(TAG, "persistDataAndUploadPicture");
        assertTrue(PersistenceManager.shared.backupStorage.list(appContext).length == 0);

        int scoreBefore = UserInformation.shared.getUserSnapshot().points;
        final int[] backupFiles = new int[]{-1};
        PersistenceManager.shared.backupStorage.addObserver(new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                if (backupFiles[0] == -1)// set only once at the start
                    backupFiles[0] = PersistenceManager.shared.backupStorage.list(appContext).length;
            }
        });

        final int[] uploadCount = new int[]{-1};

        PersistenceManager.shared.uploadMonitor.addObserver(new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                if (uploadCount[0] == -1)// set only once at the start
                    uploadCount[0] = PersistenceManager.shared.uploadMonitor.countActiveTasks();
            }
        });

        TaskMonitor tasks = PersistenceManager.shared.persistDataAndUploadPicture(appContext, data);
        for (TaskMonitor.Tuple task : tasks.list()) {
            super.registerFirebaseTask((Task<?>) task.v2);
        }


        // see if backup file was created
        assertEquals("unexpected count of backup files!", 1, backupFiles[0]);

        // see if upload count was updated
        assertEquals("unexpected count of uploads!", 1, uploadCount[0]);

        // all tasks should succeed
        assertAllTasksSucceed();

        // check if user's score was updated
        assertEquals("unexpected points of user!", scoreBefore + data.credits, UserInformation.shared.getUserSnapshot().points);
    }

    @After
    @Override
    public void teardown() throws InterruptedException {
        super.teardown();
    }
}
