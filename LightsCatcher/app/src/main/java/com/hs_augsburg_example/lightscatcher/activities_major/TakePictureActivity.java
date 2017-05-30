package com.hs_augsburg_example.lightscatcher.activities_major;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.ViewTarget;
import com.hs_augsburg_example.lightscatcher.R;
import com.hs_augsburg_example.lightscatcher.camera.CameraTexturePreview;
import com.hs_augsburg_example.lightscatcher.dataModels.LightGroup;
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

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN;
import static android.widget.RelativeLayout.*;
import static com.hs_augsburg_example.lightscatcher.dataModels.LightPhase.GREEN;
import static com.hs_augsburg_example.lightscatcher.dataModels.LightPhase.OFF;
import static com.hs_augsburg_example.lightscatcher.dataModels.LightPhase.RED;
import static com.hs_augsburg_example.lightscatcher.utils.MiscUtils.dp;
import static com.hs_augsburg_example.lightscatcher.utils.UserPreference.MAXIMUM_SNAPSHOT_ALERT;

/**
 * Activity to capture a traffic light. Up to 2 pictures can be taken in a row, one for each state of the traffic light.
 */
public class TakePictureActivity extends FragmentActivity implements Camera.PictureCallback {
    private static final String TAG = "TakePictureActivity";
    private static final boolean LOG = true;//&& Log.ENABLED;

    private static final int REQUEST_CAMERA = 1;
    private static final int REQUEST_LOCATION = 2;
    private static final int REQUEST_ALL = REQUEST_CAMERA | REQUEST_LOCATION;

    public static final int STATE_RED = LightPhase.RED.value;
    public static final int STATE_GREEN = LightPhase.GREEN.value;
    private static final int STATE_OFF = OFF.value;

    public static final int MAX_SNAPSHOTS = 3;

    private Button exitButton;
    private TextView txtCaption;
    private RelativeLayout rl;
    private CameraTexturePreview camPreview;
    private CompoundButton[] phaseSelect = new CompoundButton[3];
    private BadgeDrawable[] badges = new BadgeDrawable[3];
    private FloatingActionButton btnCapture;

    private Camera camera;
    private SensorManager mSensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private LocationService locationService;
    private MotionService motionService;

    private final TakePictureStateMachine stateMachine = new TakePictureStateMachine(this);
    private final CrosshairManager crosshair = new CrosshairManager();
    private List<LightGroup> lightGroups = new ArrayList<>();
    private boolean locationPermission;
    private boolean cameraPermission = false;

    private AlertDialog pictureHelpDialog;
    private ShowcaseHandler showcaseHandler;

    /* =======================================
     * ACTIVITY-LIFE-CYCLE:
     * -------------------------------------*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(FLAG_FULLSCREEN, FLAG_FULLSCREEN);
        setContentView(R.layout.activity_take_picture);

        ActivityRegistry.register(this);

        rl = (RelativeLayout) findViewById(R.id.take_picture_rl);

        exitButton = (Button) findViewById(R.id.takePicture_exitBtn);
        txtCaption = (TextView) findViewById(R.id.takePicture_caption);
        btnCapture = (FloatingActionButton) findViewById(R.id.takePicture_btn_capture);

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
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        layoutParams.addRule(CENTER_IN_PARENT);
        rl.addView(camPreview, 0, layoutParams);
        rl.post(new Runnable() {
            @Override
            public void run() {

                // this has to wait until the layout-process has finished for the first time:
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
            // fine
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
//        if (true) {
        if (UserPreference.shouldShowDialog(getApplicationContext(), ShowcaseHandler.SETTINGS_KEY)) {
            showcaseHandler = new ShowcaseHandler();
            showcaseHandler.startShowcase();
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
        this.finish();
    }

    @Override
    protected void onDestroy() {
        if (LOG) Log.d(TAG, "onDestroy");
        super.onDestroy();

        locationService.stopListening();
        mSensorManager.unregisterListener(motionService.getEventListener());
    }

    /* =======================================
     * EVENT-HANDLING:
     * -------------------------------------*/

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        btnCapture.setEnabled(true);

        Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
        bmp = MiscUtils.rotateImage(bmp, 90);

        final PhaseContext context = stateMachine.currentContext();
        final LightPhase phase = context.phase;


        Location loc = null;
        if (locationService != null && locationService.canGetLocation())
            loc = locationService.getLocation();

        float gyro = 0;
        if (motionService != null)
            gyro = motionService.getPitch();

        PointF crossCenter = crosshair.currentPos();
        PointF crossLight = new PointF(crossCenter.x, crossCenter.y);

        // if this is a red picture the light is a little above
        // if it's a green picture it's bellow the crossHair center
        float shiftY = 0;
        switch (phase) {
            case RED:
                shiftY = -crosshair.crossHairCenterToLight;
                break;
            case GREEN:
                shiftY = crosshair.crossHairCenterToLight;
                break;
            case OFF:
                shiftY = 0;
                break;
        }
        crossLight.y += shiftY / camPreview.getZoom();

        Log.d(TAG, "===> set Position: {0} , {1}", crossLight.x, crossLight.y);

        LightPosition pos = new LightPosition(crossLight.x, crossLight.y, phase, true);

        String uid = UserInformation.shared.getUserId();

        LightGroup lightGroup;
        int photoNum = context.getSnapshotCount();
        if (photoNum >= lightGroups.size()) {
            if (LOG) Log.d(TAG, "create new lightGroup");
            lightGroup = LightGroup.create();
            lightGroups.add(lightGroup);
        } else {
            if (LOG) Log.d(TAG, "use existing lightGroup");
            lightGroup = lightGroups.get(photoNum);
        }

        int credits;
        switch (phase) {
            case GREEN:
                credits = 2;
                break;
            case RED:
            case OFF:
            default:
                credits = 1;
                break;
        }

        Photo snapshotData = Photo.buildNew()
                .setUser(uid)
                .setBitmap(bmp)
                .addLightPos(pos)
                .setLocation(loc)
                .setGyro(gyro)
                .setCredits(credits)
                .commit();

        lightGroup.put(snapshotData);

        TakePictureActivity.this.stateMachine.snapshotTaken(snapshotData);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        for (int i = 0; i < permissions.length; i++) {
            if (LOG)
                Log.d(TAG, "onRequestPermissionsResult: code: {0}, relult: {1}", permissions[i], grantResults[i]);

            switch (permissions[i]) {
                case Manifest.permission.CAMERA:
                    cameraPermission = grantResults[i] == PackageManager.PERMISSION_GRANTED;
                    if (!cameraPermission) {
                        showCameraRequestDialog();
                        Toast.makeText(this, "LightsCatcher funktioniert nicht ohne Zugriff auf die Kamera!", Toast.LENGTH_LONG).show();
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
    public void onBackPressed() {
        navigateBack(null);
        super.onBackPressed();
    }

    public void onCaptureButtonPressed(View view) {
        if (!cameraPermission || camPreview.getCamera() == null) {
            Toast.makeText(this.getApplicationContext(), "Kamera nicht verfügbar :(", Toast.LENGTH_SHORT).show();
            return;
        }
        btnCapture.setEnabled(false);
//        camera.takePicture(null, null, TakePictureActivity.this);
        Log.d(TAG, "active focus-mode: " + camera.getParameters().getFocusMode());
        switch (camera.getParameters().getFocusMode()) {
            case Camera.Parameters.FOCUS_MODE_AUTO:
            case Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE:
            case Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO:

                camera.autoFocus(new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean success, Camera camera) {
                        if (!success)
                            Log.e(TAG, "no autofocus");
                        camera.takePicture(null, null, TakePictureActivity.this);
                        camera.cancelAutoFocus();
                    }
                });
            default:
                camera.takePicture(null, null, TakePictureActivity.this);
        }
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

    public void zoomIn_Click(View view) {
        camPreview.zoomIn();
    }

    public void zoomOut_Click(View view) {
        camPreview.zoomOut();
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
            if (camera != null)
                camera.startPreview();
        }

    };

    private BadgeDrawable createBadge(CompoundButton btn) {
        StateListDrawable background = (StateListDrawable) btn.getBackground();
        BadgeDrawable badgeDrawable = new BadgeDrawable(this);

        LayerDrawable localLayerDrawable = new LayerDrawable(new Drawable[]{background, badgeDrawable});
        btn.setBackgroundDrawable(localLayerDrawable);
        return badgeDrawable;
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
            camera.setDisplayOrientation(90);
        } catch (Exception e) {
            // Camera is not available (in use or does not exist)
            Log.e(TAG, e);
        }
        return camera; // returns null if camera is unavailable
    }

    /**
     * Updates the UI to reflect the given lightphase
     */
    private void notifyNextPhase(PhaseContext newContext, PhaseContext oldContext) {


        int phase = newContext.phase.value;

        this.phaseSelect[phase].setChecked(true);
        crosshair.internalMarker[phase].setVisibility(View.VISIBLE);
        txtCaption.setText(getString(R.string.fotografieren, newContext.phase.getGermanText()));
        if (oldContext != null) {
            int oldPhase = oldContext.phase.value;
            phaseSelect[oldPhase].setChecked(false);
            crosshair.internalMarker[oldPhase].setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Updates the UI to show that the snapshot was taken and committed
     */
    private void notifySnapshotCommitted(PhaseContext context, Photo snapshot) {
        BadgeDrawable view = badges[context.phase.value];
        if (view != null) {
            view.setCount(context.getSnapshotCount());
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

    public void navigateBack(View view) {
        Intent intent = new Intent(getApplicationContext(), HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    private void showSubmitPopup(Photo snapshotData) {
        final SubmitDialog popup = new SubmitDialog.Builder(submitListener)
                .setPhoto(snapshotData)
                .build();


        if (UserPreference.shouldShowDialog(getApplicationContext(), SubmitDialog.ShowcaseHandler.SETTINGS_KEY)) {
            SubmitDialog.ShowcaseHandler submitShowCase = new SubmitDialog.ShowcaseHandler(this);
            submitShowCase.startShowcase(new Runnable() {
                @Override
                public void run() {
                    popup.show(getSupportFragmentManager(), SubmitDialog.TAG);
                }
            });
        } else {
            popup.show(getSupportFragmentManager(), SubmitDialog.TAG);
        }
    }

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
                .setNeutralButton("App-Einstellungen", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final Intent i = new Intent();
                        i.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        i.addCategory(Intent.CATEGORY_DEFAULT);
                        i.setData(Uri.parse("package:" + getApplicationContext().getPackageName()));
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                        i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                        startActivity(i);
                    }
                })
                .create();
        dlg.show();
    }

    private void maximumSnapshotAlertDialog(final LightPhase nextPhase) {
        LayoutInflater adbInflater = LayoutInflater.from(this);
        final View dlgLayout = adbInflater.inflate(R.layout.dialog_max_snapshot_alert, null);

        final CheckBox neverShowAgain = (CheckBox) dlgLayout.findViewById(R.id.dialog_skip);
        neverShowAgain.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                UserPreference.neverShowAgain(getApplicationContext(), MAXIMUM_SNAPSHOT_ALERT, isChecked);
            }
        });


        AlertDialog.Builder dlg = new AlertDialog.Builder(this);
        if (nextPhase != null) {
            dlg.setPositiveButton("OK, auf " + nextPhase.getGermanText() + " umschalten.", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {


                    stateMachine.switchPhase(nextPhase);
                }
            });
        }
        dlg.setNegativeButton("Vorgang Abschließen", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                leave();
            }
        });
        dlg.setView(dlgLayout);
        dlg.create().show();
    }

    public class CrosshairManager {
        public final String TAG = "CrosshairManager";

        // crosshair constraints (center-coordinates relative to layoutContainer)
        public static final float CROSSHAIR_X_MIN = .25f;
        public static final float CROSSHAIR_X_MAX = 1f - CROSSHAIR_X_MIN;
        public static final float CROSSHAIR_Y_MIN = .2f;
        public static final float CROSSHAIR_Y_MAX = .45f;
        private final Random random = new Random();

        private View crosshairView;
        private View[] internalMarker = new View[3];
        private float crossHairCenterToLight;
        private final List<PointF> positions = new ArrayList<>();
        private int cursor;

        public PointF currentPos() {
            return positions.get(cursor);
        }

        public void setPositionContextBasesd() {
            PhaseContext context = stateMachine.currentContext();
            int photoNum = context != null ? context.snapshots.size() : 0;

            PointF pos;
            if (photoNum == positions.size()) {
                // if position is not defined already, ...
                if (LOG) Log.d(TAG, "setPositionContextBasesd: generate new position");
                // ... take random values
                pos = generateRandomPos();
                // remember this position
                positions.add(pos);
            } else {
                if (LOG) Log.d(TAG, "setPositionContextBasesd: reuse position");
                // or else re-use the same position
            }

            pos = positions.get(photoNum);
            setPositionOfView(pos);
            cursor = photoNum;
        }

        private PointF generateRandomPos() {
            float x = MiscUtils.randomFloat(CROSSHAIR_X_MIN, CROSSHAIR_X_MAX);
            float y = MiscUtils.randomFloat(CROSSHAIR_Y_MIN, CROSSHAIR_Y_MAX);

            if (showcaseHandler != null) {
                y = CROSSHAIR_Y_MIN;
            }
            return new PointF(x, y);
        }

        private void setPositionOfView(PointF pnt) {
            float x = pnt.x;
            float y = pnt.y;
            if (LOG) Log.d(TAG, "setPositionOfView: " + x + "; " + y);

            int parentW = camPreview.getWidth();
            int parentH = camPreview.getHeight();

            float density = getApplicationContext().getResources().getDisplayMetrics().density;

            int viewHeight = dp(50 * 2, density);
            int viewWidth = dp(50, density);

            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(viewWidth, viewHeight);
            params.leftMargin = (int) (parentW * x - (float) viewWidth / 2);
            params.topMargin = (int) (parentH * y - (float) viewHeight / 2);

            if (crosshairView == null) {
                LayoutInflater inflater = getLayoutInflater();
                crosshairView = inflater.inflate(R.layout.view_camera_crosshair, null);
                rl.addView(crosshairView, params);

                this.internalMarker[STATE_RED] = crosshairView.findViewById(R.id.takePicture_redCross);
                this.internalMarker[STATE_GREEN] = crosshairView.findViewById(R.id.takePicture_greenCross);
                this.internalMarker[STATE_OFF] = new View(TakePictureActivity.this);
            } else {
                crosshairView.setLayoutParams(params);
            }

            // set pivot point, important for zoom and camera-focus
            camPreview.setPivot(new PointF(x, y));


            // distance between light-position (colored cross) and center of the crosshair-border (coordinates specified by parameters x and y)
            crossHairCenterToLight = (float) viewHeight / parentH / 4;
        }
    }

    /**
     * This class manages the workflow of the {@link TakePictureActivity}.
     */
    private class TakePictureStateMachine {

        private final TakePictureActivity owner;
        private final PhaseContext[] stateContext = new PhaseContext[]{new PhaseContext(RED), new PhaseContext(GREEN), new PhaseContext(OFF)};
        private LightPhase current;

        public PhaseContext currentContext() {
            if (current == null) return null;
            return getContext(current);
        }

        public LightPhase currentPhase() {
            return current;
        }

        private PhaseContext getContext(LightPhase phase) {
            if (phase == null)
                throw new IllegalArgumentException("'phase' was null but is required");
            return stateContext[phase.value];
        }

        private void setCurrent(LightPhase phase) {
            current = phase;
        }

        private void clear() {
            stateContext[0].clear();
            stateContext[1].clear();
            stateContext[2].clear();
        }


        TakePictureStateMachine(TakePictureActivity owner) {
            this.owner = owner;
        }

        boolean switchPhase(LightPhase newPhase) {
            if (newPhase == null)
                throw new IllegalArgumentException("'newPhase' was null but is required");

            if (this.current != null && this.current.value == newPhase.value)
                return false;

            PhaseContext oldCtx = currentContext();
            PhaseContext newCtx = getContext(newPhase);


//            // allow maximal 3 pictures per phase per traffic-light
//            int count = newCtx.snapshots.size();
//            if (count >= MAX_SNAPSHOTS) {
//
//                LightPhase nextPhase = getNextPhase();
//                if (nextPhase != null)
//                    if (UserPreference.shouldShowDialog(owner.getApplicationContext(), MAXIMUM_SNAPSHOT_ALERT)) {
//                        maximumSnapshotAlertDialog(nextPhase);
//                        return false;
//                    } else {
//                        return switchPhase(nextPhase);
//                    }
//                else
//                    return false;
//            }

            current = newPhase;

            // reposition the crosshair
            crosshair.setPositionContextBasesd();

            // update UI
            owner.notifyNextPhase(newCtx, oldCtx);
            return true;
        }

        void snapshotTaken(Photo snapshot) {
            owner.showSubmitPopup(snapshot);
        }

        void snapshotCommitted(Photo snapshot) {

            PhaseContext context = currentContext();

            // add the photo
            context.addSnapshot(snapshot);

            // show visual feedback, that the snapshot was taken
            owner.notifySnapshotCommitted(context, snapshot);

//            // allow maximal 3 pictures per phase per traffic-light
//            int count = context.snapshots.size();
//            if (count >= MAX_SNAPSHOTS) {
//
//                LightPhase nextPhase = getNextPhase();
//                if (UserPreference.shouldShowDialog(owner.getApplicationContext(), MAXIMUM_SNAPSHOT_ALERT)) {
//                    maximumSnapshotAlertDialog(nextPhase);
//                } else {
//                    if (nextPhase != null)
//                        switchPhase(nextPhase);
//                    else
//                        leave();
//                }
//
//            } else {
//                // change the position of the crosshair
            crosshair.setPositionContextBasesd();
//            }


        }

        LightPhase getNextPhase() {
            switch (this.current) {
                case RED:
                    return GREEN;
                case OFF:
                    return RED;
                default:
                case GREEN:
                    return null; // null signals that we should finish this activity
            }
        }

        void leave() {
            this.owner.leave();
        }

        void reset() {
            // update UI
            owner.notifyReset();

            // switch to default state
            this.switchPhase(RED);
        }
    }

    private class PhaseContext {
        public final LightPhase phase;
        private final List<Photo> snapshots = new ArrayList<>();


        private PhaseContext(LightPhase phase) {
            this.phase = phase;
        }

        public void clear() {
            snapshots.clear();
        }

        public void addSnapshot(Photo photo) {
            snapshots.add(photo);
        }

        public int getSnapshotCount() {
            return snapshots.size();
        }
    }

    public class ShowcaseHandler implements View.OnClickListener {
        public static final String SETTINGS_KEY = "HELP_TAKE_PICTURE";

        int counter = 0;
        private ShowcaseView showcaseView;

        public void startShowcase() {
            RelativeLayout.LayoutParams buttonParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            buttonParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            buttonParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
//            buttonParams.setMargins(10, 10, 10, dp(getApplicationContext(), 100));

            showcaseView = new ShowcaseView.Builder(TakePictureActivity.this)
                    .withHoloShowcase2()
                    .setTarget(new ViewTarget(findViewById(R.id.takePicture_layout_selectPhase)))
                    .setContentText("Wenn Du eine Fußgängerampel gefunden hast, wähle zuerst die Phase der Fußgängerampel: Rot, grün oder ausgeschaltet.\n\nFür ein Foto gibt es je nach Phase unterschiedlich viele Punkte:\n\trot:\t 1 Punkt\n\tgrün:\t 2 Punkte\n\tausgeschaltet:\t1 Punkt\n\nHINWEIS: Bitte lauf noch nicht nicht los, während du die Grünphase fotografierst, wir bekommen sonst nur verwackelte Bilder. Für gute \"grüne\" Bilder gibt es doppelte Punkte.")
                    .setStyle(R.style.CustomShowcaseTheme)
                    .setOnClickListener(this)
                    .build();
            showcaseView.setButtonText("Weiter");
            showcaseView.setButtonPosition(buttonParams);
        }

        @Override
        public void onClick(View v) {
            switch (counter) {

                case 0:
                    showcaseView.setShowcase(new ViewTarget(crosshair.crosshairView), false);
                    showcaseView.setContentText("Ziele mit dem farbigen Plus auf das Licht der Fußgängerampel. Wenn mehrere Ampeln im selben Bildausschnitt zu sehen sind, wähle die vorderste Fußgängerampel.\n\nDas Fadenkreuz wird nach jedem Foto zufällig plaziert, damit Fotos aus unterschiedlichen Perspektiven entstehen.");
                    break;
                case 1:
                    showcaseView.setShowcase(new ViewTarget(findViewById(R.id.takePicture_layout_zoom)), false);
                    showcaseView.setContentText("Du kannst auch zoomen.\n\nIdealerweise stellst du den Zoom so ein, dass das schwarze Ampelgehäuse mit dem hellblauen Rahmen übereinstimmt.\n\nDie Zoom-Geste mit 2 Fingern (Pinch Zoom) funktioniert auch.");

                    break;
                case 2:
                    showcaseView.setShowcase(new ViewTarget(btnCapture), false);
                    showcaseView.setContentText("Wenn alles passt, dann drücke den Auslöser.");
                    showcaseView.setButtonText("Schließen");
                    break;
                case 3:
                default:
                    showcaseView.hide();
                    TakePictureActivity.this.showcaseHandler = null;
                    UserPreference.neverShowAgain(getApplicationContext(), SETTINGS_KEY, true);
                    break;
            }

            counter++;
        }
    }

}
