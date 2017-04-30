package com.hs_augsburg_example.lightscatcher.activities_major;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.hs_augsburg_example.lightscatcher.R;
import com.hs_augsburg_example.lightscatcher.camera.utils.CameraPreview;
import com.hs_augsburg_example.lightscatcher.dataModels.LightPosition;
import com.hs_augsburg_example.lightscatcher.dataModels.Photo;
import com.hs_augsburg_example.lightscatcher.dataModels.Record;
import com.hs_augsburg_example.lightscatcher.services.LocationService;
import com.hs_augsburg_example.lightscatcher.services.MotionService;
import com.hs_augsburg_example.lightscatcher.singletons.UserInformation;
import com.hs_augsburg_example.lightscatcher.utils.ActivityRegistry;
import com.hs_augsburg_example.lightscatcher.dataModels.LightPhase;
import com.hs_augsburg_example.lightscatcher.utils.UserPreference;

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

    // crosshair constraints (center-coordinates relative to layoutContainer)
    private static final double CROSSHAIR_X_MIN = .3;
    private static final double CROSSHAIR_X_MAX = 1 - CROSSHAIR_X_MIN;
    private static final double CROSSHAIR_Y_MIN = .2;
    private static final double CROSSHAIR_Y_MAX = .4;
    private static final Random random = new Random();

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
    private double crossHairWidth;
    private TextView txtCaption;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_take_picture);

        ActivityRegistry.register(this);

        rl = (RelativeLayout) findViewById(R.id.take_picture_rl);

        exitButton = (Button) findViewById(R.id.takePicture_exitBtn);
        txtCaption = (TextView) findViewById(R.id.takePicture_caption);
        this.snapshotStatus[PHASE_RED] = findViewById(R.id.takePicture_modeRedStatus);
        this.snapshotStatus[PHASE_GREEN] = findViewById(R.id.takePicture_modeGreenStatus);
        this.redGreenSelect[PHASE_RED] = (RadioButton) findViewById(R.id.takePicture_modeRedSelect);
        this.redGreenSelect[PHASE_GREEN] = (RadioButton) findViewById(R.id.takePicture_modeGreenSelect);


        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        motionService = new MotionService();
        locationService = new LocationService(getApplicationContext());


        this.rl.post(new Runnable() {
            @Override
            public void run() {
                // this has to wait until the layout-process has finished:

                // generate crosshair position (center coordinates, relative to parent_view)
                double x = randomDouble(CROSSHAIR_X_MIN, CROSSHAIR_X_MIN);
                double y = randomDouble(CROSSHAIR_Y_MIN, CROSSHAIR_Y_MAX);

                addCrosshairView(x, y);
                stateMachine.switchPhase(PHASE_RED);
            }
        });
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

            /*List<Camera.Size> sizes = params.getSupportedPictureSizes();

            for (Camera.Size s : sizes) {
                System.out.println("Available resolutions: " + s.width + ", " + s.height);
            }*/

            cam.setDisplayOrientation(90);

            camPreview = new CameraPreview(this, cam);
            FrameLayout preview = (FrameLayout) findViewById(R.id.take_picture_cameraPreview);

            preview.addView(camPreview);
        }
        ;
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


    private static double randomDouble(double min, double max) {
        final double d = 1.0 - max - min;
        return min + d * random.nextDouble();
    }

    private void addCrosshairView(double x, double y) {
        // actually should use dimensions of the container-view instead of screen
//        DisplayMetrics displayMetrics = new DisplayMetrics();
//        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
//        final int parent_h = displayMetrics.heightPixels;
//        final int parent_w = displayMetrics.widthPixels;

        Rect parentRect = new Rect();
        rl.getDrawingRect(parentRect);
        parentRect.set(0, 0, rl.getWidth(), rl.getHeight());
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
//        final int parent_h = displayMetrics.heightPixels;
//        final int parent_w = displayMetrics.widthPixels;
        float density = getApplicationContext().getResources().getDisplayMetrics().density;

        int viewHeight = (int) (50 * density) * 2;
        int viewWidth = (int) (50 * density);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(viewWidth, viewHeight);
        params.leftMargin = (int) (parentRect.width() * x - viewWidth / 2);
        params.topMargin = (int) (parentRect.height() * y - viewHeight / 2);

        LayoutInflater inflater = this.getLayoutInflater();
        crosshairView = inflater.inflate(R.layout.view_camera_crosshair, null);
        //((FrameLayout)findViewById(R.id.take_picture_cameraPreview)).addView(crosshairView, params);
        rl.addView(crosshairView, params);

        this.cross[PHASE_RED] = crosshairView.findViewById(R.id.takePicture_redCross);
        this.cross[PHASE_GREEN] = crosshairView.findViewById(R.id.takePicture_greenCross);

        //remember posiction of the cross
        this.crossHairX = x;
        this.crossHairY = y;
        // size relative to parent_width
        this.crossHairWidth = (double) viewWidth / parentRect.width();
    }

    private static int togglePhase(int in) {
        return in == PHASE_RED ? PHASE_GREEN : PHASE_RED;
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
        cross[phase].setVisibility(View.VISIBLE);
        cross[togglePhase(phase)].setVisibility(View.INVISIBLE);
        txtCaption.setText(getString(R.string.fotografieren, phase == 0 ? "Rot-Phase" : "Grün-Phase"));
    }

    /**
     * @param redOrGreen use {@link #PHASE_RED} or {@link #PHASE_GREEN}
     */
    private void notifySnapshotTaken(int redOrGreen) {
        CheckBox box = (CheckBox) this.snapshotStatus[redOrGreen];
        box.setChecked(true);
    }

    private void navigateToTagLights() {

        // +1 points for each photo
        int score = (snapshots[0] == null ? 0 : 1) + (snapshots[1] == null ? 0 : 1);

        Record.Builder r = Record.buildNew();
        r.setUser(UserInformation.shared.getUid());
        r.setRedPhoto(snapshots[PHASE_RED]);
        r.setGreenPhoto(snapshots[PHASE_GREEN]);
        r.giveAppropriatePoints();

        Record.latestRecord = r;

        Intent intent = new Intent(TakePictureActivity.this, SubmitActivity.class);
        startActivity(intent);
    }

    private boolean permissionGranted = false;

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
        bmp = rotateImage(bmp, 90);


        final LightPhase phase = stateMachine.phase == PHASE_RED ? LightPhase.RED : LightPhase.GREEN;

        Point crosshair = translateRelativeLayoutToImage(crossHairX, crossHairY);
        //TODO: fix lightPosition: consider the size of the corsair
        // if this is a red picture the light is above
        // if it's a green picture it's bellow the crossHair center
        LightPosition pos = new LightPosition(crosshair, phase, true);

        Photo.Builder photoB = Photo.buildNew()
                .setBitmap(bmp)
                .setLightPos(pos);

        if (motionService != null)
            photoB.setGyro(motionService.getPitch());

        if (locationService != null && locationService.canGetLocation()) {
            Location l = locationService.getLocation();
            photoB.setLocation(l);
        }

        Photo snapshotData = photoB.commit();
        // Store the image
        this.snapshots[this.stateMachine.phase] = snapshotData;

        // show visual feedback, that the snapshot was taken
        this.notifySnapshotTaken(this.stateMachine.phase);

        // After at least one snapshots was taken, we allow the user to submit the snapshots
        this.allowSubmit(true);

        TakePictureActivity.this.stateMachine.onSnapshotTaken(snapshotData);

        // prepare camera for next picture
        camera.stopPreview();
        camera.startPreview();
    }

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
    public void onBackPressed() {
        navigateBack(null);
        super.onBackPressed();
    }

    public void navigateBack(View view) {
        Intent intent = new Intent(getApplicationContext(), HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    private Point translateRelativeLayoutToImage(double x, double y) {

        // layoutsize in screen
        Rect clip = new Rect();
        this.camPreview.getDrawingRect(clip);

        Camera.Parameters camParams = this.camPreview.getCamera().getParameters();
        Camera.Size picSize = camParams.getPictureSize();

        Rect surface = this.camPreview.getSurface().getSurfaceFrame();

        double ratioX = picSize.width / surface.width();
        double ratioY = picSize.height / surface.height();


//        Point result = new Point((int) (clip.left + x * clip.width()), (int) (clip.top + y * clip.height()));
        Point result = new Point((int) (clip.left + ratioX * x * clip.width()), (int) (clip.top + ratioY * y * clip.height()));

        return result;
    }

    public void onGreenSelect(View view) {
        stateMachine.switchPhase(PHASE_GREEN);
    }

    public void onRedSelect(View view) {
        stateMachine.switchPhase(PHASE_RED);
    }

    public void onCaptureButtonPressed(View view) {
        if (!permissionGranted) {
            return;
        }

        cam.takePicture(null, null, this);
    }

    public void onExitButtonClick(View view) {
        stateMachine.prepareUpload();
    }

    public void onInfoButtonPressed(View view) {
        if (pictureHelpDialog == null) {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder.setTitle(getString(R.string.take_picture_activity_title_info));
            dialogBuilder.setMessage(getString(R.string.take_picture_activity_txt_info));
            dialogBuilder.setPositiveButton(getString(R.string.take_picture_activity_button_info), null);

            pictureHelpDialog = dialogBuilder.create();
        }

        pictureHelpDialog.show();
    }

    private AlertDialog pictureHelpDialog;

    public static Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
                matrix, true);
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

        public void onSnapshotTaken(Photo snapshot) {


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
                this.owner.navigateToTagLights();
            }
        }
    }

}
