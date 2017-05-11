package com.hs_augsburg_example.lightscatcher.activities_major;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.PointF;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
import com.hs_augsburg_example.lightscatcher.R;
import com.hs_augsburg_example.lightscatcher.dataModels.Photo;
import com.hs_augsburg_example.lightscatcher.utils.Log;
import com.hs_augsburg_example.lightscatcher.views.Crosshair;


/**
 * Created by quirin on 09.05.17.
 */

public class SubmitDialog extends DialogFragment {
    public static final String TAG = "SubmitDialog";
    private SubmitDialogListener mListener;

    public interface SubmitDialogListener {
        void commit();

        void discard();
    }

    public static class Builder {
        private final SubmitDialog dialog;

        public Builder(Context ctx) {
            this.dialog = new SubmitDialog();
        }

        public SubmitDialog.Builder setPhoto(Photo photo) {
            dialog.mPhoto = photo;
            return this;
        }

        public SubmitDialog build() {
            if (dialog.mPhoto == null)
                throw new IllegalArgumentException("photo is required. Use SubmitDialog.Builder.setPhoto(...) to set a photo)");
            return this.dialog;
        }
    }

    private Photo mPhoto;
    private ViewGroup mContainer;

    public void setData(Photo photo) {
        this.mPhoto = photo;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (SubmitDialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement SubmitDialogListener");
        }
    }

    private final DialogInterface.OnClickListener positiveAction = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (mListener != null)
                mListener.commit();
        }
    };
    private final DialogInterface.OnClickListener negativeAction = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (mListener != null)
                mListener.discard();
        }
    };


    //based on http://stackoverflow.com/questions/4661459/dialog-problem-requestfeature-must-be-called-before-adding-content#4661526
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContainer = container;
        if (getShowsDialog()) {
            // one could return null here, or be nice and call super()
            return super.onCreateView(inflater, container, savedInstanceState);
        }
        return getLayout(inflater, container);
    }

    private View getLayout(LayoutInflater inflater, ViewGroup container) {
        View mLayout = inflater.inflate(R.layout.dialog_submit, container, false);
        return mLayout;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = getLayout(LayoutInflater.from(getContext()), mContainer);

        initPhotoView(view);

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
                .setTitle("Markieren")
                .setNegativeButton("Verwerfen", negativeAction)
                .setPositiveButton("Absenden", positiveAction)
                .setView(view);

        return builder.create();
    }

    void initPhotoView(View view) {
        Photo photo = mPhoto;
        if (photo != null) {
            final SubsamplingScaleImageView photoView = (SubsamplingScaleImageView) view.findViewById(R.id.submit_photoView);
            if (photoView == null) {
                Log.e(TAG, "missing layout-element: R.id.submit_photoView");
                return;
            }
            photoView.setImage(ImageSource.bitmap(photo.bitMap));

            // photoView eats absolute positions
            float x = (float) (photo.bitMap.getWidth() * photo.lightPos.x);
            float y = (float) (photo.bitMap.getHeight() * photo.lightPos.y);
            photoView.setScaleAndCenter(2, new PointF(x, y));

            final Crosshair cross = (Crosshair) view.findViewById(R.id.submit_crossHair);
            if (cross == null) {
                Log.e(TAG, "missing layout-element: R.id.submit_crossHair");
                return;
            }

            int color;
            switch (photo.lightPos.phase) {
                case RED:
                    color = 0x88FF0000;
                    break;
                case GREEN:
                    color = 0x8800ff00;
                    break;
                default:
                    color = 0x88000000;
            }
            cross.setCrossColor(color);
        }
    }
}
