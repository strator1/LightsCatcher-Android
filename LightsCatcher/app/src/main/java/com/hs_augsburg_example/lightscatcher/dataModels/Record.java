package com.hs_augsburg_example.lightscatcher.dataModels;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;
import com.hs_augsburg_example.lightscatcher.BuildConfig;

import java.io.Serializable;
import java.util.UUID;
import java.util.function.BinaryOperator;

/**
 * Created by quirin on 26.04.17.
 */

@IgnoreExtraProperties
public class Record implements Serializable {

    public static Record latestRecord;
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

    public static Builder buildNew() {
        Record r = new Record();
        r.appVersion = BuildConfig.VERSION_CODE;
        r.id = UUID.randomUUID().toString().toUpperCase();
        return new Builder(r);
    }

    public Record(String userID, Photo redPhoto, Photo greenPhoto, int points) {
        this.userID = userID;
        this.redPhoto = redPhoto;
        this.greenPhoto = greenPhoto;
        this.points = points;
        this.appVersion = BuildConfig.VERSION_CODE;
        this.id = UUID.randomUUID().toString().toUpperCase();
    }

    public static class Builder {

        private final Record r;

        public Builder(Record r) {
            this.r = r;
        }

        public Builder setUser(String uid) {
            r.userID = uid;
            return this;
        }

        public Builder setRedPhoto(Photo red) {
            r.redPhoto = red;
            return this;
        }

        public Builder setGreenPhoto(Photo greenPhoto) {
            r.greenPhoto = greenPhoto;
            return this;
        }

        public Builder setPoints(int points) {
            r.points = points;
            return this;
        }

        public Record commit() {
            return r;
        }
    }
}
