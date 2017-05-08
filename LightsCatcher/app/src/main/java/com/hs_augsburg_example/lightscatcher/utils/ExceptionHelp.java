package com.hs_augsburg_example.lightscatcher.utils;

import android.content.Context;

import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.database.DatabaseError;
import com.hs_augsburg_example.lightscatcher.R;

import static com.google.firebase.database.DatabaseError.*;

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
                    return ctx.getString(R.string.ERROR_EMAIL_ALREADY_IN_USE);
                case "ERROR_USER_DISABLED":
                    return ctx.getString(R.string.ERROR_USER_DISABLED);
                case "ERROR_WRONG_PASSWORD":
                    return ctx.getString(R.string.ERROR_WRONG_PASSWORD);
                case "ERROR_USER_NOT_FOUND":
                    return ctx.getString(R.string.ERROR_USER_NOT_FOUND);
                default:
                    return ((FirebaseAuthException) e).getErrorCode();
            }
        } else if (e instanceof FirebaseNetworkException) {
            return ctx.getString(R.string.error_bad_network);
        } else if (e != null) {
            return e.getLocalizedMessage();
        } else {
            return "";
        }
    }

    public static String germanMsg(Context ctx, DatabaseError error) {
        switch (error.getCode()) {
            case DISCONNECTED:
            case NETWORK_ERROR:
                return ctx.getString(R.string.error_bad_network);
            case DATA_STALE:
            case OPERATION_FAILED:
            case PERMISSION_DENIED:
            case EXPIRED_TOKEN:
            case INVALID_TOKEN:
            case MAX_RETRIES:
            case OVERRIDDEN_BY_SET:
            case UNAVAILABLE:
            case USER_CODE_EXCEPTION:
            case WRITE_CANCELED:
            case UNKNOWN_ERROR:
            default:
                return error.getMessage();
        }
    }
}
