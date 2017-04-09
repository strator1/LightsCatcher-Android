package com.hs_augsburg_example.lightscatcher.camera;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.util.Size;
import android.widget.FrameLayout;

import com.hs_augsburg_example.lightscatcher.R;

import org.w3c.dom.Text;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;

public class TakePictureActivity extends AppCompatActivity{

    private Camera cam;
    private CameraPreview camPreview;

    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

//            Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);

            onAfterPictureTaken(data);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_take_picture);

        if (checkCameraHardware(getApplicationContext())) {
            if (cam != null) {
                cam.release();
            }
            cam = getCameraInstance();
            cam.setDisplayOrientation(0);

            camPreview = new CameraPreview(this, cam);
            FrameLayout preview = (FrameLayout) findViewById(R.id.take_picture_cameraPreview);

            preview.addView(camPreview);
        };
    }

    public void onCaptureButtonPressed(View view) {
        cam.takePicture(null, null, mPicture);
    }

    private void onAfterPictureTaken(byte[] data) {
        Intent intent = new Intent(this, TagLightsActivity.class);
        intent.putExtra("image", data);
        startActivity(intent);
    }

    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }

    /** A safe way to get an instance of the Camera object. */
    private Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
            e.printStackTrace();
        }
        return c; // returns null if camera is unavailable
    }

    public void navigateBack(View view) {

    }
}
