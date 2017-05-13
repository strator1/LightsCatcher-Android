package com.hs_augsburg_example.lightscatcher;

import android.app.Application;

import com.hs_augsburg_example.lightscatcher.singletons.PersistenceManager;
import com.hs_augsburg_example.lightscatcher.singletons.UserInformation;

/**
 * Created by quirin on 03.05.17.
 */

public class App extends Application {

    @Override
    public void onCreate() {

        //Has to be called before using Firebase
        PersistenceManager.init();
        UserInformation.init();

        super.onCreate();
    }
}
