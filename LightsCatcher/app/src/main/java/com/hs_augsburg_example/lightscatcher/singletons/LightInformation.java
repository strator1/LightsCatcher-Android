package com.hs_augsburg_example.lightscatcher.singletons;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.hs_augsburg_example.lightscatcher.singletons.PhotoInformation;

/**
 * Created by patrickvalenta on 08.04.17.
 */

public class LightInformation extends AppCompatActivity {

    public static final int WIDTH = 40;
    public static final int HEIGHT = 40;

    public Context ctx;
    public View view;
    public PhotoInformation.LightPhase phase;
    public boolean isMostRelevant;
    public int x;
    public int y;

    public double[] relPos = new double[2];

    public LightInformation(View v, Context ctx) {
        this.ctx = ctx;
        this.view = v;
        this.isMostRelevant = false;
    }

    public void setPos(int x, int y) {
        this.x = x - getWidth()/2;
        this.y = y - getHeight()/2;

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(getWidth(), getHeight());
        params.leftMargin = this.x;
        params.topMargin = this.y;

        this.getView().setLayoutParams(params);
    }

    public boolean containsTouchPosition(int x, int y) {
        Rect outRect = new Rect();
        this.getView().getHitRect(outRect);

        return outRect.contains(x, y);
    }

    public void convertToAbsoluteXY(ImageView iv, Bitmap img) {
        float percentX = (float) (this.x + (this.getWidth() /2)) / (float) iv.getWidth();
        float percentY = (float) (this.y + (this.getHeight() / 2)) / (float) iv.getHeight();

        this.x = (int) (img.getWidth() * percentX);
        this.y = (int) (img.getHeight() * percentY);

        this.relPos[0] = (img.getWidth() * percentX) / img.getWidth();
        this.relPos[1] = (img.getHeight() * percentY) / img.getHeight();
    }

    public int getWidth() {
        return (int) (WIDTH * ctx.getResources().getDisplayMetrics().density);
    }

    public int getHeight() {
        return (int) (HEIGHT * ctx.getResources().getDisplayMetrics().density);
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
        int c = this.phase == PhotoInformation.LightPhase.RED ? Color.argb(0x60,0xFF,0x00,0x00) : Color.argb(0x60,0x00,0xFF,0x00);

        this.getView().setBackgroundColor(c);

        GradientDrawable gd = new GradientDrawable();
        gd.setCornerRadius(5);
        gd.setColor(c);

        if (isMostRelevant) {
            gd.setStroke(5, Color.YELLOW);
        } else {
            gd.setStroke(5, c);
        }

        this.getView().setBackgroundDrawable(gd);

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

    public boolean isMostRelevant() {
        return isMostRelevant;
    }

    public void setMostRelevant(boolean mostRelevant) {
        isMostRelevant = mostRelevant;
    }
}
