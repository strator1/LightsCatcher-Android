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
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.hs_augsburg_example.lightscatcher.R;

import static com.hs_augsburg_example.lightscatcher.utils.MiscUtils.dp;

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
        View hor2 = new View(this.getContext());
        hor2.setBackgroundColor(0xffbbbb00);
        View ver2 = new View(this.getContext());
        ver2.setBackgroundColor(0xffbbbb00);
        float d = ctx.getResources().getDisplayMetrics().density;

        this.addView(hor, new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(5, d), Gravity.CENTER));
        this.addView(ver, new FrameLayout.LayoutParams(dp(5, d), LayoutParams.MATCH_PARENT, Gravity.CENTER));
        this.addView(hor2, new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(1, d), Gravity.CENTER));
        this.addView(ver2, new FrameLayout.LayoutParams(dp(1, d), LayoutParams.MATCH_PARENT, Gravity.CENTER));

        FrameLayout.LayoutParams layoutParams2 = (LayoutParams) hor2.getLayoutParams();
        layoutParams2.setMargins(dp(2, d), dp(2, d), dp(2, d), dp(2, d));
        hor2.setLayoutParams(layoutParams2);

        FrameLayout.LayoutParams layoutParams1 = (LayoutParams) ver2.getLayoutParams();
        layoutParams1.setMargins(dp(2, d), dp(2, d), dp(2, d), dp(2, d));
        ver2.setLayoutParams(layoutParams1);
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
