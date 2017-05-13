package com.hs_augsburg_example.lightscatcher.persistenceTest;

import android.graphics.Bitmap;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.google.firebase.storage.UploadTask;
import com.hs_augsburg_example.lightscatcher.singletons.PersistenceManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Observable;
import java.util.Observer;
import java.util.Random;
import java.util.UUID;

import static junit.framework.Assert.assertTrue;

/**
 * Created by Quirin on 13.05.2017.
 */

@RunWith(AndroidJUnit4.class)
public class UploadImageTest extends PersistenceManagerTest {
    private static final String TAG = "UploadImageTest";

    private String testFile;
    private Bitmap bmp;

    @Before
    @Override
    public void setup() throws InterruptedException, IOException {
        Log.d(TAG, "setup");
        super.setup();
        testFile = UUID.randomUUID().toString().toUpperCase();
        bmp = randomBitmap(32, 32);
    }

    private Bitmap randomBitmap(int w, int h) {
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ALPHA_8);
        Random rnd = new Random();
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                bmp.setPixel(x, y, rnd.nextInt());
            }
        }
        return bmp;
    }

    @Test
    public void persistLightsImageAndUpload() {
        assertTrue(PersistenceManager.shared.backupStorage.list(appContext).length == 0);

        final int[] updated = new int[]{0};
        PersistenceManager.shared.backupStorage.addObserver(new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                updated[0] = PersistenceManager.shared.backupStorage.list(appContext).length;
            }
        });

        UploadTask task = PersistenceManager.shared.persistLightsImage(appContext, testFile, bmp);
        super.registerStorageTask(task);
        assertTrue(updated[0] == 1);
    }

}
