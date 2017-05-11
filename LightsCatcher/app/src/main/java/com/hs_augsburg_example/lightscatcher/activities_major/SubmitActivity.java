package com.hs_augsburg_example.lightscatcher.activities_major;

import android.content.Intent;
import android.graphics.PointF;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.hs_augsburg_example.lightscatcher.R;
import com.hs_augsburg_example.lightscatcher.activities_minor.LoginActivity;
import com.hs_augsburg_example.lightscatcher.dataModels.LightPosition;
import com.hs_augsburg_example.lightscatcher.dataModels.Photo;
import com.hs_augsburg_example.lightscatcher.dataModels.Record;
import com.hs_augsburg_example.lightscatcher.singletons.UserInformation;
import com.hs_augsburg_example.lightscatcher.utils.ActivityRegistry;
import com.hs_augsburg_example.lightscatcher.utils.Log;
import com.hs_augsburg_example.lightscatcher.utils.UserPreference;

import java.util.List;

@Deprecated
public class SubmitActivity extends AppCompatActivity implements OnFailureListener {
    private static final String TAG = "TagLightsActivity";
    private static final boolean LOG = Log.ENABLED && true;

    private ProgressBar progressBar;
    private SubsamplingScaleImageView photoTop;
    private SubsamplingScaleImageView photoBottom;

    // data-object containing photos
    private Record.Builder recordBuilder;

    private FirebaseDatabase mDatabaseRef;
    private View btnUpload;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityRegistry.register(this);

        setContentView(R.layout.activity_submit);

        btnUpload = findViewById(R.id.submit_btn_upload);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        photoTop = initPhotoView(recordBuilder.record.redPhoto, (FrameLayout) findViewById(R.id.submit_frameTop), (FrameLayout) findViewById(R.id.submit_frameBottom));
        photoBottom = initPhotoView(recordBuilder.record.greenPhoto, (FrameLayout) findViewById(R.id.submit_frameBottom), (FrameLayout) findViewById(R.id.submit_frameTop));

        mDatabaseRef = FirebaseDatabase.getInstance();
        recordBuilder = Record.latestRecord;
    }

    private SubsamplingScaleImageView initPhotoView(final Photo photo, FrameLayout targetFrame, FrameLayout otherFrame) {
        if (photo != null) {
            targetFrame.setVisibility(View.VISIBLE);

            final SubsamplingScaleImageView photoView = (SubsamplingScaleImageView) targetFrame.getChildAt(0);

            photoView.setImage(ImageSource.bitmap(photo.bitMap));
            LightPosition pos = photo.lightPositions.getMostRelevant();
            // photoView eats absolute positions
            float x = (float) (photo.bitMap.getWidth() * pos.x);
            float y = (float) (photo.bitMap.getHeight() * pos.y);
            photoView.setScaleAndCenter(2, new PointF(x, y));

            return photoView;
        } else {
            otherFrame.setVisibility(View.GONE);
            return null;
        }
    }

    public void onUploadPressed(View v) {
        setUiBusy(true);
        mDatabaseRef.getReference("bannedUsers").child(UserInformation.shared.getUserId()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() == null) {
                    //continue
                    uploadPhoto();
                } else {
                    //banned
                    UserPreference.setUserBanned(SubmitActivity.this.getApplicationContext());
                    UserInformation.shared.logout();
                    Intent i = new Intent(SubmitActivity.this, LoginActivity.class);
                    startActivity(i);
                    finish();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast toast = Toast.makeText(getApplicationContext(), "Dein Benutzeraccount kann nicht überprüft werden. Probiere es später einfach nochmal ;)", Toast.LENGTH_LONG);
                toast.show();
                setUiBusy(false);
            }
        });
    }

    private void uploadPhoto() {
//
//        // read out current light-position from photoview:
//        if (photoTop != null) {
//            Photo ph = recordBuilder.record.redPhoto;
//            PointF center = photoTop.getCenter();
//            ph.lightPos.x = center.x * ph.bitMap.getWidth();
//            ph.lightPos.y = center.y * ph.bitMap.getHeight();
//        }
//        if (photoBottom != null) {
//            Photo ph = recordBuilder.record.greenPhoto;
//            PointF center = photoBottom.getCenter();
//            ph.lightPos.x = center.x * ph.bitMap.getWidth();
//            ph.lightPos.y = center.y * ph.bitMap.getHeight();
//        }
//
//        // Check for valid data
//        Record rec = recordBuilder.commit();
//
//        // save database entry
//        PersistenceManager.shared.persist(rec);
//
//        // give credits
//        UserInformation.shared.increaseUserPoints(rec.points);
//
//        Context ctx = getApplicationContext();
//        // upload red photo
//        if (rec.redPhoto != null) {
//            try {
//                PersistenceManager.shared.uploadLightsImage(ctx, rec.redPhoto);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//
//        // upload green photo
//        if (rec.redPhoto != null) {
//            try {
//                PersistenceManager.shared.uploadLightsImage(ctx, rec.redPhoto.id);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//
        Intent intent = new Intent(SubmitActivity.this, FinishActivity.class);
        startActivity(intent);
        finish();
    }

    private void setUiBusy(boolean busy) {
        progressBar.setVisibility(busy ? View.VISIBLE : View.GONE);
        btnUpload.setEnabled(!busy);
    }


    @Override
    public void onFailure(@NonNull Exception e) {
        progressBar.setVisibility(View.GONE);
        Toast toast = Toast.makeText(getApplicationContext(), "Upload nicht erfolgreich!!!", Toast.LENGTH_LONG);
        toast.show();
    }


    public void undoBtnPressed(View view) {
        // submitDiscarded this recordBuilder.
        Record.latestRecord = null;
        this.finish();
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
}
