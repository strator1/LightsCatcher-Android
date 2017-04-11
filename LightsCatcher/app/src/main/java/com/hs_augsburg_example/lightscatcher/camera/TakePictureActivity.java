package com.hs_augsburg_example.lightscatcher.camera;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.FrameLayout;

import com.hs_augsburg_example.lightscatcher.R;
import com.hs_augsburg_example.lightscatcher.camera.utils.CameraPreview;
import com.hs_augsburg_example.lightscatcher.dataModels.PhotoInformation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class TakePictureActivity extends AppCompatActivity{

    private static Camera cam;
    private CameraPreview camPreview;

    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
            Bitmap rotated = rotateImage(bmp, 90);
            PhotoInformation.shared.setImage(rotated);

            onAfterPictureTaken();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_take_picture);

        if (checkCameraHardware(getApplicationContext())) {
            cam = getCameraInstance();
            Camera.Parameters params = cam.getParameters();

            List<Camera.Size> sizes = params.getSupportedPictureSizes();

            for (Camera.Size s: sizes) {
                System.out.println("Available resolutions: " + s.width + ", " + s.height);
            }

            /*for (Camera.Size s: sizes) {
                if (s.width >= 600 && s.width <= 800) {
                    params.setPictureSize(s.width, s.height);
                    break;
                }
            }*/

            cam.setParameters(params);
            cam.setDisplayOrientation(90);

            camPreview = new CameraPreview(this, cam);
            FrameLayout preview = (FrameLayout) findViewById(R.id.take_picture_cameraPreview);

            preview.addView(camPreview);
        };
    }

    private static Camera.Size chooseOptimalSize(List<Camera.Size> choices, int width, int height) {
        List<Camera.Size> bigEnough = new ArrayList<Camera.Size>();
        for(Camera.Size option : choices) {
            if(option.height == option.width * height / width &&
                    option.width >= width && option.height >= height) {
                bigEnough.add(option);
            }
        }
        if(bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizeByArea());
        } else {
            return choices.get(0);
        }
    }

    private static class CompareSizeByArea implements Comparator<Camera.Size> {

        @Override
        public int compare(Camera.Size lhs, Camera.Size rhs) {
            return Long.signum( (long)(lhs.width * lhs.height) -
                    (long)(rhs.width * rhs.height));
        }
    }

    public void onCaptureButtonPressed(View view) {
        cam.takePicture(null, null, mPicture);
    }

    private void onAfterPictureTaken() {
        Intent intent = new Intent(this, TagLightsActivity.class);
        startActivity(intent);
    }

    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }

    /** A safe way to get an instance of the Camera object. */
    private Camera getCameraInstance(){
        if (cam != null) {
            return cam;
            /*cam.release();
            cam = null;*/
        }

        try {
            cam = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
            e.printStackTrace();
        }
        return cam; // returns null if camera is unavailable
    }

    public void navigateBack(View view) {

    }

    public static Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
                matrix, true);
    }
}
