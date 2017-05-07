package com.hs_augsburg_example.lightscatcher.camera;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.hs_augsburg_example.lightscatcher.FinishActivity;
import com.hs_augsburg_example.lightscatcher.LoginActivity;
import com.hs_augsburg_example.lightscatcher.R;
import com.hs_augsburg_example.lightscatcher.dataModels.Light;
import com.hs_augsburg_example.lightscatcher.singletons.LightInformation;
import com.hs_augsburg_example.lightscatcher.singletons.PersistenceManager;
import com.hs_augsburg_example.lightscatcher.singletons.PhotoInformation;
import com.hs_augsburg_example.lightscatcher.singletons.UserInformation;
import com.hs_augsburg_example.lightscatcher.utils.ActivityRegistry;
import com.hs_augsburg_example.lightscatcher.utils.LightUploadMonitor;
import com.hs_augsburg_example.lightscatcher.utils.Log;
import com.hs_augsburg_example.lightscatcher.utils.UserPreference;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.hs_augsburg_example.lightscatcher.singletons.PhotoInformation.LightPhase.GREEN;
import static com.hs_augsburg_example.lightscatcher.singletons.PhotoInformation.LightPhase.RED;

public class TagLightsActivity extends AppCompatActivity implements View.OnTouchListener, ViewTreeObserver.OnPreDrawListener {

    private static final String TAG = "TagLightsActivity";
    private static final boolean LOG = Log.ENABLED && true;

    private ProgressBar progressBar;
    private ImageView imageView;
    private Bitmap image;
    private RelativeLayout rl;
    private FloatingActionButton undoBtn;

    private List<LightInformation> insertedViews = new ArrayList<LightInformation>();
    private List<LightInformation> pickedUpViews = new ArrayList<LightInformation>();
    private int ivHeight;
    private int ivWidth;

    private long lastTouchDown;
    private int CLICK_ACTION_THRESHHOLD = 200;

    private StorageReference mStorageRef;
    private FirebaseAuth mAuthRef;
    private FirebaseDatabase mDatabaseRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tag_lights);

        ActivityRegistry.register(this);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        imageView = (ImageView) findViewById(R.id.imageView);
        rl = (RelativeLayout) findViewById(R.id.tag_lights_rl);
        undoBtn = (FloatingActionButton) findViewById(R.id.button_undo);

        imageView.setOnTouchListener(this);

        ViewTreeObserver vto = imageView.getViewTreeObserver();
        vto.addOnPreDrawListener(this);

        if (PhotoInformation.shared.getImage() != null) {
            image = PhotoInformation.shared.getImage();
            imageView.setImageBitmap(image);

            LightInformation mostRel = PhotoInformation.shared.getMostRelevantPosition();

            if (mostRel != null) {
                addNewView(0, 0, mostRel);
            } else {
                undoBtn.setEnabled(false);
            }
        }

        mStorageRef = FirebaseStorage.getInstance().getReference();
        mAuthRef = FirebaseAuth.getInstance();
        mDatabaseRef = FirebaseDatabase.getInstance();

    }

    @Override
    public boolean onPreDraw() {
        imageView.getViewTreeObserver().removeOnPreDrawListener(this);
        ivHeight = imageView.getMeasuredHeight();
        ivWidth = imageView.getMeasuredWidth();
        return true;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int[] location = new int[2];
        v.getLocationOnScreen(location);

        int x = (int) event.getRawX();
        int y = (int) event.getRawY() - location[1];

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                lastTouchDown = System.currentTimeMillis();
                onActionDownTouch(x, y);
                break;
            case MotionEvent.ACTION_UP:
                if (System.currentTimeMillis() - lastTouchDown < CLICK_ACTION_THRESHHOLD) {
                    if (pickedUpViews.size() == 0) {
                        addNewView(x, y, null);
                    } else if (pickedUpViews.size() == 1) {
                        showLightPhaseAlertView(pickedUpViews.get(0), false);
                    }
                }
                onActionUpTouch();
                break;
            case MotionEvent.ACTION_MOVE:
                onActionMoveTouch(x, y);
                break;
            default:
                return v.onTouchEvent(event);
        }

        return true;
    }

    private void onActionDownTouch(int x, int y) {
        pickedUpViews = inViewInBounds(x, y);
    }

    private void onActionUpTouch() {
        // Drop nodes
        pickedUpViews = new ArrayList<LightInformation>();
    }

    private void onActionMoveTouch(int x, int y) {
        // Move nodes
        for (LightInformation pos : pickedUpViews) {
            pos.setPos(x, y);
        }
    }

    public void onUploadPressed(View v) {

        if (insertedViews.size() == 0) {
            Toast.makeText(this.getApplicationContext(), "Bitte erst mindestens eine Markierung hinzufügen! (Auf das Bild tippen)", Toast.LENGTH_LONG).show();
            return;
        }

        for (LightInformation pos : insertedViews) {
            pos.convertToAbsoluteXY(imageView, image);
        }
        if (PersistenceManager.shared.connectedListener.isConnected()) {

        }
        mDatabaseRef.getReference("bannedUsers").child(UserInformation.shared.getUserId()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() == null) {
                    //continue
                    uploadPhoto();
                } else {
                    //banned
                    UserPreference.setUserBanned(TagLightsActivity.this.getApplicationContext());
                    UserInformation.shared.logout();
                    Intent i = new Intent(TagLightsActivity.this, LoginActivity.class);
                    startActivity(i);
                    finish();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast toast = Toast.makeText(getApplicationContext(), "Du scheinst momentan keine Internetverbindung zu haben. Probiere es später einfach nochmal ;)", Toast.LENGTH_LONG);
                toast.show();
            }
        });
    }

    private void uploadPhoto() {
        PhotoInformation.shared.resetLightPositions();
        PhotoInformation.shared.setLightInformationList(insertedViews);
        final Light light = PhotoInformation.shared.getLight();

        progressBar.setVisibility(View.VISIBLE);
        final LightUploadMonitor monitor = LightUploadMonitor.newInstance(this.getApplicationContext());

        final String imageId = UUID.randomUUID().toString().toUpperCase();
        PersistenceManager.shared.persist(light, imageId);

        StorageTask uploadTask = null;
        try {
            uploadTask = PersistenceManager.shared.persistLightsImage(this.getApplicationContext(), imageId, image);
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), "Während dem Upload ist ein Fehler aufgetreten :(", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Exception beim Upload: " + e.getMessage(), e);
        }

        UserInformation.shared.increaseUserPoints(1);

        Intent intent = new Intent(TagLightsActivity.this, FinishActivity.class);
        startActivity(intent);
        finish();
    }

    private void addNewView(int x, int y, LightInformation existingPos) {
        if (insertedViews.size() >= 3) {
            return;
        }

        View v = new View(getApplicationContext());
        v.setBackgroundColor(Color.BLACK);

        LightInformation pos;

        if (existingPos == null) {
            pos = new LightInformation(v, getApplicationContext());

            boolean mostRelevantExists = false;
            for (LightInformation p : insertedViews) {
                if (p.isMostRelevant) {
                    mostRelevantExists = true;
                    break;
                }
            }

            pos.setMostRelevant(!mostRelevantExists);
            pos.setPos(x, y);
        } else {
            pos = existingPos;
            pos.setView(v);
        }

        undoBtn.setEnabled(true);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(pos.getWidth(), pos.getHeight());
        params.leftMargin = pos.getX();
        params.topMargin = pos.getY();

        rl.addView(v, params);
        insertedViews.add(pos);
        showLightPhaseAlertView(pos, true);
    }

    private AlertDialog lightPhaseDialog;
    private View lightPhaseDialogView;

    private void showLightPhaseAlertView(final LightInformation pos, final boolean isNew) {

        if (lightPhaseDialog == null) {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            LayoutInflater inflater = this.getLayoutInflater();

            lightPhaseDialogView = inflater.inflate(R.layout.content_lightpos_dialog, null);
            dialogBuilder.setView(lightPhaseDialogView);
            dialogBuilder.setTitle("Rot oder Grünphase?");

            lightPhaseDialog = dialogBuilder.create();
        }


        Button deleteButton = (Button) lightPhaseDialogView.findViewById(R.id.deleteButton);
        Button redButton = (Button) lightPhaseDialogView.findViewById(R.id.redButton);
        Button greenButton = (Button) lightPhaseDialogView.findViewById(R.id.greenButton);


        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rl.removeView(pos.getView());
                insertedViews.remove(pos);
                lightPhaseDialog.dismiss();
            }
        });

        redButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pos.setPhase(RED);
                lightPhaseDialog.dismiss();
            }
        });

        greenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pos.setPhase(GREEN);
                lightPhaseDialog.dismiss();
            }
        });

        lightPhaseDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Abbrechen", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (!isNew) {
                    return;
                }

                undoBtnPressed(null);
                lightPhaseDialog.dismiss();
            }
        });

        lightPhaseDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                if (!isNew) {
                    return;
                }

                undoBtnPressed(null);
            }
        });

        lightPhaseDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if (UserPreference.isFirstTagging(getApplicationContext())) {
                    infoBtnPressed(null);
                }
            }
        });

        if (!isNew) {
            deleteButton.setVisibility(View.VISIBLE);
        }

        lightPhaseDialog.show();
    }

    public void undoBtnPressed(View view) {
//        if (insertedViews.size() == 0) {
//            return;
//        }

        LightInformation lastPos = insertedViews.get(insertedViews.size() - 1);
        rl.removeView(lastPos.getView());
        insertedViews.remove(lastPos);

        if (insertedViews.size() == 0) {
            undoBtn.setEnabled(false);
        }
    }

    private AlertDialog lightPhaseHelpDialog;
    private View lightPhaseHelpDialogView;

    public void infoBtnPressed(View view) {
        if (lightPhaseHelpDialog == null) {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            LayoutInflater inflater = this.getLayoutInflater();

            lightPhaseHelpDialogView = inflater.inflate(R.layout.content_lightpos_helpdialog, null);
            dialogBuilder.setView(lightPhaseHelpDialogView);
            dialogBuilder.setTitle("Erste Hilfe");

            dialogBuilder.setPositiveButton("Verstanden", null);

            lightPhaseHelpDialog = dialogBuilder.create();
        }

        lightPhaseHelpDialog.show();
    }

    private List<LightInformation> inViewInBounds(int x, int y) {
        List<LightInformation> foundViews = new ArrayList<LightInformation>();

        for (LightInformation pos : insertedViews) {
            if (pos.containsTouchPosition(x, y)) {
                foundViews.add(pos);
            }
        }

        return foundViews;
    }

}
