package com.hs_augsburg_example.lightscatcher.dataModels;

import android.graphics.Color;
import android.graphics.Rect;
import android.view.View;
import android.widget.RelativeLayout;

/**
 * Created by patrickvalenta on 08.04.17.
 */

public class LightPosition {

    public static final int WIDTH = 60;
    public static final int HEIGHT = 60;

    public View view;
    public PhotoInformation.LightPhase phase;
    public int x;
    public int y;

    public LightPosition(View v) {
        this.view = v;
    }

    public void setPos(int x, int y) {
        this.x = x - WIDTH/2;
        this.y = y - HEIGHT/2;

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(WIDTH, HEIGHT);
        params.leftMargin = this.x;
        params.topMargin = this.y;

        this.getView().setLayoutParams(params);
    }

    public boolean containsTouchPosition(int x, int y) {
        Rect outRect = new Rect();
        this.getView().getHitRect(outRect);

        return outRect.contains(x, y);
    }

    public View getView() {
        return view;
    }

    public void setView(View view) {
        this.view = view;
    }

    public PhotoInformation.LightPhase getPhase() {
        return phase;
    }

    public void setPhase(PhotoInformation.LightPhase phase) {
        this.phase = phase;

        this.getView().setBackgroundColor(this.phase == PhotoInformation.LightPhase.RED ? Color.RED : Color.GREEN);
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }
}
