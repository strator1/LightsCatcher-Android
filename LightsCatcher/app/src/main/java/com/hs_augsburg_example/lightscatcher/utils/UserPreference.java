package com.hs_augsburg_example.lightscatcher.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by patrickvalenta on 14.04.17.
 */

public class UserPreference {

    public static final String MY_PREFERENCES = "MyPreferences";

    public final static String IS_FIRST_CAPTURE_KEY = "firstPic";
    public final static String IS_FIRST_TAGGIN_KEY = "firstTag";
    public final static String IS_USER_BANNED = "isUserBanned";
    public final static String MAXIMUM_SNAPSHOT_ALERT = "MAXIMUM_SNAPSHOT_ALERT";

    public static boolean isFirstCapture(Context ctx) {
        SharedPreferences preferences = ctx.getSharedPreferences(MY_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        boolean firstCapture = !preferences.getBoolean(IS_FIRST_CAPTURE_KEY, false);

        if (firstCapture) {
            editor.putBoolean(IS_FIRST_CAPTURE_KEY, true).apply();
        }

        return firstCapture;
    }

    public static boolean isFirstTagging(Context ctx) {
        SharedPreferences preferences = ctx.getSharedPreferences(MY_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        boolean firstTag = !preferences.getBoolean(IS_FIRST_TAGGIN_KEY, false);

        if (firstTag) {
            editor.putBoolean(IS_FIRST_TAGGIN_KEY, true).apply();
        }

        return firstTag;
    }

    public static boolean isUserBanned(Context ctx) {
        SharedPreferences preferences = ctx.getSharedPreferences(MY_PREFERENCES, Context.MODE_PRIVATE);

        return preferences.getBoolean(IS_USER_BANNED, false);
    }

    public static boolean setUserBanned(Context ctx) {
        SharedPreferences preferences = ctx.getSharedPreferences(MY_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        editor.putBoolean(IS_USER_BANNED, true).apply();
        return true;
    }

    public static boolean shouldShowDialog(Context ctx, String key) {
        SharedPreferences preferences = ctx.getSharedPreferences(MY_PREFERENCES, Context.MODE_PRIVATE);
        return preferences.getBoolean(key, true);
    }

    public static void neverShowAgain(Context ctx, String key, boolean neverShowAgain) {
        SharedPreferences preferences = ctx.getSharedPreferences(MY_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(key, !neverShowAgain).apply();
    }
}
