package com.hs_augsburg_example.lightscatcher.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import com.hs_augsburg_example.lightscatcher.R;

/**
 * Created by quirin on 27.04.17.
 */

public class Crosshair extends FrameLayout {
    private int crossColor;
    private View hor, ver;

    public Crosshair(Context context) {
        super(context);
        init(context);
    }

    public Crosshair(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
        applyAttributes(context, attrs);
    }

    public Crosshair(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
        applyAttributes(context, attrs);
    }

    private void init(Context ctx) {
        hor = new View(this.getContext());
        ver = new View(this.getContext());
        float d = ctx.getResources().getDisplayMetrics().density;

        this.addView(hor,new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, (int)(5*d), Gravity.CENTER));
        this.addView(ver,new FrameLayout.LayoutParams((int)(5*d), LayoutParams.MATCH_PARENT, Gravity.CENTER));
    }

    private void applyAttributes(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.Crosshair);
        int color = a.getColor(R.styleable.Crosshair_crossColor, 0xFF000000);
        setCrossColor(color);
    }

    public int getCrossColor() {
        return this.crossColor;
    }

    public void setCrossColor(int value) {
        this.crossColor = value;
        hor.setBackgroundColor(value);
        ver.setBackgroundColor(value);
        this.invalidate();
    }
}
