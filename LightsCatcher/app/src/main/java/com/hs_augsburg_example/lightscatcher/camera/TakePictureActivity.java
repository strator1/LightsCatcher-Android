package com.hs_augsburg_example.lightscatcher.camera;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.hs_augsburg_example.lightscatcher.R;
import com.hs_augsburg_example.lightscatcher.camera.utils.CameraPreview;
import com.hs_augsburg_example.lightscatcher.singletons.LightInformation;
import com.hs_augsburg_example.lightscatcher.services.LocationService;
import com.hs_augsburg_example.lightscatcher.services.MotionService;
import com.hs_augsburg_example.lightscatcher.singletons.PhotoInformation;
import com.hs_augsburg_example.lightscatcher.utils.UserPreference;

import java.util.List;
import java.util.Random;

public class TakePictureActivity extends AppCompatActivity{

    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private static final String FRAGMENT_DIALOG = "dialog";

    private static Camera cam;
    private CameraPreview camPreview;

    SensorManager mSensorManager;
    Sensor accelerometer;
    Sensor magnetometer;
    Sensor vectorSensor;
    MotionService motionService;

    private LocationService locationService;



    private RelativeLayout rl;
    private View crosshairView;

    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
            bmp = rotateImage(bmp, 90);

            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) crosshairView.getLayoutParams();
            LightInformation pos = new LightInformation(new View(getApplicationContext()), getApplicationContext());
            pos.setX(params.leftMargin + pos.getWidth()/2);
            pos.setY(params.topMargin + pos.getHeight()/2);
            pos.setMostRelevant(true);

            if (locationService.getLocation() != null) {
                PhotoInformation.shared.setLocation(locationService.getLocation());
            }

            PhotoInformation.shared.resetLightPositions();
            PhotoInformation.shared.addToLightPosition(pos);
            PhotoInformation.shared.setImage(bmp);
            PhotoInformation.shared.setGyroPosition(motionService.getPitch());

            onAfterPictureTaken();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_take_picture);

        rl = (RelativeLayout) findViewById(R.id.take_picture_rl);

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        motionService = new MotionService();
        locationService = new LocationService(getApplicationContext());

        addCrosshairView();
    }

    private boolean permissionGranted = false;
    @Override
    protected void onResume() {
        super.onResume();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {

            permissionGranted = true;
            if (camPreview == null) {
                setupCamera();
            }

        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {
            permissionGranted = false;
        } else {
            permissionGranted = false;
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION);
        }

        if (locationService.hasPermission()) {
            locationService.startListening();
        }

        if (UserPreference.isFirstCapture(getApplicationContext())) {
            onInfoButtonPressed(null);
        }

        mSensorManager.registerListener(motionService.getEventListener(), accelerometer, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(motionService.getEventListener(), magnetometer, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (camPreview != null && camPreview.getCamera() != null) {
            camPreview.getCamera().stopPreview();
        }

        locationService.stopListening();
        mSensorManager.unregisterListener(motionService.getEventListener());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        locationService.stopListening();
        mSensorManager.unregisterListener(motionService.getEventListener());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CAMERA_PERMISSION:
                if (permissions.length != 1 || grantResults.length != 1) {
                    throw new RuntimeException("Error on requesting camera permission.");
                }
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "No permission!",
                            Toast.LENGTH_SHORT).show();
                } else {
                    if (!locationService.hasPermission()) {
                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                2);
                    }
                }
                // No need to start camera here; it is handled by onResume
                break;
        }
    }

    private void setupCamera() {
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
        if (!permissionGranted) {
            return;
        }

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

    private AlertDialog pictureHelpDialog;

    public void onInfoButtonPressed(View view) {
        if (pictureHelpDialog == null) {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder.setTitle("Erste Hilfe");
            dialogBuilder.setMessage("Bringe die momentan relevante Ampel ins Fadenkreuz und drücke den Auslöser");

            dialogBuilder.setPositiveButton("Verstanden", null);

            pictureHelpDialog = dialogBuilder.create();
        }

        pictureHelpDialog.show();
    }

    public static Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
                matrix, true);
    }


}
