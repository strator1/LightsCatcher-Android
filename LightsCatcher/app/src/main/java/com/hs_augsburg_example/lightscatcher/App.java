package com.hs_augsburg_example.lightscatcher;

import android.app.Application;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.hs_augsburg_example.lightscatcher.activities_minor.LoginActivity;
import com.hs_augsburg_example.lightscatcher.singletons.PersistenceManager;
import com.hs_augsburg_example.lightscatcher.singletons.UserInformation;
import com.hs_augsburg_example.lightscatcher.utils.ActivityRegistry;
import com.hs_augsburg_example.lightscatcher.utils.Log;

/**
 * Created by quirin on 03.05.17.
 */

public class App extends Application {

    private static App current;
    private static int vCode;

    public static String getFingerprint() {
        return Build.FINGERPRINT;
    }

    @Override
    public void onCreate() {
        if (Log.ENABLED) Log.d("App", "onCreate");


        //Has to be called before using Firebase
        PersistenceManager.init();
        UserInformation.init();

        current = this;
        super.onCreate();
    }

    public static int getVersionCode() {
        if (vCode == 0) {
            try {
                vCode = current.getPackageManager().getPackageInfo(current.getPackageName(), 0).versionCode;
            } catch (PackageManager.NameNotFoundException e) {
                Log.e("App", e);
            }
        }
        return vCode;
    }

    public static App getCurrent() {
        return current;
    }
}
