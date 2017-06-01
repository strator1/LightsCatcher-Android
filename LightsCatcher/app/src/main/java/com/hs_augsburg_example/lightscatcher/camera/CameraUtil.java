package com.hs_augsburg_example.lightscatcher.camera;

import android.Manifest;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.hardware.Camera;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Size;

import com.hs_augsburg_example.lightscatcher.utils.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * See here https://github.com/commonsguy/cwac-camera/blob/master/camera/src/com/commonsware/cwac/camera/CameraUtils.java
 */

public class CameraUtil {
    private static final String TAG = "CameraUtil";
    private static final int MAX_PICTURE_SIZE = 2048;

    /**
     * Tests the app and the device to confirm that the code
     * in this library should work. This is called automatically
     * by other classes (e.g., CameraActivity), and so you probably
     * do not need to call it yourself. But, hey, it's a public
     * method, so call it if you feel like it.
     * <p>
     * The method will throw an IllegalStateException if the
     * environment is unsatisfactory, where the exception message
     * will tell you what is wrong.
     *
     * @param ctxt any Context will do
     */
    public static void validateEnvironment(Context ctxt,
                                           boolean failIfNoPermissions) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            throw new IllegalStateException("App is running on device older than API Level 14");
        }

        PackageManager pm = ctxt.getPackageManager();

        if (!pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) &&
                !pm.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            throw new IllegalStateException("App is running on device that lacks a camera");
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && failIfNoPermissions) {
            if (ctxt.checkSelfPermission(Manifest.permission.CAMERA) !=
                    PackageManager.PERMISSION_GRANTED) {
                throw new IllegalStateException("We do not have the CAMERA permission");
            }
        }
    }

    /**
     * Algorithm for determining if the system bar is on the
     * bottom or right. Based on implementation of PhoneWindowManager.
     * Pray that it holds up.
     *
     * @param ctxt any Context will do
     * @return true if the system bar should be on the bottom in
     * the current configuration, false otherwise
     */
    public static boolean isSystemBarOnBottom(Context ctxt) {
        Resources res = ctxt.getResources();
        Configuration cfg = res.getConfiguration();
        DisplayMetrics dm = res.getDisplayMetrics();
        boolean canMove = (dm.widthPixels != dm.heightPixels &&
                cfg.smallestScreenWidthDp < 600);

        return (!canMove || dm.widthPixels < dm.heightPixels);
    }

    public static Camera.Size getLargestPictureSize(Camera.Parameters descriptor) {
        Camera.Size result = null;

        for (Camera.Size size : descriptor.getSupportedPictureSizes()) {
            if (result == null) {
                result = size;
            } else {
                int resultArea = result.width * result.height;
                int newArea = size.width * size.height;

                if (newArea > resultArea) {
                    result = size;
                }
            }
        }

        return (result);
    }

    public static Camera.Size getMediumPictureSize(Camera.Parameters descriptor) {
        Camera.Size result = null;


        List<Camera.Size> supportedSizes = descriptor.getSupportedPictureSizes();
        Camera.Size size1 = getPictureSizeWidthSmaller(1280, supportedSizes);
        Collections.reverse(supportedSizes);
        Camera.Size size2 = getPictureSizeWidthSmaller(1280, supportedSizes);

        return size1.width >= size2.width ? size1 : size2;
    }

    public static Camera.Size getSmallestPictureSize(Camera.Parameters descriptor) {
        Camera.Size result = null;

        for (Camera.Size size : descriptor.getSupportedPictureSizes()) {
            if (result == null) {
                result = size;
            } else {
                int resultArea = result.width * result.height;
                int newArea = size.width * size.height;

                if (newArea < resultArea) {
                    result = size;
                }
            }
        }

        return (result);
    }

    public static Camera.Size getPictureSizeWidthSmaller(int maxWidth, List<Camera.Size> supportedSizes) {
        Camera.Size result = null;

        for (Camera.Size size : supportedSizes) {
            if (result == null) {
                result = size;
            }

            if (size.width <= maxWidth) {
                return size;
            }
        }

        return result;
    }

    /**
     * Given {@code choices} of {@code Size}s supported by a camera, chooses the biggest one whose
     * width and height are smaller or equal to {@Code maxH} and {@Code maxW}, and whose aspect
     * ratio matches with the specified value. If aspect ratio does not match, chooses the element
     * whose aspect ratio differs as less as possible
     *
     * @param choices The list of sizes that the camera supports for the intended output class
     * @return The optimal {@code Size}
     */
    public static Camera.Size chooseOptimalSize(List<Camera.Size> choices, int preferedW, int preferedH, int maxW, int maxH) {
        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Camera.Size> bigEnough = new ArrayList<Camera.Size>();

        double preferredAspect = (double) preferedH / preferedW;
        long preferredArea = preferedH * preferedW;

        if (Log.ENABLED)
            Log.d(TAG, "expecting aspect: {0}, pref: {3} x {4}, maximum: {1} x {2}", preferredAspect, maxW, maxH, preferedW, preferedH);

        // ensure that collection is sorted in equal direction on each device
        Collections.sort(choices, new CompareSizesByArea());

        double bestAspectDiff = Double.MAX_VALUE;
        double bestAreaDiff = Double.MAX_VALUE;
        Camera.Size bestAspectMatch = null;
        Camera.Size bestAreaMatch = null;

        for (Camera.Size option : choices) {
            if (option.height > maxH || option.width > maxW) {
                break; // max size reached
            }

            double areaDiff = Math.abs((area(option) - preferredArea));
            if (areaDiff < bestAreaDiff) {
                bestAreaDiff = areaDiff;
                bestAreaMatch = option;
            }

            double diff = Math.abs(aspect(option) - preferredAspect);
            if (diff <= bestAspectDiff) {
                bestAspectDiff = diff;
                bestAspectMatch = option;
            }
            if (Log.ENABLED) {
                double d = aspect(option);
                Log.d(TAG, "\t{0} x {1}; {2}; diff: {3} / {4}", option.width, option.height, d, diff, areaDiff);
            }
        }
        Log.d(TAG, "bestAspectDiff: " + bestAspectDiff);
        Log.d(TAG, "bestAreaDiff: " + bestAreaDiff);
        // prefer best matching aspect but only if the area difference is not too big
        if (bestAspectMatch != null && (double) area(bestAspectMatch) / preferredArea >= (.1)) {
            return bestAspectMatch;
        } else if (bestAreaMatch != null) {
            return bestAreaMatch;
        } else {
            Log.e("CAM", "Couldn't find any suitable size");
            return getPictureSizeWidthSmaller(maxW, choices);
        }
    }

    static long area(Camera.Size size) {
        return (long) size.width * size.height;
    }

    static double aspect(Camera.Size size) {
        return (double) size.height / size.width;
    }

    static double aspect(int h, int w) {
        return (double) h / w;
    }


    static class CompareSizesByArea implements Comparator<Camera.Size> {

        @Override
        public int compare(Camera.Size lhs, Camera.Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.width * lhs.height -
                    (long) rhs.width * rhs.height);
        }

    }
}
