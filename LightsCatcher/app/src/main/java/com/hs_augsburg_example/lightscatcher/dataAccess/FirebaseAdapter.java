package com.hs_augsburg_example.lightscatcher.dataAccess;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.google.firebase.database.DatabaseReference;
import com.hs_augsburg_example.lightscatcher.R;

/**
 * Created by quirin on 14.04.17.
 */

public class FirebaseAdapter extends ArrayAdapter {

    final int rowResource;
    final DatabaseReference dataref;

    public FirebaseAdapter(@NonNull Context context, @LayoutRes int resource,DatabaseReference dataRef) {
        super(context, resource);
        this.dataref = dataRef;
        this.rowResource = resource;
    }
}
