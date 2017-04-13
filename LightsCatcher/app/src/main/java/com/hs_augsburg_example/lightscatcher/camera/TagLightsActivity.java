package com.hs_augsburg_example.lightscatcher.camera;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.PopupMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.hs_augsburg_example.lightscatcher.FinishActivity;
import com.hs_augsburg_example.lightscatcher.HomeActivity;
import com.hs_augsburg_example.lightscatcher.LoginActivity;
import com.hs_augsburg_example.lightscatcher.R;
import com.hs_augsburg_example.lightscatcher.dataModels.LightPosition;
import com.hs_augsburg_example.lightscatcher.dataModels.PhotoInformation;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.hs_augsburg_example.lightscatcher.dataModels.PhotoInformation.LightPhase.GREEN;
import static com.hs_augsburg_example.lightscatcher.dataModels.PhotoInformation.LightPhase.RED;

public class TagLightsActivity extends AppCompatActivity implements View.OnTouchListener, ViewTreeObserver.OnPreDrawListener {

    private ProgressBar progressBar;
    private ImageView imageView;
    private Bitmap image;
    private RelativeLayout rl;

    private List<LightPosition> insertedViews = new ArrayList<LightPosition>();
    private List<LightPosition> pickedUpViews = new ArrayList<LightPosition>();
    private int ivHeight;
    private int ivWidth;

    private long lastTouchDown;
    private int CLICK_ACTION_THRESHHOLD = 200;

    private StorageReference mStorageRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tag_lights);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        imageView = (ImageView) findViewById(R.id.imageView);
        rl = (RelativeLayout) findViewById(R.id.tag_lights_rl);

        imageView.setOnTouchListener(this);

        ViewTreeObserver vto = imageView.getViewTreeObserver();
        vto.addOnPreDrawListener(this);

        if (PhotoInformation.shared.getImage() != null) {
            image = PhotoInformation.shared.getImage();
            imageView.setImageBitmap(image);

            LightPosition mostRel = PhotoInformation.shared.getMostRelevantPosition();

            if (mostRel != null) {
                addNewView(0, 0, mostRel);
            }
        }

        mStorageRef = FirebaseStorage.getInstance().getReference();

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
                if (System.currentTimeMillis() - lastTouchDown < CLICK_ACTION_THRESHHOLD)  {
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
        pickedUpViews = new ArrayList<LightPosition>();
    }

    private void onActionMoveTouch(int x, int y) {
        // Move nodes
        for (LightPosition pos : pickedUpViews) {
            pos.setPos(x, y);
        }
    }

    public void onUploadPressed(View v) {
        for(LightPosition pos : insertedViews) {
            pos.convertToAbsoluteXY(imageView, image);
        }

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 100, baos);

        StorageReference storageReference = mStorageRef.child("lights_images").child(UUID.randomUUID().toString().toUpperCase());

        progressBar.setVisibility(View.VISIBLE);
        UploadTask uploadTask = storageReference.putBytes(baos.toByteArray());

        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast toast = Toast.makeText(getApplicationContext(), "Upload not successful", Toast.LENGTH_LONG);
                toast.show();
            }
        });

        uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                progressBar.setVisibility(View.GONE);
                Toast toast = Toast.makeText(getApplicationContext(), "Upload successful!!!!", Toast.LENGTH_LONG);
                toast.show();
            }
        });

        Intent intent = new Intent(TagLightsActivity.this, FinishActivity.class);
        startActivity(intent);
        finish();

        try {
            baos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addNewView(int x, int y, LightPosition existingPos) {
        if (insertedViews.size() >= 3) {
            return;
        }

        View v = new View(getApplicationContext());
        v.setBackgroundColor(Color.BLACK);

        LightPosition pos;

        if (existingPos == null) {
            pos = new LightPosition(v, getApplicationContext());
            pos.setPos(x, y);
        } else {
            pos = existingPos;
            pos.setView(v);
        }

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(pos.getWidth(), pos.getHeight());
        params.leftMargin = pos.getX();
        params.topMargin = pos.getY();

        rl.addView(v, params);
        insertedViews.add(pos);
        showLightPhaseAlertView(pos, true);
    }

    private void showLightPhaseAlertView(final LightPosition pos, final boolean isNew) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();

        View dialogView = inflater.inflate(R.layout.content_lightpos_dialog, null);
        dialogBuilder.setView(dialogView);
        dialogBuilder.setTitle("\uD83D\uDEA6 Rot oder Grünphase?");

        dialogBuilder.setPositiveButton("Abbrechen", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (!isNew) {
                    return;
                }

                LightPosition lastPos = insertedViews.get(insertedViews.size() - 1);
                rl.removeView(lastPos.getView());
                insertedViews.remove(lastPos);
            }
        });

        Button deleteButton = (Button) dialogView.findViewById(R.id.deleteButton);
        Button redButton = (Button) dialogView.findViewById(R.id.redButton);
        Button greenButton = (Button) dialogView.findViewById(R.id.greenButton);

        if (!isNew) {
            deleteButton.setVisibility(View.VISIBLE);
        }

        final AlertDialog alertDialog = dialogBuilder.create();

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.println("Delete pressed");
                rl.removeView(pos.getView());
                insertedViews.remove(pos);
                alertDialog.dismiss();
            }
        });

        redButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.println("red pressed");
                pos.setPhase(RED);
                alertDialog.dismiss();
            }
        });

        greenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.println("green pressed");
                pos.setPhase(GREEN);
                alertDialog.dismiss();
            }
        });

        alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                if (!isNew) {
                    return;
                }

                undoBtnPressed(null);
            }
        });
        alertDialog.show();
    }

    public void undoBtnPressed(View view) {
        if (insertedViews.size() == 0) {
            return;
        }

        LightPosition lastPos = insertedViews.get(insertedViews.size() - 1);
        rl.removeView(lastPos.getView());
        insertedViews.remove(lastPos);
    }

    public void infoBtnPressed(View view) {
    }

    private List<LightPosition> inViewInBounds(int x, int y) {
        List<LightPosition> foundViews = new ArrayList<LightPosition>();

        for (LightPosition pos : insertedViews) {
            if (pos.containsTouchPosition(x, y)) {
                foundViews.add(pos);
            }
        }

        return foundViews;
    }

}
