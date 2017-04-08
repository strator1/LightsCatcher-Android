package com.hs_augsburg_example.lightscatcher.camera;

import android.media.Image;

/**
 * Created by patrickvalenta on 08.04.17.
 */

public class PhotoInformation {

    public enum LightPhase {
        RED(0), GREEN(1);
        private final int value;

        private LightPhase(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    public Image image;
    public int lightCount;
    public double gyroPosition;

    public String longitude;
    public String latitude;


    public Image getImage() {
        return image;
    }

    public void setImage(Image image) {
        this.image = image;
    }

    public int getLightCount() {
        return lightCount;
    }

    public void setLightCount(int lightCount) {
        this.lightCount = lightCount;
    }

    public double getGyroPosition() {
        return gyroPosition;
    }

    public void setGyroPosition(double gyroPosition) {
        this.gyroPosition = gyroPosition;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

}
