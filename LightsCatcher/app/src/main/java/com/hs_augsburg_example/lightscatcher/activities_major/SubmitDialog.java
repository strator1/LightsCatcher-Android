package com.hs_augsburg_example.lightscatcher.activities_major;

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
import android.widget.TextView;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
import com.hs_augsburg_example.lightscatcher.R;
import com.hs_augsburg_example.lightscatcher.dataModels.LightPosition;
import com.hs_augsburg_example.lightscatcher.dataModels.Photo;
import com.hs_augsburg_example.lightscatcher.singletons.PersistenceManager;
import com.hs_augsburg_example.lightscatcher.utils.Log;
import com.hs_augsburg_example.lightscatcher.utils.TaskMonitor;
import com.hs_augsburg_example.lightscatcher.views.Crosshair;

import java.text.DecimalFormat;

import static com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView.PAN_LIMIT_CENTER;


/**
 * Created by quirin on 09.05.17.
 * <p>
 * based on http://stackoverflow.com/questions/4661459/dialog-problem-requestfeature-must-be-called-before-adding-content#4661526
 */

public class SubmitDialog extends DialogFragment {
    public static final String TAG = "SubmitDialog";
    private static final boolean LOG = true && Log.ENABLED;
    private SubmitDialogListener mListener;
    private SubsamplingScaleImageView photoView;
    private LightPosition mCurrentPos;
    private Crosshair centerCross;


    public interface SubmitDialogListener {

        void submitCommitted(Photo snapshot, TaskMonitor monitor);

        void submitDiscarded();
    }

    public static class Builder {
        private final SubmitDialog dialog;

        public Builder(SubmitDialogListener host) {
            this.dialog = new SubmitDialog();
            dialog.mListener = host;
        }

        public SubmitDialog.Builder setPhoto(Photo photo) {
            dialog.mPhoto = photo;
            return this;
        }

        public SubmitDialog build() {
            if (dialog.mPhoto == null)
                throw new IllegalArgumentException("photo is required. Use SubmitDialog.Builder.setPhoto(...) to set a photo!");

            if (dialog.mPhoto.bitMap == null)
                throw new IllegalArgumentException("Photo.bitMap was null but is required!");

            return this.dialog;
        }
    }

    private Photo mPhoto;
    private ViewGroup mContainer;

    public void setData(Photo photo) {
        this.mPhoto = photo;
        this.mCurrentPos = photo.lightPositions.getMostRelevant();
    }

    private final DialogInterface.OnClickListener positiveAction = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            Photo photo = mPhoto;

            // read out current light-position from photoview:
            if (photoView != null) {
                PointF center = photoView.getCenter();
                LightPosition pos = photo.lightPositions.getMostRelevant();
                if (pos != null) {
                    pos.x = center.x / photo.bitMap.getWidth();
                    pos.y = center.y / photo.bitMap.getHeight();
                }
            }

            Context appCtx = SubmitDialog.this.getActivity().getApplicationContext();
            TaskMonitor monitor = PersistenceManager.shared.persistDataAndUploadPicture(appCtx, photo);

            if (mListener != null) {
                mListener.submitCommitted(photo, monitor);
            }
        }
    };
    private final DialogInterface.OnClickListener negativeAction = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (mListener != null) {
                mListener.submitDiscarded();
            }
        }
    };


    @Override
    public void onDismiss(DialogInterface dialog) {
        if (LOG) Log.d(TAG, "onDismiss");
        super.onDismiss(dialog);

        if (mListener != null) {
            mListener.submitDiscarded();
        }
    }

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

        initView(view);

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
                .setTitle("Markieren")
                .setNegativeButton("Verwerfen", negativeAction)
                .setPositiveButton("Absenden", positiveAction)
                .setView(view);

        AlertDialog alertDialog = builder.create();
        return alertDialog;
    }

    void initView(View root) {
        final Photo photo = mPhoto;
        if (photo != null) {
            final SubsamplingScaleImageView photoView = (SubsamplingScaleImageView) root.findViewById(R.id.submit_photoView);
            if (photoView == null) {
                Log.e(TAG, "missing layout-element: R.id.submit_photoView");
                return;
            }
            photoView.setPanLimit(PAN_LIMIT_CENTER);
            photoView.setImage(ImageSource.bitmap(photo.bitMap));
            this.photoView = photoView;

            LightPosition pos = photo.lightPositions.getMostRelevant();
            // photoView eats absolute positions
            final float w = photo.bitMap.getWidth();
            final float h = photo.bitMap.getHeight();
            float x = (float) (w * pos.x);
            float y = (float) (h * pos.y);
//            photoView.setScaleAndCenter(2, new PointF(x, y));

            centerCross = (Crosshair) root.findViewById(R.id.submit_crossHair);
            if (centerCross == null) {
                Log.e(TAG, "missing layout-element: R.id.submit_crossHair");
                return;
            }
            updatePhase();

            final TextView txtCenter = (TextView) root.findViewById(R.id.submit_txt_center);
            photoView.setOnStateChangedListener(new SubsamplingScaleImageView.OnStateChangedListener() {
                @Override
                public void onScaleChanged(float v, int i) {

                }

                @Override
                public void onCenterChanged(PointF pointF, int i) {
                    DecimalFormat d = new DecimalFormat("#");
                    DecimalFormat f = new DecimalFormat("#.##");
                    txtCenter.setText(d.format(pointF.x) + ";" + d.format(pointF.y) + "  " + f.format(pointF.x / w) + ";" + f.format(pointF.y / h));
                }
            });
        }
    }

    void updatePhase() {
        LightPosition pos = mCurrentPos;
        if (pos == null) return;

        int color;
        switch (pos.getPhase()) {
            case RED:
                color = 0x88FF0000;
                break;
            case GREEN:
                color = 0x8800ff00;
                break;
            default:
                color = 0x88000000;
        }
        centerCross.setCrossColor(color);
    }

}
