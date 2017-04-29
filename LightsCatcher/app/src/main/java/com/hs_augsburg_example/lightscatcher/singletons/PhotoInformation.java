package com.hs_augsburg_example.lightscatcher.singletons;

import android.graphics.Bitmap;
import android.location.Location;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.hs_augsburg_example.lightscatcher.dataModels.Light;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
    public List<LightInformation> lightInformationList;
    public float gyroPosition;

    public Location location;

    public static PhotoInformation shared = new PhotoInformation();

    private DatabaseReference mDatabase;

    private PhotoInformation() {
        this.image = null;
        this.lightCount = 0;
        this.lightInformationList = new ArrayList<LightInformation>();
        this.gyroPosition = 0;
        this.location = null;

        this.mDatabase = FirebaseDatabase.getInstance().getReference();
    }


    public Bitmap getImage() {
        return image;
    }

    public void setImage(Bitmap image) {
        if (this.image != null) {
            this.image.recycle();
        }

        this.image = image;
    }

    public Light getLight() {
        Light light = new Light(this);
        return light;
    }

    public void createLight(String uid, Light light) {
        mDatabase.child("lights/v1_0").child(uid).setValue(light);
    }

    public int getLightCount() {
        return lightCount;
    }

    public void setLightCount(int lightCount) {
        this.lightCount = lightCount;
    }

    public float getGyroPosition() {
        return gyroPosition;
    }

    public void setGyroPosition(float gyroPosition) {
        this.gyroPosition = gyroPosition;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public void addToLightPosition(LightInformation pos) {
        this.lightInformationList.add(pos);
    }

    public List<LightInformation> getLightInformationList() {
        return lightInformationList;
    }

    public void setLightInformationList(List<LightInformation> lightInformationList) {
        this.lightInformationList = lightInformationList;
    }

    public void resetLightPositions() {
        this.lightInformationList = new ArrayList<LightInformation>();
    }

    public LightInformation getMostRelevantPosition() {
        for (LightInformation pos : lightInformationList) {
            if (pos.isMostRelevant()) {
                return pos;
            }
        }

        return null;
    }

}
