package com.hs_augsburg_example.lightscatcher.camera;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.hs_augsburg_example.lightscatcher.R;
import com.hs_augsburg_example.lightscatcher.camera.utils.CameraPreview;
import com.hs_augsburg_example.lightscatcher.dataModels.LightPosition;
import com.hs_augsburg_example.lightscatcher.dataModels.PhotoInformation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class TakePictureActivity extends AppCompatActivity{

    private static Camera cam;
    private CameraPreview camPreview;

    private RelativeLayout rl;
    private View crosshairView;

    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
            bmp = rotateImage(bmp, 90);

            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) crosshairView.getLayoutParams();
            LightPosition pos = new LightPosition(new View(getApplicationContext()), getApplicationContext());
            pos.setX(params.leftMargin + pos.getWidth()/2);
            pos.setY(params.topMargin + pos.getHeight()/2);
            pos.setMostRelevant(true);

            PhotoInformation.shared.resetLightPositions();
            PhotoInformation.shared.addToLightPosition(pos);
            PhotoInformation.shared.setImage(bmp);

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

            cam.setDisplayOrientation(90);

            camPreview = new CameraPreview(this, cam);
            FrameLayout preview = (FrameLayout) findViewById(R.id.take_picture_cameraPreview);

            preview.addView(camPreview);
        };

        rl = (RelativeLayout) findViewById(R.id.take_picture_rl);

        addCrosshairView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        camPreview.getCamera().startPreview();
    }

    @Override
    protected void onPause() {
        super.onPause();
        camPreview.getCamera().stopPreview();
    }

    private void addCrosshairView() {
        float density = getApplicationContext().getResources().getDisplayMetrics().density;
        int viewHeight = (int) (90 * density);
        int viewWidth = (int) (90 * density);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int maxHeight = (displayMetrics.heightPixels / 2) - viewHeight;
        int maxWidth = displayMetrics.widthPixels - viewWidth;

        LayoutInflater inflater = this.getLayoutInflater();
        crosshairView = inflater.inflate(R.layout.view_camera_crosshair, null);

        Random r = new Random();
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(viewHeight, viewWidth);
        params.leftMargin = r.nextInt(maxWidth - 0) + 0;
        params.topMargin = r.nextInt(maxHeight - (int) (40 * density)) + (int) (40 * density);

        rl.addView(crosshairView, params);
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
