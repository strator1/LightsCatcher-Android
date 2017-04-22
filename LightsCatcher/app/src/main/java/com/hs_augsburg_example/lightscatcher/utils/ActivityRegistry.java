package com.hs_augsburg_example.lightscatcher.utils;

import android.app.Activity;
import java.util.ArrayList;
import java.util.List;

public class ActivityRegistry {

    private static List<Activity> activitiesList;

    // add activity to activitiesList
    public static void register(Activity activity) {
        if (activitiesList == null) {
            activitiesList = new ArrayList<Activity>();
        }
        activitiesList.add(activity);
    }

    // finish each activity
    public static void finishAll() {
        for (Activity activity : activitiesList) {
            activity.finish();
        }
    }
}
