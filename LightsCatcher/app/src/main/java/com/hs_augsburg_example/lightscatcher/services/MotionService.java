package com.hs_augsburg_example.lightscatcher.services;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.ExifInterface;

/**
 * Created by patrickvalenta on 15.04.17.
 */

// see here https://gist.github.com/abdelhady/501f6e48c1f3e32b253a#file-deviceorientation

public class MotionService {
    private final int ORIENTATION_PORTRAIT = ExifInterface.ORIENTATION_ROTATE_90; // 6
    private final int ORIENTATION_LANDSCAPE_REVERSE = ExifInterface.ORIENTATION_ROTATE_180; // 3
    private final int ORIENTATION_LANDSCAPE = ExifInterface.ORIENTATION_NORMAL; // 1
    private final int ORIENTATION_PORTRAIT_REVERSE = ExifInterface.ORIENTATION_ROTATE_270; // 8

    int smoothness = 1;
    private float averagePitch = 0;
    private float averageRoll = 0;
    private int orientation = ORIENTATION_PORTRAIT;

    private float[] pitches;
    private float[] rolls;

    public MotionService() {
        pitches = new float[smoothness];
        rolls = new float[smoothness];
    }

    public SensorEventListener getEventListener() {
        return sensorEventListener;
    }

    public int getOrientation() {
        return orientation;
    }

    SensorEventListener sensorEventListener = new SensorEventListener() {
        float[] mGravity;
        float[] mGeomagnetic;

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
                mGravity = event.values;
            if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
                mGeomagnetic = event.values;
            if (mGravity != null && mGeomagnetic != null) {
                float R[] = new float[9];
                float I[] = new float[9];
                boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
                if (success) {
                    float orientationData[] = new float[3];
                    SensorManager.getOrientation(R, orientationData);
                    float pitch = Math.abs(orientationData[1]);
                    float roll = orientationData[2];

                    averagePitch = (float) (Math.toDegrees(pitch) / 100);

                    System.out.println(averagePitch);
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // TODO Auto-generated method stub

        }
    };

    private float addValue(float value, float[] values) {
        value = (float) Math.round((Math.toDegrees(value)));
        float average = 0;
        for (int i = 1; i < smoothness; i++) {
            values[i - 1] = values[i];
            average += values[i];
        }
        values[smoothness - 1] = value;
        average = (average + value) / smoothness;
        return average;
    }

    public float getPitch() {
        return averagePitch;
    }
}
