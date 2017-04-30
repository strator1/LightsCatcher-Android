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
import com.github.chrisbanes.photoview.PhotoView;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.hs_augsburg_example.lightscatcher.R;
import com.hs_augsburg_example.lightscatcher.activities_minor.LoginActivity;
import com.hs_augsburg_example.lightscatcher.dataModels.Photo;
import com.hs_augsburg_example.lightscatcher.dataModels.Record;
import com.hs_augsburg_example.lightscatcher.singletons.PersistenceManager;
import com.hs_augsburg_example.lightscatcher.singletons.UserInformation;
import com.hs_augsburg_example.lightscatcher.utils.ActivityRegistry;
import com.hs_augsburg_example.lightscatcher.utils.UserPreference;


public class SubmitActivity extends AppCompatActivity implements OnFailureListener {

    private ProgressBar progressBar;
    private PhotoView photoTop;
    private PhotoView photoBottom;

    // data-object containing photos
    private Record.Builder recordBuilder;

    private FirebaseAuth mAuthRef;
    private FirebaseDatabase mDatabaseRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityRegistry.register(this);

        setContentView(R.layout.activity_submit);
        recordBuilder = Record.latestRecord;

        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        initPhotoView(recordBuilder.record.redPhoto, (FrameLayout) findViewById(R.id.submit_frameTop), (FrameLayout) findViewById(R.id.submit_frameBottom));
        initPhotoView(recordBuilder.record.greenPhoto, (FrameLayout) findViewById(R.id.submit_frameBottom), (FrameLayout) findViewById(R.id.submit_frameTop));

        mAuthRef = FirebaseAuth.getInstance();
        mDatabaseRef = FirebaseDatabase.getInstance();
    }

    private void initPhotoView(final Photo photo, FrameLayout targetFrame, FrameLayout otherFrame) {
        if (photo != null) {
            targetFrame.setVisibility(View.VISIBLE);
            final SubsamplingScaleImageView photoView = (SubsamplingScaleImageView) targetFrame.getChildAt(0);
            //photoView.setImageBitmap(data.bitMap);


            photoView.setImage(ImageSource.bitmap(photo.bitMap));

//                    PointF marker = new PointF(data.lightPos.x,data.lightPos.y);
            PointF marker = new PointF(photo.bitMap.getWidth() / 2, photo.bitMap.getHeight());
            photoView.setScaleAndCenter(2, marker);

            //photoTop = photoView;
            //photoView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            //photoView.setScale(2f,true);
//            photoView.post(new Runnable() {
//                @Override
//                public void run() {
//
//                }
//            });
        } else {
            otherFrame.setVisibility(View.GONE);
        }
    }

    public void onUploadPressed(View v) {
        mDatabaseRef.getReference("bannedUsers").child(UserInformation.shared.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
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
                Toast toast = Toast.makeText(getApplicationContext(), "Du scheinst momentan keine Internetverbindung zu haben. Probiere es später einfach nochmal ;)", Toast.LENGTH_LONG);
                toast.show();
            }
        });
    }

    private void uploadPhoto(){
        // Check for valid data
        Record rec = recordBuilder.commit();

        // one of the photos, either red or green may be null
        final Photo photo1, photo2;
        if (rec.redPhoto != null) {
            photo1 = rec.redPhoto;
            photo2 = rec.greenPhoto;
        } else {
            photo1 = rec.greenPhoto;
            photo2 = null;
        }

          /*
        UploadTask task1;
        task1 = PersistenceManager.shared.persistStorage(photo1.id, photo2.bitMap);
        task1.addOnFailureListener(this);
        task1.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                photo1.imageUrl = taskSnapshot.getDownloadUrl().toString();

                if (photo2 == null)
                    SubmitActivity.this.onUploadComplete(rec);
                else {
                    UploadTask task2 = PersistenceManager.shared.persistStorage(photo2.id, photo2.bitMap);
                    task2.addOnFailureListener(SubmitActivity.this);
                    task2.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            photo2.imageUrl = taskSnapshot.getDownloadUrl().toString();
                            SubmitActivity.this.onUploadComplete(rec);
                        }
                    });
                }
            }
        });
        */
    }

    private void onUploadComplete(Record rec) {
        Task task3 = PersistenceManager.shared.persist(rec);
        task3.addOnFailureListener(SubmitActivity.this);
        task3.addOnSuccessListener(new OnSuccessListener() {

            @Override
            public void onSuccess(Object o) {
                Intent intent = new Intent(SubmitActivity.this, FinishActivity.class);
                startActivity(intent);
                finish();
            }
        });

    }


    @Override
    public void onFailure(@NonNull Exception e) {
        progressBar.setVisibility(View.GONE);
        Toast toast = Toast.makeText(getApplicationContext(), "Upload nicht erfolgreich!!!", Toast.LENGTH_LONG);
        toast.show();
    }


    public void undoBtnPressed(View view) {
        // discard this recordBuilder.
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
