package com.hs_augsburg_example.lightscatcher.utils;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.database.Query;

/**
 * Created by quirin on 22.04.17
 */

/**
 * A modification of  @see com.firebase.ui.database.FirebaseListAdapter.
 * The only difference ist that the items are provided in reverse order.
 */
public abstract class ReverseFirebaseRecyclerAdapter<T,TH extends ViewHolder> extends FirebaseRecyclerAdapter<T, TH > {


    /**
     * @param modelClass      Firebase will marshall the data at a location into
     *                        an instance of a class that you provide
     * @param modelLayout     This is the layout used to represent a single item in the list.
     *                        You will be responsible for populating an instance of the corresponding
     *                        view with the data from an instance of modelClass.
     * @param viewHolderClass The class that hold references to all sub-views in an instance modelLayout.
     * @param ref             The Firebase location to watch for data changes. Can also be a slice of a location,
     *                        using some combination of {@code limit()}, {@code startAt()}, and {@code endAt()}.
     */
    public ReverseFirebaseRecyclerAdapter(Class<T> modelClass, int modelLayout, Class<TH> viewHolderClass, Query ref) {
        super(modelClass, modelLayout, viewHolderClass, ref);
    }

    @Override
    public T getItem(int pos) {
        // most simple workaround
        // for other solutions see http://stackoverflow.com/questions/37396246/android-firebaselistadapter-in-reverse-order
        return super.getItem(super.getItemCount() - 1 - pos);
    }

}
