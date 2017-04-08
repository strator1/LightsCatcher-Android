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

import java.util.ArrayList;
import java.util.List;

public class TagLightsActivity extends AppCompatActivity {

    private ImageView imageView;
    private RelativeLayout rl;

    private List<View> insertedViews = new ArrayList<View>();
    private List<View> pickedUpViews = new ArrayList<View>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tag_lights);

        imageView = (ImageView) findViewById(R.id.imageView);
        rl = (RelativeLayout) findViewById(R.id.tag_lights_rl);

//        imageView.setOnTouchListener(new TouchListener());
        rl.setOnTouchListener(new TouchListener());
    }

    private final class TouchListener implements View.OnTouchListener {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int[] location = new int[2];
            v.getLocationOnScreen(location);

            int x = (int) event.getRawX();
            int y = (int) event.getRawY() - location[1];

            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    // Check if location intersects with existing node
                    // -> Yes - pick up node
                    // -> No - create new node

                    pickedUpViews = inViewInBounds(x, y);
                    if (inViewInBounds(x, y).size() == 0) {
                        addNewView(x, y);
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    // Drop nodes
                    pickedUpViews = new ArrayList<View>();
                    break;
                case MotionEvent.ACTION_MOVE:
                    // Move nodes
                    for (View pickedView : pickedUpViews) {
                        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(60, 60);
                        params.leftMargin = x;
                        params.topMargin = y;

                        pickedView.setLayoutParams(params);
                    }

                    break;
                default:
                    return v.onTouchEvent(event);
            }

            return true;
        }

    }

    private void addNewView(int x, int y) {
        if (insertedViews.size() < 3) {
            View v = new View(getApplicationContext());
            v.setBackgroundColor(Color.BLACK);

            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(60, 60);
            params.leftMargin = x;
            params.topMargin = y;

            rl.addView(v, params);
            insertedViews.add(v);
            showLightPhaseAlertView(v);
        }
    }

    private void showLightPhaseAlertView(final View v) {
        AlertDialog alertDialog = new AlertDialog.Builder(TagLightsActivity.this).create();
        alertDialog.setTitle("Rot oder Grünphase?");
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Rot",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        v.setBackgroundColor(Color.RED);
                        dialog.dismiss();
                    }
                });
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Grün",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        v.setBackgroundColor(Color.GREEN);
                        dialog.dismiss();
                    }
                });
        alertDialog.show();
    }

    private List<View> inViewInBounds(int x, int y) {
        List<View> foundViews = new ArrayList<View>();

        for (View v : insertedViews) {
            if (inViewBounds2(v, x, y)) {
                System.out.println("you touched inside button");
                foundViews.add(v);
            } else {
                System.out.println("you touched outside button");
            }
        }

        return foundViews;
    }

    private boolean inViewBounds2 (View view, int x, int y) {
        Rect outRect = new Rect();
        view.getHitRect(outRect);

        return outRect.contains(x, y);
    }
}
