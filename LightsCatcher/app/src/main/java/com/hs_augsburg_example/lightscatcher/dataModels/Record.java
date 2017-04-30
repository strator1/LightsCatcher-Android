package com.hs_augsburg_example.lightscatcher.dataModels;

import android.util.Log;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;
import com.hs_augsburg_example.lightscatcher.BuildConfig;

import java.io.Serializable;
import java.util.UUID;

/**
 * Created by quirin on 26.04.17.
 */

/**
 * The data-structure representing a snapshot of a traffic-light with ideally both red and green phases. Though the user may capture only one phase.
 */
@IgnoreExtraProperties
public class Record implements Serializable {

    public static Record.Builder latestRecord;
    @Exclude
    public String id;
    public String userID;
    public Photo redPhoto;
    public Photo greenPhoto;
    public int points;
    public int appVersion;

    public Record() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public static Builder buildNew(String id) {
        Record r = new Record();
        r.appVersion = BuildConfig.VERSION_CODE;
        r.id = id;
        return new Builder(r);
    }
    public static Builder buildNew(){
        return buildNew(UUID.randomUUID().toString().toUpperCase());
    }

//    public Record(String userID, Photo redPhoto, Photo greenPhoto, int points) {
//        this.userID = userID;
//        this.redPhoto = redPhoto;
//        this.greenPhoto = greenPhoto;
//        this.points = points;
//        this.appVersion = BuildConfig.VERSION_CODE;
//        this.id = UUID.randomUUID().toString().toUpperCase();
//    }

    public static class Builder {

        public final Record record;

        public Builder(Record r) {
            this.record = r;
        }

        public Builder setUser(String uid) {
            record.userID = uid;
            return this;
        }

        public Builder setPhoto(LightPhase phase, Photo photo) {
            switch (phase) {
                case RED:
                    record.redPhoto = photo;
                case GREEN:
                    record.greenPhoto = photo;
            }
            return this;
        }

        public Builder setRedPhoto(Photo red) {
            record.redPhoto = red;
            return this;
        }

        public Builder setGreenPhoto(Photo greenPhoto) {
            record.greenPhoto = greenPhoto;
            return this;
        }

        public Builder setPoints(int points) {
            record.points = points;
            return this;
        }

        /**
         * Gives one point for each photo.
         */
        public Builder giveAppropriatePoints() {
            int points = 0;
            if (record.redPhoto != null)
                points++;
            if (record.greenPhoto != null)
                points++;

            record.points = points;
            return this;
        }

        /**
         * Checks for valid data and throws IllegalArgumentException when invalid.
         */
        public Record commit() throws IllegalArgumentException {
            if (record.id == null)
                throw new IllegalArgumentException("Record.id was null but is required!");
            if (record.greenPhoto == null && record.redPhoto == null)
                throw new IllegalArgumentException("Record.greenPhoto and Record.redPhoto both were null. At least one is required!");
            if (record.userID == null)
                throw new IllegalArgumentException("Record.userID was null but is required!");
            if (record.points < 0) throw new IllegalArgumentException("Record.points was < 0!");
            if (record.points == 0)
                Log.w("APP", "Record.points was 0, Are you sure to give no credits?");
            return record;
        }
    }
}
