package com.hs_augsburg_example.lightscatcher.views;

/**
 * Created by quirin on 08.05.17.
 * <p>
 * based on http://www.javarticles.com/2015/09/android-icon-badge-example-using-layer-list-drawable.html
 */


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.support.annotation.IntRange;
import android.support.annotation.Nullable;

import com.hs_augsburg_example.lightscatcher.R;

public class BadgeDrawable extends Drawable {

    private Paint mBadgePaint;
    private String mCount = "";
    private Paint mTextPaint;
    private float mTextSize;
    private Rect mTxtRect = new Rect();
    private boolean mWillDraw = false;

    public BadgeDrawable(Context paramContext) {
        this.mBadgePaint = new Paint();
        this.mBadgePaint.setColor(0xff8888cc);
        this.mBadgePaint.setAntiAlias(true);
        this.mBadgePaint.setStyle(Paint.Style.FILL);
        this.mTextSize = paramContext.getResources().getDimension(R.dimen.badgeSize);
        this.mTextPaint = new Paint();
        this.mTextPaint.setColor(-1);
        this.mTextPaint.setTypeface(Typeface.DEFAULT);
        this.mTextPaint.setTextSize(this.mTextSize);
        this.mTextPaint.setAntiAlias(true);
        this.mTextPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void draw(Canvas paramCanvas) {
        if (!this.mWillDraw) {
            return;
        }
        Rect localRect = getBounds();
        float width = localRect.width();
        float height = localRect.height();
        float circleRadius = Math.min(width, height) / 4.0f + 2.5F;
        float circleX = circleRadius;
        float circleY = height - circleRadius;
        paramCanvas.drawCircle(circleX, circleY, circleRadius, this.mBadgePaint);
        this.mTextPaint.getTextBounds(this.mCount, 0, this.mCount.length(), this.mTxtRect);
        float textY = circleY + (this.mTxtRect.bottom - this.mTxtRect.top) / 2.0F;
        float textX = circleX;
        if (Integer.parseInt(this.mCount) >= 10) {
            textX = textX - 1.0F;
            textY = textY - 1.0F;
        }
        paramCanvas.drawText(this.mCount, textX, textY, this.mTextPaint);
    }

    @Override
    public void setAlpha(@IntRange(from = 0, to = 255) int alpha) {

    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {

    }

    @Override
    public int getOpacity() {
        return PixelFormat.UNKNOWN;
    }

    public void setCount(int paramInt) {
        this.mCount = Integer.toString(paramInt);
        this.mWillDraw = paramInt > 0;
        invalidateSelf();
    }

}