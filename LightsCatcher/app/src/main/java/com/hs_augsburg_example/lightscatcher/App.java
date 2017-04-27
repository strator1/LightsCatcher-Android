package com.hs_augsburg_example.lightscatcher;

import android.app.Application;
import android.content.Context;

/**
 * Created by quirin on 26.04.17.
 */

public class App extends Application{

    private static Context ctx;

    @Override
    public void onCreate() {
        super.onCreate();
        App.ctx = getApplicationContext();
    }

    public static Context getContext(){
        return ctx;
    }

}
