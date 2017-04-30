package com.hs_augsburg_example.lightscatcher.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by patrickvalenta on 14.04.17.
 */

public class UserPreference {

    final static String IS_FIRST_CAPTURE_KEY = "firstPic";
    final static String IS_FIRST_TAGGIN_KEY = "firstTag";
    final static String IS_USER_BANNED = "isUserBanned";

    public static boolean isFirstCapture(Context ctx) {
        SharedPreferences preferences = ctx.getSharedPreferences("MyPreferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        boolean firstCapture = !preferences.getBoolean(IS_FIRST_CAPTURE_KEY, false);

        if (firstCapture) {
            editor.putBoolean(IS_FIRST_CAPTURE_KEY, true).apply();
        }

        return firstCapture;
    }

    public static boolean isFirstTagging(Context ctx) {
        SharedPreferences preferences = ctx.getSharedPreferences("MyPreferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        boolean firstTag = !preferences.getBoolean(IS_FIRST_TAGGIN_KEY, false);

        if (firstTag) {
            editor.putBoolean(IS_FIRST_TAGGIN_KEY, true).apply();
        }

        return firstTag;
    }

    public static boolean isUserBanned(Context ctx) {
        SharedPreferences preferences = ctx.getSharedPreferences("MyPreferences", Context.MODE_PRIVATE);

        return preferences.getBoolean(IS_USER_BANNED, false);
    }

    public static boolean setUserBanned(Context ctx) {
        SharedPreferences preferences = ctx.getSharedPreferences("MyPreferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        editor.putBoolean(IS_USER_BANNED, true).apply();
        return true;
    }
}
