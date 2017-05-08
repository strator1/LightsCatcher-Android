package com.hs_augsburg_example.lightscatcher.activities_major;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.hs_augsburg_example.lightscatcher.R;
import com.hs_augsburg_example.lightscatcher.camera.CameraTextureView;
import com.hs_augsburg_example.lightscatcher.dataModels.LightPhase;
import com.hs_augsburg_example.lightscatcher.dataModels.LightPosition;
import com.hs_augsburg_example.lightscatcher.dataModels.Photo;
import com.hs_augsburg_example.lightscatcher.dataModels.Record;
import com.hs_augsburg_example.lightscatcher.services.LocationService;
import com.hs_augsburg_example.lightscatcher.services.MotionService;
import com.hs_augsburg_example.lightscatcher.singletons.UserInformation;
import com.hs_augsburg_example.lightscatcher.utils.ActivityRegistry;
import com.hs_augsburg_example.lightscatcher.utils.Log;
import com.hs_augsburg_example.lightscatcher.utils.UserPreference;
import com.hs_augsburg_example.lightscatcher.views.BadgeDrawable;

import java.util.Random;

/**
 * Activity to capture a traffic light. Up to 2 pictures can be taken in a row, one for each phase of the traffic light.
 */
public class TakePictureActivity extends AppCompatActivity implements Camera.PictureCallback {
    private static final String TAG = "TakePictureActivity";
    private static final boolean LOG = true;

    private static final int REQUEST_CAMERA_PERMISSION = 1;
    static final int PHASE_INIT = -1;
    public static final int PHASE_RED = 0;

    public static final int PHASE_GREEN = 1;
    // crosshair constraints (center-coordinates relative to layoutContainer)
    public static final float CROSSHAIR_X_MIN = .3f;
    public static final float CROSSHAIR_X_MAX = 1f - CROSSHAIR_X_MIN;
    public static final float CROSSHAIR_Y_MIN = .2f;
    public static final float CROSSHAIR_Y_MAX = .45f;
    private static final Random random = new Random();


    private float crossHairX;
    private float crossHairY;
    private float crossHairWidth;

    private View[] cross = new View[2];


    private CompoundButton[] phaseSelect = new CompoundButton[2];
    private BadgeDrawable[] badges = new BadgeDrawable[2];
    private Button exitButton;
    private TextView txtCaption;
    private RelativeLayout rl;
    private View crosshairView;
    private CameraTextureView camPreview;
    private Camera camera;

    // use PHASE_GREEN or PHASE_RED as index to access the corresponding snapshot
    // so index 0 is for red-snapshot, index 1 for green
    private final Photo[] snapshots = new Photo[2];

    private SensorManager mSensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private LocationService locationService;
    private MotionService motionService;

    private final TakePictureStateMachine stateMachine = new TakePictureStateMachine(this);
    private Animation grow;
    private Animation shrink;

    private static <T> int indexOf(T[] array, T elem) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == elem) {
                return i;
            }
        }
        return -1;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_take_picture);

        ActivityRegistry.register(this);

        rl = (RelativeLayout) findViewById(R.id.take_picture_rl);

        exitButton = (Button) findViewById(R.id.takePicture_exitBtn);
        txtCaption = (TextView) findViewById(R.id.takePicture_caption);

        // phase-button animation:
        shrink = AnimationUtils.loadAnimation(this, R.anim.phaseselect_shrink);
        grow = AnimationUtils.loadAnimation(this, R.anim.phaseselect_grow);

        CompoundButton.OnCheckedChangeListener onCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

            }
        };
        CompoundButton.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int phase = indexOf(phaseSelect, v);
                if (!stateMachine.switchPhase(phase))
                    v.startAnimation(grow);
            }
        };
        int[] ids = new int[]{R.id.takePicture_redSelect, R.id.takePicture_greenSelect,};
        for (int phase = 0; phase < 2; phase++) {

            CompoundButton b = (CompoundButton) findViewById(ids[phase]);
            b.setOnCheckedChangeListener(onCheckedChangeListener);
            b.setOnClickListener(onClickListener);
            try {
                this.badges[phase] = createBadge(b);
            } catch (Exception ex) {
                Log.e(TAG, ex);
            }

            this.phaseSelect[phase] = b;
        }

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        motionService = new MotionService();
        locationService = new LocationService(getApplicationContext());

        // generate crosshair position (center coordinates, relative to parent_view)
        crossHairX = randomFloat(CROSSHAIR_X_MIN, CROSSHAIR_X_MAX);
        crossHairY = randomFloat(CROSSHAIR_Y_MIN, CROSSHAIR_Y_MAX);

        crossHairX = CROSSHAIR_X_MAX;
        crossHairY = CROSSHAIR_Y_MAX;

        camPreview = new CameraTextureView(this.getApplicationContext());
        camPreview.setPivotRelative(new PointF(crossHairX, crossHairY));
        camPreview.setPivotRelative(new PointF(crossHairX, crossHairY));
        rl.addView(camPreview, 0);

        this.rl.post(new Runnable() {
            @Override
            public void run() {
                // this has to wait until the layout-process has finished:
                addCrosshairView(crossHairX, crossHairY);
                stateMachine.switchPhase(PHASE_RED);
            }
        });
    }

    private BadgeDrawable createBadge(CompoundButton btn) {
        StateListDrawable background = (StateListDrawable) btn.getBackground();
        BadgeDrawable badgeDrawable = new BadgeDrawable(this);

        LayerDrawable localLayerDrawable = new LayerDrawable(new Drawable[]{background, badgeDrawable});
        btn.setBackgroundDrawable(localLayerDrawable);
        return badgeDrawable;
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
        if (camera != null) {
            return camera;
            /*camera.release();
            camera = null;*/
        }

        try {
            camera = Camera.open(); // attempt to get a Camera instance
        } catch (Exception e) {
            // Camera is not available (in use or does not exist)
            e.printStackTrace();
        }
        camera.setDisplayOrientation(90);
        return camera; // returns null if camera is unavailable
    }


    public static float randomFloat(float min, float max) {
        final double d = max - min;
        return (float) (min + d * random.nextDouble());
    }

    private void addCrosshairView(float x, float y) {
        if (LOG) Log.d(TAG, "addCrosshairView: " + x + "; " + y);
        Rect parentRect = new Rect();
        rl.getDrawingRect(parentRect);
        parentRect.set(0, 0, rl.getWidth(), rl.getHeight());

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
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
        this.crossHairWidth = (float) viewWidth / parentRect.width();
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
    private void notifyNextPhase(int phase) {
        int otherPhase = togglePhase(phase);
        this.phaseSelect[phase].setChecked(true);
        phaseSelect[otherPhase].setChecked(false);
        cross[phase].setVisibility(View.VISIBLE);
        cross[otherPhase].setVisibility(View.INVISIBLE);
        txtCaption.setText(getString(R.string.fotografieren, phase == 0 ? "Rot-Phase" : "Grün-Phase"));
        phaseSelect[phase].startAnimation(grow);
        phaseSelect[otherPhase].startAnimation(shrink);
    }

    /**
     * @param redOrGreen use {@link #PHASE_RED} or {@link #PHASE_GREEN}
     */
    private void notifySnapshotTaken(int redOrGreen, Photo photo) {
        BadgeDrawable view = badges[stateMachine.phase];
        if (view != null) {
            view.setCount(1);
        }
    }

    private void notifyReset() {
        badges[0].setCount(0);
        badges[1].setCount(0);

        phaseSelect[0].startAnimation(grow);
    }

    private void navigateToSubmit() {

        Record.Builder r = Record.buildNew();
        r.setUser(UserInformation.shared.getUserId());
        r.setRedPhoto(snapshots[PHASE_RED]);
        r.setGreenPhoto(snapshots[PHASE_GREEN]);
        r.giveAppropriatePoints();

        Record.latestRecord = r;

        Intent intent = new Intent(TakePictureActivity.this, SubmitActivity.class);
        startActivity(intent);
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
        bmp = rotateImage(bmp, 90);

        final LightPhase phase = stateMachine.phase == PHASE_RED ? LightPhase.RED : LightPhase.GREEN;

        Location loc = null;
        if (locationService != null && locationService.canGetLocation())
            loc = locationService.getLocation();

        float gyro = 0;
        if (motionService != null)
            gyro = motionService.getPitch();


        //TODO: fix lightPosition: consider the size of the corsair
        // if this is a red picture the light is a little above
        // if it's a green picture it's bellow the crossHair center
        //TODO: transform to coordinates relative to the image
        // crossHair coordinates are relative to the layout-box of camPreview
        LightPosition pos = new LightPosition(crossHairX, crossHairY, phase, true);

        Photo snapshotData = Photo.buildNew()
                .setBitmap(bmp)
                .setLightPos(pos)
                .setLocation(loc)
                .setGyro(gyro)
                .commit();


        TakePictureActivity.this.stateMachine.snapshotTaken(snapshotData);

        // prepare camera for next picture
        camera.stopPreview();
        camera.startPreview();
    }

    private boolean permissionGranted = false;

    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            permissionGranted = true;
            if (camera == null) {
                setupCamera();
            }
            camPreview.statPreview();
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            permissionGranted = false;
        } else {
            permissionGranted = false;
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
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

        if (camPreview != null) {
            camPreview.releaseCamera();
            camera = null;
        }

        locationService.stopListening();
        mSensorManager.unregisterListener(motionService.getEventListener());
    }

    private void setupCamera() {
        if (checkCameraHardware(getApplicationContext())) {
            camera = getCameraInstance();
            camPreview.setCamera(camera);
        }
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

    public void onCaptureButtonPressed(View view) {
        if (!permissionGranted || camPreview.camera == null) {
            Toast.makeText(this.getApplicationContext(), "Kamera nicht verfügbar :(", Toast.LENGTH_SHORT).show();
            return;
        }
        camPreview.camera.takePicture(null, null, this);
    }

    public void onExitButtonClick(View view) {
        stateMachine.goToSubmit();
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

    public void onDiscardButtonClick(View view) {
        this.stateMachine.reset();
    }

    public void zoomIn_Click(View view) {
        camPreview.zoomIn();
    }

    public void zoomOut_Click(View view) {
        camPreview.zoomOut();
    }

    /**
     * This class manages the workflow of the {@link TakePictureActivity}.
     */
    private class TakePictureStateMachine {

        private final TakePictureActivity owner;
        private int phase = PHASE_INIT;


        TakePictureStateMachine(TakePictureActivity owner) {
            this.owner = owner;
        }


        boolean switchPhase(int newPhase) {
            if (this.phase == newPhase) return false;

            this.phase = newPhase;
            owner.notifyNextPhase(newPhase);
            return true;
        }

        void snapshotTaken(Photo snapshot) {

            // Store the image
            owner.snapshots[this.phase] = snapshot;

            // show visual feedback, that the snapshot was taken
            owner.notifySnapshotTaken(this.phase, snapshot);

            // After at least one snapshots was taken, we allow the user to submit the snapshots
            owner.allowSubmit(true);

            // decide what to do next
            switch (this.phase) {
                case PHASE_RED:
                    this.switchPhase(PHASE_GREEN);
                    break;
                case PHASE_GREEN:
                    goToSubmit();
            }
        }

        void goToSubmit() {
            if (owner.snapshots[PHASE_RED] == null && owner.snapshots[PHASE_GREEN] == null) {
                // no snapshots were taken at all
                Toast.makeText(TakePictureActivity.this.getApplicationContext(), R.string.toast_snapshotNotTaken, Toast.LENGTH_SHORT).show();
            } else {
                this.owner.navigateToSubmit();
            }
        }

        void reset() {
            // discard existing snapshots
            Photo[] photos = owner.snapshots;
            photos[0] = photos[1] = null;

            // don't allow continue
            allowSubmit(false);

            // update UI
            owner.notifyReset();

            // switch to default phase
            this.switchPhase(PHASE_RED);
        }
    }


}
