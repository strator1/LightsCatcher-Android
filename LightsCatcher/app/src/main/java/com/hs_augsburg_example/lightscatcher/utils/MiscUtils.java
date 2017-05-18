package com.hs_augsburg_example.lightscatcher.utils;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.util.DisplayMetrics;

import java.util.Random;

/**
 * Created by Quirin on 11.05.2017.
 */

public class MiscUtils {
    public static float randomFloat(Random random,float min, float max) {
        final double d = max - min;
        return (float) (min + d * random.nextDouble());
    }

    public static <T> int indexOf(T[] array, T elem) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == elem) {
                return i;
            }
        }
        return -1;
    }

    public static Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
                matrix, true);
    }

    /**
     * calculates device-independent dimensions in actual pixels
     * @param value
     * @param density
     * @return
     */
    public static int dp(int value, float density) {
        return (int) (value * density);
    }

}
