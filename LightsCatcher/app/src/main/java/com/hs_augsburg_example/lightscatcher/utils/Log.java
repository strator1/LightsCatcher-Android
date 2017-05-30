package com.hs_augsburg_example.lightscatcher.utils;

import java.text.MessageFormat;

/**
 * Created by quirin on 05.05.17.
 */

/**
 * Wrapper for android.util.Log
 */
public class Log {
    // flag to disable logging application-wide
    public static final boolean ENABLED = false;

    public static void d(String tag, String format) {
        android.util.Log.d(tag, format);
    }

    public static void d(String tag, String format, Object... params) {
        android.util.Log.d(tag, MessageFormat.format(format, params));
    }

    public static void e(String tag, Throwable ex) {
        android.util.Log.e(tag, "ERROR:", ex);
    }

    public static void e(String tag, String s) {
        android.util.Log.e(tag, s);
    }

    public static void e(String tag, Exception e) {
        android.util.Log.e(tag, e.getMessage(), e);
    }

    public static void e(String tag, String s, Exception e) {
        android.util.Log.e(tag, s, e);
    }
}
