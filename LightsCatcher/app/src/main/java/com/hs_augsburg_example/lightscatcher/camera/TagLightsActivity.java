package com.hs_augsburg_example.lightscatcher.camera;

import android.graphics.Color;
import android.graphics.Rect;
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
//            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) v.getLayoutParams();

            int[] location = new int[2];
            v.getLocationOnScreen(location);

            int x = (int) event.getRawX();
            int y = (int) event.getRawY() - location[1];

            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    // Check if location intersects with existing node
                    // -> Yes - pick up node
                    // -> No - create new node
                    addNewView(x, y);
                    break;
                case MotionEvent.ACTION_UP:
                    // Drop nodes
                    break;
                case MotionEvent.ACTION_MOVE:
                    // Move nodes
                    break;
            }
            return true;
        }

    }

    private void addNewView(int x, int y) {
        View v = new View(getApplicationContext());
        v.setBackgroundColor(Color.BLACK);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(30, 30);
        params.leftMargin = x;
        params.topMargin = y;

        rl.addView(v, params);
        insertedViews.add(v);
    }

    private boolean inViewInBounds(View view, int x, int y) {
        view.getDrawingRect(outRect);
        view.getLocationOnScreen(location);
        outRect.offset(location[0], location[1]);
        return outRect.contains(x, y);
    }
}
