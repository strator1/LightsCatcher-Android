package com.hs_augsburg_example.lightscatcher.camera;

import android.view.View;

/**
 * Created by patrickvalenta on 08.04.17.
 */

class LightPosition {
    View view;
    PhotoInformation.LightPhase phase;
    Double x;
    Double y;


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
    }

    public Double getX() {
        return x;
    }

    public void setX(Double x) {
        this.x = x;
    }

    public Double getY() {
        return y;
    }

    public void setY(Double y) {
        this.y = y;
    }
}
