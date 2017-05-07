package com.hs_augsburg_example.lightscatcher.camera;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.TextureView;
import android.view.View;

import com.hs_augsburg_example.lightscatcher.camera.utils.CameraUtil;

import java.io.IOException;

/**
 * Created by quirin on 01.05.17.
 */
public class CameraTextureView extends TextureView implements TextureView.SurfaceTextureListener, View.OnTouchListener, ScaleGestureDetector.OnScaleGestureListener {
    static final String TAG = "CameraTextureView";
    static final boolean LOG = false;
    public Camera camera;
    private float mScale = 1.0f;
    private float maxScale = 10.0f;
    private float minScale = 1.0f;
    private ScaleGestureDetector mScaleDetector;
    private Point mSurfaceSize;
    private PointF mPivotR;
    private Camera.Size mPictureSize;
    private Camera.Size mPreviewSize;
    private SurfaceTexture mSurface;
    private float mZoomStep = 1.1f;

    public CameraTextureView(Context context) {
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
        if (LOG) Log.d(TAG, "onSurfaceTextureAvailable");
        mSurfaceSize = new Point(width, height);
        mSurface = surface;
        if (camera != null) {
            initCamera(camera, mSurface);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        if (LOG) Log.d(TAG, "onSurfaceTextureSizeChanged");
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
        if (LOG) Log.d(TAG, "onSurfaceTextureUpdated");

    }

    void initCamera(Camera cam, SurfaceTexture surfaceTexture) {
        try {
            camera.setPreviewTexture(surfaceTexture);
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.camera = cam;
        this.mSurface = surfaceTexture;

        Camera.Parameters parameters = cam.getParameters();

        mPictureSize = CameraUtil.getMediumPictureSize(parameters);
        mPreviewSize = CameraUtil.chooseOptimalSize(parameters.getSupportedPreviewSizes(), mSurfaceSize.x, mSurfaceSize.y, mPictureSize);

        parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
        parameters.setPictureSize(mPictureSize.width, mPictureSize.height);

        cam.setParameters(parameters);
    }

    public void setCamera(Camera camera) {
        this.camera = camera;
        if (camera != null && mSurface != null) {
            initCamera(camera, mSurface);
        }
    }

    public void statPreview() {
        if (camera != null)
            camera.startPreview();
        else if (LOG) Log.e("APP", "cannot start camera-preview. camera was null");
    }

    public void releaseCamera() {
        camera.stopPreview();
        try {
            camera.setPreviewTexture(null);
        } catch (IOException e) {
            e.printStackTrace();
        }
        camera.release();
        camera = null;
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
}
