package com.hs_augsburg_example.lightscatcher.utils;

import android.content.Context;

import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuthException;
import com.hs_augsburg_example.lightscatcher.R;

/**
 * Created by quirin on 03.05.17.
 */

/**
 * Translates common Exception-Messages of firebase to german
 */
public class ExceptionHelp {
    public static String germanMsg(Context ctx, Exception e) {
        if (e instanceof FirebaseAuthException) {
            switch (((FirebaseAuthException) e).getErrorCode()) {
                case "ERROR_EMAIL_ALREADY_IN_USE":
                    return "Diese Email-Adresse wird bereits verwendet.";
                case "ERROR_USER_DISABLED":
                    return "Dieser Account wurde deaktiviert.";
                case "ERROR_WRONG_PASSWORD":
                    return "Falsches Passwort.";
                case "ERROR_USER_NOT_FOUND":
                    return "Unbekannter Benutzername.";
                default:
                    return ((FirebaseAuthException) e).getErrorCode();
            }
        } else if (e instanceof FirebaseNetworkException) {
            return  ctx.getString(R.string.error_bad_network);
        } else if (e != null){
            return  e.getLocalizedMessage();
        }else{
            return  "";
        }
    }
}
