package com.hs_augsburg_example.lightscatcher;

import com.hs_augsburg_example.lightscatcher.activities_major.TakePictureActivity;
import com.hs_augsburg_example.lightscatcher.utils.MiscUtils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */

@RunWith(Parameterized.class)
public class ExampleUnitTest {

    private float max;
    private float min;

    public ExampleUnitTest(float pMin, float pMax) {
        max = pMax;
        min = pMin;
    }

    @Test
    public void randomFloat() throws Exception {
        for (int i = 0; i < 1000; i++) {
            float rnd = MiscUtils.randomFloat(min, max);

            assertEquals(true, rnd >= min);
            assertEquals(true, rnd <= max);

            System.out.format("%1$f <= %2$f <= %3$f\n", min, rnd, max);
        }
    }

    @Parameterized.Parameters
    public static Collection primeNumbers() {
        return Arrays.asList(new Object[][]{
                {TakePictureActivity.CrosshairManager.CROSSHAIR_X_MIN, TakePictureActivity.CrosshairManager.CROSSHAIR_X_MAX},
                {TakePictureActivity.CrosshairManager.CROSSHAIR_Y_MIN, TakePictureActivity.CrosshairManager.CROSSHAIR_Y_MAX},
                {0f, 1f},
                {0f, .7f},
                {.3f, 1f},
                {.3f, .300001f },
        });
    }
}