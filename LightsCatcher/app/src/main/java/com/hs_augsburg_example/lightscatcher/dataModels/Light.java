package com.hs_augsburg_example.lightscatcher.dataModels;

import com.google.firebase.database.IgnoreExtraProperties;
import com.hs_augsburg_example.lightscatcher.singletons.LightInformation;
import com.hs_augsburg_example.lightscatcher.singletons.PhotoInformation;
import com.hs_augsburg_example.lightscatcher.singletons.UserInformation;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by patrickvalenta on 15.04.17.
 * replaced by class {@link Record}
 */

@Deprecated
@IgnoreExtraProperties
public class Light {

    public String user;
    public String imageUrl;
    public String gyroPosition;
    public int lightsCount;
    public List<LightPosition> lightPositions;
    public String longitude;
    public String latitude;

    public String createdAt;

    public Light() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public Light(PhotoInformation information) {
        this.user = UserInformation.shared.getUserId();
        this.gyroPosition = Float.toString(information.getGyroPosition());
        this.lightsCount = information.getLightInformationList().size();

        this.longitude = information.getLocation() != null ? Double.toString(information.getLocation().getLongitude()) : "0";
        this.latitude = information.getLocation() != null ? Double.toString(information.getLocation().getLatitude()) : "0";

        this.lightPositions = new ArrayList<LightPosition>();

        this.imageUrl = "";
        this.createdAt = Long.toString(System.currentTimeMillis());

        for(LightInformation l : information.getLightInformationList()) {
            lightPositions.add(new LightPosition(l.relPos[0], l.relPos[1], l.getPhase(), l.isMostRelevant()));
        }
    }

}
