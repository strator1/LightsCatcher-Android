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

    public String imageUrl;
    @Exclude
    public Bitmap bitMap;
    public String gyroPosition;
    public String longitude;
    public String latitude;
    public long createdAt;
    public LightPosition lightPos;

    public Photo() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public static Builder buildNew() {
        Photo p = new Photo();
        p.id = UUID.randomUUID().toString().toUpperCase();
        return new Builder(p);
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

        public Builder setLightPos(LightPosition lightPosition) {
            p.lightPos = lightPosition;
            return this;
        }

        public Builder setCreatedAt(Long pCreatedAt) {
            p.createdAt = pCreatedAt;
            return this;
        }

        public Photo commit(){
            // check required fields.
            if (p.id == null) Log.e("","Photo.id was not set!");
            if (p.createdAt == 0) p.createdAt = System.currentTimeMillis();
            return p;
        }
    }

}
