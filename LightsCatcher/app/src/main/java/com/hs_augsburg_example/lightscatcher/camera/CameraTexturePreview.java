package com.hs_augsburg_example.lightscatcher.camera;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.TextureView;
import android.view.View;

import com.hs_augsburg_example.lightscatcher.utils.Log;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;

/**
 * Created by quirin on 01.05.17.
 */

/**
 * A Camera-Preview which supports zooming
 */
public class CameraTexturePreview extends TextureView implements TextureView.SurfaceTextureListener, View.OnTouchListener, ScaleGestureDetector.OnScaleGestureListener {
    static final String TAG = "CameraTextureView";
    static final boolean LOG = true && Log.ENABLED;
    private Camera camera;
    private float mScale = 1.0f;
    private float maxScale = 6.0f;
    private float minScale = 1.0f;
    private ScaleGestureDetector mScaleDetector;
    private Point mSurfaceSize;
    private PointF mPivotR;
    private Camera.Size mPictureSize;
    private Camera.Size mPreviewSize;
    private SurfaceTexture mSurface;
    private float mZoomStep = 1.1f;

    public CameraTexturePreview(Context context) {
        super(context);
        this.setSurfaceTextureListener(this);
        this.setOnTouchListener(this);
        mScaleDetector = new ScaleGestureDetector(context, this);
    }

    public void setPivotRelative(PointF pivot) {
        mPivotR = pivot;
    }


    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if (LOG) Log.d(TAG, "onSurfaceTextureAvailable: {0};{1}", width, height);

        mSurfaceSize = new Point(width, height);
        mSurface = surface;
        if (camera != null) {
            initCamera(camera, mSurface);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        if (LOG) Log.d(TAG, "onSurfaceTextureSizeChanged: {0};{1}", width, height);
        mSurfaceSize = new Point(width, height);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        if (LOG) Log.d(TAG, "onSurfaceTextureDestroyed");
        this.mSurface = null;
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        //if (LOG) Log.d(TAG, "onSurfaceTextureUpdated");

    }

    void initCamera(Camera cam, SurfaceTexture surfaceTexture) {


        this.camera = cam;
        this.mSurface = surfaceTexture;

        Camera.Parameters params = cam.getParameters();

        double aspect = (double) mSurfaceSize.x / mSurfaceSize.y;
        mPictureSize = CameraUtil.chooseOptimalSize(params.getSupportedPictureSizes(), 1920, 1080, aspect);
        if (LOG) Log.d(TAG, "choose pictureSize: {0}x{1}", mPictureSize.width, mPictureSize.height);

        aspect = (double) mPictureSize.height / mPictureSize.width;
        mPreviewSize = CameraUtil.chooseOptimalSize(params.getSupportedPreviewSizes(), mSurfaceSize.y, mSurfaceSize.x, aspect);
        if (LOG) Log.d(TAG, "choose previewSize: {0}x{1}", mPreviewSize.width, mPreviewSize.height);

        params.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
//        params.setPreviewSize(1056,864);
        params.setPictureSize(mPictureSize.width, mPictureSize.height);
//        params.setPictureSize(640,480);

        List<String> supportedFocusModes = params.getSupportedFocusModes();

        if (LOG) Log.d(TAG, "supported focusmodes:");
        forLoop:
        for (String mode : supportedFocusModes) {
            if (LOG) Log.d(TAG, '\t' + mode);
            switch (mode) {
                case Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE:
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                    break forLoop;
                case Camera.Parameters.FOCUS_MODE_AUTO:
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                    break; // don't leave for loop because we might get the better continuous-picture-mode in next iterations
            }
        }

        cam.setParameters(params);
        try {
            camera.setPreviewTexture(surfaceTexture);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setCamera(Camera camera) {
        this.camera = camera;
        if (camera != null && mSurface != null) {
            initCamera(camera, mSurface);
        }
    }

    public Camera getCamera() {
        return this.camera;
    }

    public void statPreview() {
        if (camera != null)
            camera.startPreview();
        else if (LOG) Log.e("APP", "cannot start camera-preview. camera was null");
    }

    public void releaseCamera() {
        if (camera != null) {

            camera.stopPreview();
            try {
                camera.setPreviewTexture(null);
            } catch (IOException e) {
                e.printStackTrace();
            }
            camera.release();
            camera = null;
        }
    }

    void updateScale() {
        if (mSurfaceSize == null) return;

        // calculate transformation matrix
        Matrix matrix = new Matrix();
        matrix.setScale(mScale, mScale, mSurfaceSize.x * mPivotR.x, mSurfaceSize.y * mPivotR.y);
        this.setTransform(matrix);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        mScaleDetector.onTouchEvent(event);
        return true;
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        mScale *= detector.getScaleFactor();
        if (mScale > maxScale) {
            mScale = maxScale;
        } else if (mScale < minScale) {
            mScale = minScale;
        }
        updateScale();
        return true;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {

    }

    public void zoomIn() {
        mScale *= mZoomStep;
        if (mScale > maxScale) {
            mScale = maxScale;
        }
        updateScale();
    }

    public void zoomOut() {
        mScale /= mZoomStep;
        if (mScale < minScale) {
            mScale = minScale;
        }
        updateScale();
    }

    public PointF translateLayoutToImage(PointF rel) {
        if (LOG)
            Log.d(TAG, "surface: {0}x{1}", mSurfaceSize.x, mSurfaceSize.y);
        if (LOG)
            Log.d(TAG, "preview: {0}x{1}", mPreviewSize.height, mPreviewSize.width);
        if (LOG)
            Log.d(TAG, "picture: {0}x{1}", mPictureSize.height, mPictureSize.width);

        if (LOG) Log.d(TAG, "relative layout: {0}; {1}", rel.x, rel.y);

        // absolute layout coordinates
        double x = rel.x * mPreviewSize.height;
        double y = rel.y * mPreviewSize.width;
        if (LOG) Log.d(TAG, "absolute layout: {0}; {1}", x, y);

        // cropped borders:
        double left = (mPictureSize.height - mPreviewSize.height) / 2;
        double top = (mPictureSize.width - mPreviewSize.width) / 2;
        if (LOG) Log.d(TAG, "borders: {0};{1}", left, top);

        // absolute image coordinates:
        x += left;
        y += top;
        if (LOG) Log.d(TAG, "absolute image: {0}; {1}", x, y);

        // relative image coordinates:
        x = x / mPictureSize.height;
        y = y / mPictureSize.width;
        if (LOG) Log.d(TAG, "relative image: {0}; {1}", x, y);

        return new PointF((float) (x), (float) (y));
    }

    public PointF translateImageToLayout(PointF rel) {

        // absolute image coordinates
        double x = rel.x * mPictureSize.width;
        double y = rel.y * mPictureSize.height;

        // cropped borders:
        double left = (mPictureSize.width - mPreviewSize.width) / 2;
        double top = (mPictureSize.height - mPreviewSize.height) / 2;

        // absolute layout:
        x += left;
        y += top;

        // relative layout:
        return new PointF((float) (x / mPreviewSize.width), (float) (y / mPreviewSize.height));
    }

    public float getZoom() {
        return mScale;
    }
}
