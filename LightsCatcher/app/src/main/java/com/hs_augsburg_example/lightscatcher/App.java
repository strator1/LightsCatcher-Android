package com.hs_augsburg_example.lightscatcher;

import android.app.Application;
import android.os.Build;

import com.hs_augsburg_example.lightscatcher.singletons.PersistenceManager;
import com.hs_augsburg_example.lightscatcher.singletons.UserInformation;
import com.hs_augsburg_example.lightscatcher.utils.Log;

/**
 * Created by quirin on 03.05.17.
 */

public class App extends Application {

    public static String getFingerprint(){
        return Build.FINGERPRINT;
    }
    @Override
    public void onCreate() {
        if (Log.ENABLED) Log.d("App", "onCreate");
        PersistenceManager.init();
        UserInformation.init();

        super.onCreate();
    }
}
