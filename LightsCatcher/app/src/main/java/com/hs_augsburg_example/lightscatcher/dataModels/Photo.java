package com.hs_augsburg_example.lightscatcher.dataModels;

import android.graphics.Bitmap;
import android.location.Location;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;
import com.hs_augsburg_example.lightscatcher.App;

import java.util.Date;
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
     * Timestamp in milliseconds (from {@link System#currentTimeMillis})
     */
    public long createdAt;

    /**
     * Timestamp - human readable
     */
    public String timeStamp;

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

    /**
     * A unique code for this app-version. Useful to identify the source of malformed data.
     */
    public String fingerPrint;

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
        p.timeStamp = new Date(p.createdAt).toGMTString();
        p.fingerPrint = App.getFingerprint();
        return new Builder(p);
    }

    /**
     * Ensures that this {@link Photo} has correct values. Otherwise throws a {@link IllegalStateException}
     */
    public void validate() {
        // check required fields.
        if (user == null)
            throw new IllegalStateException("Photo.user was null but is required.");
        if (id == null)
            throw new IllegalStateException("Photo.id was null but is required.");
        if (bitMap == null)
            throw new IllegalStateException("Photo.bitMap was null but is required.");
        if (createdAt == 0)
            throw new IllegalStateException("Photo.createdAt was not set but is required.");
        if (timeStamp == null)
            throw new IllegalStateException("Photo.timeStamp was null but is required.");
        if (credits == 0)
            throw new IllegalStateException("Photo.credits was 0. C'mon, give him some credits.");
        if (lightPositions.size() == 0)
            throw new IllegalStateException("Photo.lightPositions was empty but is should contain at least one element.");
        if (lightPositions.size() != lightsCount)
            throw new IllegalStateException("Photo.lightsCount did not match the size of the list.");
        if (fingerPrint == null) {
            throw new IllegalStateException("Photo.fingerPrint was null but is required.");
        }

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
            p.lightsCount++;
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
