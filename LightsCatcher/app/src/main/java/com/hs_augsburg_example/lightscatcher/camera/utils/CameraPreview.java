package com.hs_augsburg_example.lightscatcher.camera.utils;

import android.content.Context;
import android.hardware.Camera;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.hs_augsburg_example.lightscatcher.utils.Log;

import java.io.IOException;

/**
 * Created by patrickvalenta on 08.04.17.
 */

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private static final String TAG = "CameraPreview";
    private static final boolean LOG = Log.ENABLED && true;

    private SurfaceHolder mHolder;
    private Camera mCamera;

    private Camera.Size mPreviewSize;
    private Camera.Size mPictureSize;

    public CameraPreview(Context context, Camera camera) {
        super(context);
        mCamera = camera;

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, now tell the camera where to draw the preview.
        try {
            mCamera.setPreviewDisplay(holder);
            startPreview(holder.getSurfaceFrame().width(), holder.getSurfaceFrame().height());
        } catch (IOException e) {
            Log.e(TAG, "Error setting camera preview: " + e.getMessage());
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // empty. Take care of releasing the Camera preview in your activity.
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.

        if (mHolder.getSurface() == null) {
            // preview surface does not exist
            return;
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
            // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here

        // start preview with new settings
        try {
            mCamera.setPreviewDisplay(mHolder);
            startPreview(w, h);

        } catch (Exception e) {
            if (LOG)Log.e(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);
        setMeasuredDimension(width, height);

        mPictureSize = CameraUtil.getMediumPictureSize(mCamera.getParameters());
        mPreviewSize = CameraUtil.chooseOptimalSize(mCamera.getParameters().getSupportedPreviewSizes(), width, height, mPictureSize);

    }

    private void startPreview(int w, int h) {
        Camera.Parameters parameters = mCamera.getParameters();

        parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
        parameters.setPictureSize(mPictureSize.width, mPictureSize.height);

        if (LOG)Log.d(TAG,"PICTURE SIZE: " + mPictureSize.width + ", " + mPictureSize.height);

        mCamera.setParameters(parameters);
        mCamera.startPreview();
    }

    public Camera getCamera() {
        return mCamera;
    }

}
