package com.hs_augsburg_example.lightscatcher.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.util.DisplayMetrics;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by Quirin on 11.05.2017.
 */

public class MiscUtils {
    static  final Random random = new Random();

    public static float randomFloat( float min, float max) {
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
     *
     * @param value
     * @return
     */
    public static int dp(Context ctx, int value) {
        float density = ctx.getResources().getDisplayMetrics().density;
        return (int) (value * density);
    }
    /**
     * calculates device-independent dimensions in actual pixels
     *
     * @param value
     * @param density
     * @return
     */
    public static int dp(int value, float density) {
        return (int) (value * density);
    }

    public static <T> ArrayList<T> newArrayList(T... params) {
        ArrayList<T> objects = new ArrayList<>();
        for (T elem : params) {
            objects.add(elem);
        }
        return objects;
    }

}
