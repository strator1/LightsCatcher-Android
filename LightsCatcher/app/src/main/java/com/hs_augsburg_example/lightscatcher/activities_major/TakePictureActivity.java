package com.hs_augsburg_example.lightscatcher.activities_major;

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
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.hs_augsburg_example.lightscatcher.R;
import com.hs_augsburg_example.lightscatcher.camera.utils.CameraPreview;
import com.hs_augsburg_example.lightscatcher.dataModels.LightPosition;
import com.hs_augsburg_example.lightscatcher.dataModels.Photo;
import com.hs_augsburg_example.lightscatcher.services.LocationService;
import com.hs_augsburg_example.lightscatcher.services.MotionService;
import com.hs_augsburg_example.lightscatcher.singletons.PhotoInformation;
import com.hs_augsburg_example.lightscatcher.utils.ActivityRegistry;
import com.hs_augsburg_example.lightscatcher.dataModels.LightPhase;
import com.hs_augsburg_example.lightscatcher.utils.UserPreference;

import java.util.List;
import java.util.Random;

/**
 * Activity to capture a traffic light. Up to 2 pictures can be taken in a row, one for each phase of the traffic light.
 */
public class TakePictureActivity extends AppCompatActivity implements Camera.PictureCallback {

    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private static final String FRAGMENT_DIALOG = "dialog";
    static final int PHASE_INIT = -1;
    public static final int PHASE_RED = 0;
    public static final int PHASE_GREEN = 1;

    private static Camera cam;
    private CameraPreview camPreview;

    SensorManager mSensorManager;
    Sensor accelerometer;
    Sensor magnetometer;
    MotionService motionService;

    private LocationService locationService;

    private RelativeLayout rl;
    private View crosshairView;
    private double crossHairX;
    private double crossHairY;
    private View[] cross = new View[2];

    // use PHASE_GREEN or PHASE_RED as index to access the corresponding snapshot
    // so index 0 is for red-snapshot, index 1 for green
    private final Photo[] snapshots = new Photo[2];
    private final TakePictureStateMachine stateMachine = new TakePictureStateMachine(this);

    private RadioButton[] redGreenSelect = new RadioButton[2];
    private View[] snapshotStatus = new View[2];
    private Button exitButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_take_picture);

        ActivityRegistry.register(this);

        rl = (RelativeLayout) findViewById(R.id.take_picture_rl);

        exitButton = (Button) findViewById(R.id.takePicture_exitBtn);
        this.snapshotStatus[PHASE_RED] = findViewById(R.id.takePicture_modeRedStatus);
        this.snapshotStatus[PHASE_GREEN] = findViewById(R.id.takePicture_modeGreenStatus);
        this.redGreenSelect[PHASE_RED] = (RadioButton) findViewById(R.id.takePicture_modeRedSelect);
        this.redGreenSelect[PHASE_GREEN] = (RadioButton) findViewById(R.id.takePicture_modeGreenSelect);

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        motionService = new MotionService();
        locationService = new LocationService(getApplicationContext());


        // generate crosshair position
        // koordinates are relative to parent_view
        final double x = randomDouble(CROSSHAIR_MARGIN_HOR, CROSSHAIR_MARGIN_HOR);
        final double y = randomDouble(CROSSHAIR_MARGIN_TOP, CROSSHAIR_MARGIN_BOT);

        addCrosshairView(x,y);

        this.stateMachine.switchPhase(PHASE_RED);
    }


    private static int togglePhase(int in) {
        return in == PHASE_RED ? PHASE_GREEN : PHASE_RED;
    }

    /**
     * This class manages the workflow of the {@link TakePictureActivity}.
     */
    private class TakePictureStateMachine {

        private final TakePictureActivity owner;
        private final Photo[] snapshots;
        private int phase = PHASE_INIT;


        TakePictureStateMachine(TakePictureActivity owner) {
            this.owner = owner;
            this.snapshots = owner.snapshots;
        }


        void switchPhase(int newPhase) {
            if (this.phase == newPhase) return;

            this.phase = newPhase;
            owner.enterNextPhase(newPhase);

            switch (this.phase) {
                case PHASE_RED:
                    this.phase = PHASE_GREEN;
                case PHASE_GREEN:
                    this.phase = newPhase;
            }
        }

        public void takeSnapshot(Photo snapshot) {
            TakePictureActivity.this.snapshots[this.phase] = snapshot;

            owner.notifySnapshotTaken(this.phase);

            // After at least one snapshots was taken, we allow the user to submit the snapshots
            owner.allowSubmit(true);

            int otherPhase = togglePhase(this.phase);
            if (this.snapshots[otherPhase] == null)
                // no snapshots for the other phase was taken yet
                this.switchPhase(otherPhase);
            else {
                // snapshots for both phases were taken
                prepareUpload();
            }
        }

        public void prepareUpload() {
            if (TakePictureActivity.this.snapshots[PHASE_RED] == null && TakePictureActivity.this.snapshots[PHASE_GREEN] == null) {
                // no snapshots were taken at all
                Toast.makeText(TakePictureActivity.this.getApplicationContext(), "Please take a snapshots first!", Toast.LENGTH_SHORT).show();
            } else {
                Intent intent = new Intent(TakePictureActivity.this, TagLightsActivity.class);
                startActivity(intent);
            }
        }
    }

    /**
     * Configures the UI so that the user can navigate to next activity
     *
     * @param allow
     */
    private void allowSubmit(boolean allow) {
        this.exitButton.setEnabled(allow);
    }

    /**
     * @param phase use {@link #PHASE_RED} or {@link #PHASE_GREEN}
     */
    private void enterNextPhase(int phase) {
        RadioButton modeSelect = this.redGreenSelect[phase];
        modeSelect.setChecked(true);
        cross[phase].setVisibility(View.INVISIBLE);
        cross[togglePhase(phase)].setVisibility(View.INVISIBLE);

    }

    /**
     * @param redOrGreen use {@link #PHASE_RED} or {@link #PHASE_GREEN}
     */
    private void notifySnapshotTaken(int redOrGreen) {
        CheckBox box = (CheckBox) this.snapshotStatus[redOrGreen];
        box.setChecked(true);
    }

    public void onGreenSelect(View view) {
        stateMachine.switchPhase(PHASE_GREEN);
    }

    public void onRedSelect(View view) {
        stateMachine.switchPhase(PHASE_RED);
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
        bmp = rotateImage(bmp, 90);

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) crosshairView.getLayoutParams();

        final LightPhase phase =stateMachine.phase == PHASE_RED ? LightPhase.RED : LightPhase.GREEN;



        if (locationService.getLocation() != null) {
            PhotoInformation.shared.setLocation(locationService.getLocation());
        }

        Photo snapshot = Photo.build()
                .setBitmap(bmp)
                .setGyro(motionService.getPitch())
                .setLocation(locationService.getLocation())
                .setLightPos(new LightPosition(crossHairX,crossHairY,phase,true))
                .setCreatedAt(System.currentTimeMillis())
                .commit();

        TakePictureActivity.this.stateMachine.takeSnapshot(snapshot);

        // prepare camera for next picture
        camera.stopPreview();
        camera.startPreview();
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

            for (Camera.Size s : sizes) {
                System.out.println("Available resolutions: " + s.width + ", " + s.height);
            }

            cam.setDisplayOrientation(90);

            camPreview = new CameraPreview(this, cam);
            FrameLayout preview = (FrameLayout) findViewById(R.id.take_picture_cameraPreview);

            preview.addView(camPreview);
        }
        ;
    }


    private static final double CROSSHAIR_MARGIN_HOR = 0.25;
    private static final double CROSSHAIR_MARGIN_TOP = 0.2;
    private static final double CROSSHAIR_MARGIN_BOT = 0.5;
    private static final Random random = new Random();

    private static double randomDouble(double min, double max) {
        final double d = 1.0 - max - min;
        return min + d * random.nextDouble();
    }

    private void addCrosshairView(double x,double y) {
        // actually should use dimensions of the container-view instead of screen
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        final int screen_h = displayMetrics.heightPixels;
        final int screen_w = displayMetrics.widthPixels;

        float density = getApplicationContext().getResources().getDisplayMetrics().density;
        int viewHeight = (int) (50 * density) * 2;
        int viewWidth = (int) (50 * density);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(viewWidth, viewHeight);
        params.leftMargin = (int) (screen_w * x - viewWidth / 2);
        params.topMargin = (int) (screen_h * y - viewHeight / 2);

        LayoutInflater inflater = this.getLayoutInflater();
        crosshairView = inflater.inflate(R.layout.view_camera_crosshair, null);
        rl.addView(crosshairView, params);

        this.cross[PHASE_RED] = crosshairView.findViewById(R.id.takePicture_redCross);
        this.cross[PHASE_GREEN] = crosshairView.findViewById(R.id.takePicture_greenCross);

        this.crossHairX = x;
        this.crossHairY = y;
    }

    public void onCaptureButtonPressed(View view) {
        if (!permissionGranted) {
            return;
        }

        cam.takePicture(null, null, this);
    }

    private void onAfterPictureTaken() {
        Intent intent = new Intent(this, TagLightsActivity.class);
        startActivity(intent);
    }

    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }

    /**
     * A safe way to get an instance of the Camera object.
     */
    private Camera getCameraInstance() {
        if (cam != null) {
            return cam;
            /*cam.release();
            cam = null;*/
        }

        try {
            cam = Camera.open(); // attempt to get a Camera instance
        } catch (Exception e) {
            // Camera is not available (in use or does not exist)
            e.printStackTrace();
        }
        return cam; // returns null if camera is unavailable
    }

    public void onExitButtonClick(View view) {
        stateMachine.prepareUpload();
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
