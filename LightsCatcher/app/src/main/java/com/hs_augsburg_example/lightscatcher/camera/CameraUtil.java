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
            if (Log.ENABLED)
                Log.d("CAM", "\tpicSize: {0}x{1}; {2}", size.width, size.height, (double) size.height / size.width);

            if (result == null) {
                result = size;
            }

            if (size.width <= maxWidth) {
                return size;
            }
        }

        return result;
    }

    // based on https://github.com/googlesamples/android-Camera2Basic/blob/master/Application/src/main/java/com/example/android/camera2basic/Camera2BasicFragment.java

    /**
     * Given {@code choices} of {@code Size}s supported by a camera, chooses the smallest one whose
     * width and height are at least as large as the respective requested values, and whose aspect
     * ratio matches with the specified value.
     *
     * @param aspectRatio The aspect ratio
     * @param choices     The list of sizes that the camera supports for the intended output class
     * @param width       The minimum desired width
     * @param height      The minimum desired height
     * @param aspect
     * @return The optimal {@code Size}, or an arbitrary one if none were big enough
     */
    public static Camera.Size chooseOptimalSize(List<Camera.Size> choices, int width, int height, final double aspect) {
        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Camera.Size> bigEnough = new ArrayList<Camera.Size>();


        if (Log.ENABLED)
            Log.d("CAM", "expecting aspect: {0}, minimum: {1}x{2}; {3}", aspect, width, height, (double) height / width);
        for (Camera.Size option : choices) {
            if (Log.ENABLED) {
                double d = (double) option.height / option.width;
                Log.d("CAM", "\t{0}x{1}; {2}", option.width, option.height, d);
            }
            if (option.height == option.width * aspect &&
                    option.width >= width && option.height >= height) {
                bigEnough.add(option);
            }
        }

        // Pick the smallest of those, assuming we found any
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else {
            Log.e("CAM", "Couldn't find any suitable size");
            return Collections.max(choices, new CompareSizesByArea());
        }
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
