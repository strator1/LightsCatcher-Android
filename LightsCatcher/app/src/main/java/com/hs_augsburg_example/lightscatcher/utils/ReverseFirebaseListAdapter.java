package com.hs_augsburg_example.lightscatcher.utils;

import android.app.Activity;

import com.firebase.ui.database.FirebaseListAdapter;
import com.google.firebase.database.Query;

import java.util.HashMap;

/**
 * Created by quirin on 22.04.17
 */

/**
 * The same as @see com.firebase.ui.database.FirebaseListAdapter but provides items in reverse order.
 */
public abstract class ReverseFirebaseListAdapter<T> extends FirebaseListAdapter<T>{

    /**
     * @param activity    The activity containing the ListView
     * @param modelClass  Firebase will marshall the data at a location into
     *                    an instance of a class that you provide
     * @param modelLayout This is the layout used to represent a single list item.
     *                    You will be responsible for populating an instance of the corresponding
     *                    view with the data from an instance of modelClass.
     * @param ref         The Firebase location to watch for data changes. Can also be a slice of a location,
     *                    using some combination of {@code limit()}, {@code startAt()}, and {@code endAt()}.
     */
    public ReverseFirebaseListAdapter(Activity activity, Class<T> modelClass, int modelLayout, Query ref) {
        super(activity, modelClass, modelLayout, ref);
    }

    @Override
    public T getItem(int pos) {
        /* most simple workaraoud, other implementations are also posible.
         * see http://stackoverflow.com/questions/37396246/android-firebaselistadapter-in-reverse-order
         */
        return super.getItem(getCount() - 1 - pos);
    }
}
