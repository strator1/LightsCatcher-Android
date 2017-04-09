package com.hs_augsburg_example.lightscatcher.dataModels;

import android.graphics.Bitmap;
import android.media.Image;

import java.util.ArrayList;
import java.util.List;

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

    public Bitmap image;
    public int lightCount;
    public List<LightPosition> lightPositionList;
    public double gyroPosition;

    public String longitude;
    public String latitude;

    public static PhotoInformation shared = new PhotoInformation();

    private PhotoInformation() {
        this.image = null;
        this.lightCount = 0;
        this.lightPositionList = new ArrayList<LightPosition>();
        this.gyroPosition = 0;
        this.longitude = "";
        this.latitude = "";
    }


    public Bitmap getImage() {
        return image;
    }

    public void setImage(Bitmap image) {
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

    public List<LightPosition> getLightPositionList() {
        return lightPositionList;
    }

    public void setLightPositionList(List<LightPosition> lightPositionList) {
        this.lightPositionList = lightPositionList;
    }

}
