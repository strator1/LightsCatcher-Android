package com.hs_augsburg_example.lightscatcher.activities_major;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
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
import com.hs_augsburg_example.lightscatcher.camera.CameraTexturePreview;
import com.hs_augsburg_example.lightscatcher.dataModels.LightPhase;
import com.hs_augsburg_example.lightscatcher.dataModels.LightPosition;
import com.hs_augsburg_example.lightscatcher.dataModels.Photo;
import com.hs_augsburg_example.lightscatcher.services.LocationService;
import com.hs_augsburg_example.lightscatcher.services.MotionService;
import com.hs_augsburg_example.lightscatcher.singletons.UserInformation;
import com.hs_augsburg_example.lightscatcher.utils.ActivityRegistry;
import com.hs_augsburg_example.lightscatcher.utils.Log;
import com.hs_augsburg_example.lightscatcher.utils.MiscUtils;
import com.hs_augsburg_example.lightscatcher.utils.TaskMonitor;
import com.hs_augsburg_example.lightscatcher.utils.UserPreference;
import com.hs_augsburg_example.lightscatcher.views.BadgeDrawable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN;
import static com.hs_augsburg_example.lightscatcher.dataModels.LightPhase.RED;
import static com.hs_augsburg_example.lightscatcher.utils.MiscUtils.dp;

/**
 * Activity to capture a traffic light. Up to 2 pictures can be taken in a row, one for each state of the traffic light.
 */
public class TakePictureActivity extends FragmentActivity implements Camera.PictureCallback {
    private static final String TAG = "TakePictureActivity";
    private static final boolean LOG = true;//&& Log.ENABLED;

    private static final int REQUEST_CAMERA = 1;
    private static final int REQUEST_LOCATION = 2;
    private static final int REQUEST_ALL = REQUEST_CAMERA | REQUEST_LOCATION;

    static final int STATE_INIT = -1;
    public static final int STATE_RED = LightPhase.RED.value;
    public static final int STATE_GREEN = LightPhase.GREEN.value;
    private static final int STATE_OFF = LightPhase.OFF.value;

    // crosshair constraints (center-coordinates relative to layoutContainer)
    public static final float CROSSHAIR_X_MIN = .3f;
    public static final float CROSSHAIR_X_MAX = 1f - CROSSHAIR_X_MIN;
    public static final float CROSSHAIR_Y_MIN = .2f;
    public static final float CROSSHAIR_Y_MAX = .45f;
    private static final Random random = new Random();


    private float crossHairX;
    private float crossHairY;
    private float crossHairCenterToLight;

    private Button exitButton;
    private TextView txtCaption;
    private RelativeLayout rl;
    private View crosshairView;
    private CameraTexturePreview camPreview;
    private Camera camera;

    // use STATE_GREEN or STATE_RED as index to access the corresponding snapshot
    // so index 0 is for red-snapshot, index 1 for green
    private View[] cross = new View[3];
    private CompoundButton[] phaseSelect = new CompoundButton[3];
    private BadgeDrawable[] badges = new BadgeDrawable[3];
    private final Photo[] snapshots = new Photo[3];

    private SensorManager mSensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private LocationService locationService;
    private MotionService motionService;

    private final TakePictureStateMachine stateMachine = new TakePictureStateMachine(this);
    private boolean locationPermission;
    private LightPhase latestPhase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(FLAG_FULLSCREEN, FLAG_FULLSCREEN);
        setContentView(R.layout.activity_take_picture);

        ActivityRegistry.register(this);

        rl = (RelativeLayout) findViewById(R.id.take_picture_rl);

        exitButton = (Button) findViewById(R.id.takePicture_exitBtn);
        txtCaption = (TextView) findViewById(R.id.takePicture_caption);

        int[] ids = new int[]{
                R.id.takePicture_redSelect,
                R.id.takePicture_greenSelect,
                R.id.takePicture_offSelect,
        };
        for (int phase = 0; phase < 3; phase++) {

            // state-button animation:
            final Animation shrink = AnimationUtils.loadAnimation(this, R.anim.phaseselect_shrink);
            final Animation grow = AnimationUtils.loadAnimation(this, R.anim.phaseselect_grow);

            CompoundButton b = (CompoundButton) findViewById(ids[phase]);
            b.setTag(LightPhase.fromValue(phase));
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    LightPhase phase = (LightPhase) v.getTag();
                    if (!stateMachine.switchPhase(phase))
                        v.startAnimation(grow);
                }
            });
            b.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        buttonView.startAnimation(grow);
                    } else {
                        buttonView.startAnimation(shrink);
                    }
                }
            });
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


        camPreview = new CameraTexturePreview(this.getApplicationContext());
        rl.addView(camPreview, 0);
        rl.post(new Runnable() {
            @Override
            public void run() {
                // this has to wait until the layout-process has finished for the first time:
                setRandomCrosshairView();
                stateMachine.switchPhase(RED);
            }
        });
    }

    @Override
    protected void onStart() {
        if (LOG) Log.d(TAG, "onStart");
        super.onStart();
        cameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        locationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        List<String> requests = new ArrayList<>();
        if (cameraPermission) {
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            if (LOG) Log.d(TAG, "shouldShowRequestPermissionRationale: true");

            // the app has requested this permission previously and the user denied the request.
            // show a dialog and explain that we really need the permission
            showCameraRequestDialog();
        } else {
            if (LOG) Log.d(TAG, "shouldShowRequestPermissionRationale: false");
            //First request at all, Never ask again selected, or device policy prohibits the app from having that permission.
            requests.add(Manifest.permission.CAMERA);
        }

        if (!locationPermission) {
            boolean requestedBefore = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION);
            if (!requestedBefore)
                requests.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (requests.size() > 0) {
            if (LOG) Log.d(TAG, "calling requestPermissions ...");
            String[] arr = new String[requests.size()];
            requests.toArray(arr);
            ActivityCompat.requestPermissions(TakePictureActivity.this, arr, REQUEST_ALL);
        }
    }

    @Override
    protected void onResume() {
        if (LOG) Log.d(TAG, "onResume");
        super.onResume();
        if (LOG) Log.d(TAG, "cameraPermission: " + cameraPermission);
        if (cameraPermission) {
            if (camera == null) {
                setupCamera();
            }
            camPreview.statPreview();
        }

        if (locationPermission) {
            locationService.startListening();
        }
        if (UserPreference.isFirstCapture(getApplicationContext())) {
            new Handler().post(new Runnable() {
                @Override
                public void run() {
                    onInfoButtonPressed(null);
                }
            });
        }
        mSensorManager.registerListener(motionService.getEventListener(), accelerometer, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(motionService.getEventListener(), magnetometer, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        if (LOG) Log.d(TAG, "onPause");
        super.onPause();

        locationService.stopListening();
        mSensorManager.unregisterListener(motionService.getEventListener());
    }

    @Override
    protected void onStop() {
        if (LOG) Log.d(TAG, "onStop");
        super.onStop();

        if (camPreview != null) {
            camPreview.releaseCamera();
            camera = null;
        }
    }

    @Override
    protected void onDestroy() {
        if (LOG) Log.d(TAG, "onDestroy");
        super.onDestroy();

        locationService.stopListening();
        mSensorManager.unregisterListener(motionService.getEventListener());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        for (int i = 0; i < permissions.length; i++) {
            if (LOG)
                Log.d(TAG, "onRequestPermissionsResult: code: {0}, relult: {1}", permissions[i], grantResults[i]);

            switch (permissions[i]) {
                case Manifest.permission.CAMERA:
                    cameraPermission = grantResults[i] == PackageManager.PERMISSION_GRANTED;
                    if (!cameraPermission) {
                        showCameraRequestDialog();
                        Toast.makeText(this, "Kein Zugriff auf Kamera genemigt!", Toast.LENGTH_SHORT).show();
                    } else {
                        // No need to start camera here; it is handled by onResume
                    }
                    break;
                case Manifest.permission.ACCESS_FINE_LOCATION:
                    locationPermission = grantResults[i] == PackageManager.PERMISSION_GRANTED;
                    break;
            }
        }
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
        bmp = MiscUtils.rotateImage(bmp, 90);

        final LightPhase phase = LightPhase.fromValue(stateMachine.state);

        Location loc = null;
        if (locationService != null && locationService.canGetLocation())
            loc = locationService.getLocation();

        float gyro = 0;
        if (motionService != null)
            gyro = motionService.getPitch();


        // if this is a red picture the light is a little above
        // if it's a green picture it's bellow the crossHair center
        float shiftY = 0;
        switch (phase) {
            case RED:
                shiftY = -crossHairCenterToLight;
                break;
            case GREEN:
                shiftY = crossHairCenterToLight;
                break;
            case OFF:
                shiftY = 0;
                break;
        }
        PointF crossLayout = new PointF(crossHairX, crossHairY + shiftY / camPreview.getZoom());
        PointF crossImage = camPreview.translateLayoutToImage(crossLayout);
        Log.d(TAG, "translate crosshair: {0} -> {1}", crossLayout, crossImage);

        PointF test = camPreview.translateImageToLayout(crossImage);
        Log.d(TAG, "test: -> " + test);
        LightPosition pos = new LightPosition(crossImage.x, crossImage.y, phase, true);

        String uid = UserInformation.shared.getUserId();

        Photo snapshotData = Photo.buildNew()
                .setUser(uid)
                .setBitmap(bmp)
                .addLightPos(pos)
                .setLocation(loc)
                .setGyro(gyro)
                .setCredits(1)
                .commit();

        TakePictureActivity.this.stateMachine.snapshotTaken(snapshotData);
    }

    @Override
    public void onBackPressed() {
        navigateBack(null);
        super.onBackPressed();
    }


    private BadgeDrawable createBadge(CompoundButton btn) {
        StateListDrawable background = (StateListDrawable) btn.getBackground();
        BadgeDrawable badgeDrawable = new BadgeDrawable(this);

        LayerDrawable localLayerDrawable = new LayerDrawable(new Drawable[]{background, badgeDrawable});
        btn.setBackgroundDrawable(localLayerDrawable);
        return badgeDrawable;
    }

    private void setRandomCrosshairView() {
        float x = MiscUtils.randomFloat(random, CROSSHAIR_X_MIN, CROSSHAIR_X_MAX);
        float y = MiscUtils.randomFloat(random, CROSSHAIR_Y_MIN, CROSSHAIR_Y_MAX);

        setCrosshairView(x, y);
    }

    private void setCrosshairView(float x, float y) {
        if (LOG) Log.d(TAG, "setRandomCrosshairView: " + x + "; " + y);

        Rect parentRect = new Rect();
        parentRect.set(0, 0, camPreview.getWidth(), camPreview.getHeight());

        if (LOG) Log.d(TAG, "preview rect: " + parentRect.toShortString());

        float density = getApplicationContext().getResources().getDisplayMetrics().density;

        int viewHeight = dp(50 * 2, density);
        int viewWidth = dp(50, density);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(viewWidth, viewHeight);
        params.leftMargin = (int) (parentRect.width() * x - (float) viewWidth / 2);
        params.topMargin = (int) (parentRect.height() * y - (float) viewHeight / 2);

        if (crosshairView == null) {
            LayoutInflater inflater = this.getLayoutInflater();
            crosshairView = inflater.inflate(R.layout.view_camera_crosshair, null);
            rl.addView(crosshairView, params);

            this.cross[STATE_RED] = crosshairView.findViewById(R.id.takePicture_redCross);
            this.cross[STATE_GREEN] = crosshairView.findViewById(R.id.takePicture_greenCross);
            this.cross[STATE_OFF] = new View(this);
        } else {
            crosshairView.setLayoutParams(params);
        }

        // set pivoting, important for zoom and camera-focus
        camPreview.setPivotRelative(new PointF(x, y));

        //remember position of the cross
        this.crossHairX = x;
        this.crossHairY = y;

        // distance between light-position (colored cross) and center of the crosshair-border (coordinates specified by parameters x and y)
        this.crossHairCenterToLight = (float) viewHeight / parentRect.height() / 4;
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

    private boolean cameraPermission = false;

    private void setupCamera() {
        if (checkCameraHardware(getApplicationContext())) {
            camera = getCameraInstance();
            camPreview.setCamera(camera);
        }
    }

    /**
     * A safe way to get an instance of the Camera object.
     */
    private Camera getCameraInstance() {
        if (camera != null) {
            return camera;
        }

        try {
            camera = Camera.open(); // attempt to get a Camera instance
        } catch (Exception e) {
            // Camera is not available (in use or does not exist)
            Log.e(TAG, e);
        }
        camera.setDisplayOrientation(90);
        return camera; // returns null if camera is unavailable
    }


    /**
     * Updates the UI to reflect the given lightphase
     *
     * @param lightphase The desired phase.
     */
    private void notifyNextPhase(LightPhase lightphase) {

        int phase = lightphase.value;

        this.phaseSelect[phase].setChecked(true);
        cross[phase].setVisibility(View.VISIBLE);
        txtCaption.setText(getString(R.string.fotografieren, lightphase.getGermanText()));
        if (latestPhase != null) {
            phaseSelect[latestPhase.value].setChecked(false);
            cross[latestPhase.value].setVisibility(View.INVISIBLE);
        }
        this.latestPhase = lightphase;
    }

    /**
     * Updates the UI to show that the snapshot was taken and committed
     *
     * @param photo A reference to the committed snapshot
     */
    private void notifySnapshotTaken(Photo photo) {
        BadgeDrawable view = badges[stateMachine.state];
        if (view != null) {
            view.setCount(1);
        }
    }

    /**
     * Resets the UI to the default initial state
     */
    private void notifyReset() {
        badges[0].setCount(0);
        badges[1].setCount(0);
    }


    private void leave() {
        this.finish();
        Intent intent = new Intent(TakePictureActivity.this, FinishActivity.class);
        startActivity(intent);
    }

    private void showSubmitPopup(Photo snapshotData) {
        SubmitDialog popup = new SubmitDialog.Builder(submitListener)
                .setPhoto(snapshotData)
                .build();

        popup.show(getSupportFragmentManager(), SubmitDialog.TAG);
    }

    private final SubmitDialog.SubmitDialogListener submitListener = new SubmitDialog.SubmitDialogListener() {
        private final static String TAG = "SubmitDialogListener";

        @Override
        public void submitCommitted(Photo snapshot, TaskMonitor monitor) {
            if (LOG) Log.d(TAG, "submitCommitted");

            stateMachine.snapshotCommitted(snapshot);
            dialogCompleted();
        }

        @Override
        public void submitDiscarded() {
            if (LOG) Log.d(TAG, "submitDiscarded");
            dialogCompleted();
        }

        public void dialogCompleted() {
            // prepare camera for next picture
            camera.startPreview();
            setRandomCrosshairView();
        }

    };

    private void showCameraRequestDialog() {
        AlertDialog dlg = new AlertDialog.Builder(this)
                .setTitle("Kein Kamerazugriff")
                .setMessage("LightsCatcher benötigt zwingend Zugriff auf die Kamera, um fortzufahren.")
                .setPositiveButton("Zugriff anfordern.", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // delay so that the alert-dialog has time to close
                        new Handler().post(new Runnable() {
                            @Override
                            public void run() {
                                ActivityCompat.requestPermissions(TakePictureActivity.this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA);
                            }
                        });
                    }
                })
                .setNegativeButton("Beenden", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .create();
        dlg.show();
    }

    public void navigateBack(View view) {
        Intent intent = new Intent(getApplicationContext(), HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    public void onCaptureButtonPressed(View view) {
        if (!cameraPermission || camPreview.getCamera() == null) {
            Toast.makeText(this.getApplicationContext(), "Kamera nicht verfügbar :(", Toast.LENGTH_SHORT).show();
            return;
        }
        camPreview.getCamera().takePicture(null, null, this);
    }

    public void onExitButtonClick(View view) {
        stateMachine.leave();
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

    public void zoomIn_Click(View view) {
        camPreview.zoomIn();
    }

    public void zoomOut_Click(View view) {
        camPreview.zoomOut();
    }

    public void gamble_Click(View view) {
        setRandomCrosshairView();
    }

    /**
     * This class manages the workflow of the {@link TakePictureActivity}.
     */
    private class TakePictureStateMachine {

        private final TakePictureActivity owner;
        private int state = STATE_INIT;


        TakePictureStateMachine(TakePictureActivity owner) {
            this.owner = owner;
        }


        boolean switchPhase(LightPhase newPhase) {
            if (this.state == newPhase.value) return false;

            this.state = newPhase.value;
            owner.notifyNextPhase(newPhase);
            return true;
        }

        void snapshotTaken(Photo snapshot) {

            // let the user adjust light-position
            owner.showSubmitPopup(snapshot);
        }

        void snapshotCommitted(Photo snapshot) {
            // add the image
            owner.snapshots[this.state] = snapshot;

            // show visual feedback, that the snapshot was taken
            owner.notifySnapshotTaken(snapshot);

        }

        void leave() {

            this.owner.leave();

        }

        void discard() {
            // discard existing snapshots
            Photo[] photos = owner.snapshots;
            photos[0] = photos[1] = null;

            reset();
        }

        void reset() {
            // update UI
            owner.notifyReset();

            // switch to default state
            this.switchPhase(RED);
        }
    }


}
