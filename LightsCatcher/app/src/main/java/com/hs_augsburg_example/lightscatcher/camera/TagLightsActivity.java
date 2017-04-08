package com.hs_augsburg_example.lightscatcher.camera;

import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Rect;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.hs_augsburg_example.lightscatcher.R;
import com.hs_augsburg_example.lightscatcher.dataModels.LightPosition;
import com.hs_augsburg_example.lightscatcher.dataModels.PhotoInformation;

import java.util.ArrayList;
import java.util.List;

import static com.hs_augsburg_example.lightscatcher.dataModels.PhotoInformation.LightPhase.GREEN;
import static com.hs_augsburg_example.lightscatcher.dataModels.PhotoInformation.LightPhase.RED;

public class TagLightsActivity extends AppCompatActivity implements View.OnTouchListener {

    private ImageView imageView;
    private RelativeLayout rl;

    private List<LightPosition> insertedViews = new ArrayList<LightPosition>();
    private List<LightPosition> pickedUpViews = new ArrayList<LightPosition>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tag_lights);

        imageView = (ImageView) findViewById(R.id.imageView);
        rl = (RelativeLayout) findViewById(R.id.tag_lights_rl);

//        imageView.setOnTouchListener(new TouchListener());
        imageView.setOnTouchListener(this);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int[] location = new int[2];
        v.getLocationOnScreen(location);

        int x = (int) event.getRawX();
        int y = (int) event.getRawY() - location[1];

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                onActionDownTouch(x, y);
                break;
            case MotionEvent.ACTION_UP:
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
        // Check if location intersects with existing node
        // -> Yes - pick up node
        // -> No - create new node

        pickedUpViews = inViewInBounds(x, y);
        if (pickedUpViews.size() == 0) {
            addNewView(x, y);
        }
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

    private void addNewView(int x, int y) {
        if (insertedViews.size() < 3) {
            View v = new View(getApplicationContext());
            v.setBackgroundColor(Color.BLACK);

            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LightPosition.WIDTH, LightPosition.HEIGHT);

            LightPosition pos = new LightPosition(v);
            pos.setPos(x, y);
            params.leftMargin = pos.getX();
            params.topMargin = pos.getY();

            rl.addView(v, params);
            insertedViews.add(pos);
            showLightPhaseAlertView(pos);
        }
    }

    private void showLightPhaseAlertView(final LightPosition pos) {
        AlertDialog alertDialog = new AlertDialog.Builder(TagLightsActivity.this).create();
        alertDialog.setTitle("Rot oder Grünphase?");
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Rot",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        pos.setPhase(RED);
                        dialog.dismiss();
                    }
                });
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Grün",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        pos.setPhase(GREEN);
                        dialog.dismiss();
                    }
                });
        alertDialog.show();
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
