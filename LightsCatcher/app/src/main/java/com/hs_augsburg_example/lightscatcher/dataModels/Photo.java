package com.hs_augsburg_example.lightscatcher.dataModels;

import android.graphics.Bitmap;
import android.location.Location;
import android.util.Log;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;
import com.hs_augsburg_example.lightscatcher.singletons.LightInformation;
import com.hs_augsburg_example.lightscatcher.singletons.PhotoInformation;
import com.hs_augsburg_example.lightscatcher.singletons.UserInformation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by quirin on 26.04.17.
 */

@IgnoreExtraProperties
public class Photo {

    @Exclude
    public String id;

    @Exclude
    public Bitmap bitMap;


    /**
     * The user-id who has taken this photo
     */
    public String user;

    /**
     * Timestamp in miliseconds (from {@link System#currentTimeMillis})
     */
    public long createdAt;

    /**
     * position(s) of the traffic-light(s) relative to image-dimensions
     */
    public LightPosition.List lightPositions = new LightPosition.List();

    public int lightsCount;

    /**
     * Credits for this photo.
     */
    public int credits;

    /**
     * direct web-url to the image-data
     */
    public String imageUrl;

    public String gyroPosition;
    public String longitude;
    public String latitude;

    public Photo() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public static Builder buildNew() {
        Photo p = new Photo();
        p.id = UUID.randomUUID().toString().toUpperCase();
        p.createdAt = System.currentTimeMillis();
        return new Builder(p);
    }

    /**
     * Ensures that this {@link Photo} has correct values. Otherwise throws a {@link IllegalArgumentException}
     */
    public void validate() {
        // check required fields.
        if (user == null)
            throw new IllegalArgumentException("Photo.user was null but is required.");
        if (id == null) throw new IllegalArgumentException("Photo.id was null but is required.");
        if (bitMap == null)
            throw new IllegalArgumentException("Photo.bitMap was null but is required.");
        if (createdAt == 0)
            throw new IllegalArgumentException("Photo.createdAt was not set but is required.");
        if (credits == 0)
            throw new IllegalArgumentException("Photo.credits was 0. C'mon, give him some credits.");
        if (lightPositions.size() == 0)
            throw new IllegalArgumentException("Photo.lightPositions was empty but is should contain at least one element.");
        lightsCount = lightPositions.size();

        // check each position
        for (LightPosition p : lightPositions) {
            p.validate();
        }
    }

    public static class Builder {

        private final Photo p;

        public Builder(Photo p) {
            this.p = p;
        }

        public Builder setBitmap(Bitmap bmp) {
            p.bitMap = bmp;
            return this;
        }

        public Builder setLocation(Location l) {
            if (l == null) return this;
            p.longitude = Double.toString(l.getLongitude());
            p.latitude = Double.toString(l.getLatitude());

            return this;
        }

        public Builder setGyro(float pitch) {
            p.gyroPosition = Float.toString(pitch);
            return this;
        }

        public Builder addLightPos(LightPosition lightPosition) {
            p.lightPositions.add(lightPosition);
            return this;
        }

        public Photo commit() {
            p.validate();
            return p;
        }

        public Builder setCredits(int credits) {
            p.credits = credits;
            return this;
        }

        public Builder setUser(String uid) {
            p.user = uid;
            return this;
        }
    }

}
