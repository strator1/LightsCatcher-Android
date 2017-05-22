package com.github.amlcurran.showcaseview;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;

import com.github.amlcurran.showcaseview.ShowcaseDrawer;

/**
 * Created by Quirin on 22.05.2017.
 */

public class StandardShowcaseDrawer2 extends StandardShowcaseDrawer {
    public StandardShowcaseDrawer2(Resources resources, Resources.Theme theme) {
        super(resources, theme);
    }

    @Override
    public void drawShowcase(Bitmap buffer, float x, float y, float scaleMultiplier) {
        Canvas bufferCanvas = new Canvas(buffer);
        bufferCanvas.drawCircle(x, y, showcaseRadius * scaleMultiplier, eraserPaint);
        int halfW = (int) (getShowcaseWidth() * scaleMultiplier / 2);
        int halfH = (int) (getShowcaseHeight() * scaleMultiplier / 2);
        int left = (int) (x - halfW);
        int top = (int) (y - halfH);
        showcaseDrawable.setBounds(left, top,
                left + (int) (getShowcaseWidth() * scaleMultiplier),
                top + (int) (getShowcaseHeight() * scaleMultiplier));
        showcaseDrawable.draw(bufferCanvas);
    }
}
