package com.example.m4me.sensor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class ShakeSensor {
    private static final String TAG = "ShakeDetectorManager";
    private static final float SHAKE_THRESHOLD_GRAVITY = 2.5F;
    private static final int SHAKE_SLOP_TIME_MS = 500;
    private static final int SHAKE_COUNT_RESET_TIME_MS = 3000;

    private final SensorManager sensorManager;
    private final Sensor accelerometer;
    private final ShakeListener shakeDetector;
    private OnShakeListener listener;
    private boolean isEnabled = false;

    private BroadcastReceiver clearReceiver;
    private Context context;


    public interface OnShakeListener {
        void onShake();
    }
    
    public ShakeSensor(Context context, OnShakeListener listener) {
        this.listener = listener;
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager != null ?
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) : null;
        shakeDetector = new ShakeListener();

        clearReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                stop();
            }
        };

        if (accelerometer == null) {
            Log.w(TAG, "No accelerometer found on this device");
        }
    }

    public void registerServiceClearListener() {
        LocalBroadcastManager.getInstance(context).registerReceiver(clearReceiver, new IntentFilter("music_service_cleared"));
    }

    public void unregisterServiceClearListener() {
        LocalBroadcastManager.getInstance(context).unregisterReceiver(clearReceiver);
    }

    public void cleanup() {
        stop();
        unregisterServiceClearListener();
    }


    public boolean start() {
        if (sensorManager != null && accelerometer != null && !isEnabled) {
            sensorManager.registerListener(shakeDetector, accelerometer, SensorManager.SENSOR_DELAY_UI);
            isEnabled = true;
            return true;
        }
        return false;
    }

    public void stop() {
        if (sensorManager != null && isEnabled) {
            sensorManager.unregisterListener(shakeDetector);
            isEnabled = false;
        }
    }



    public boolean hasAccelerometer() {
        return accelerometer != null;
    }

    private class ShakeListener implements SensorEventListener {
        private long lastShakeTimestamp;
        private int shakeCount;
        private float threshold = SHAKE_THRESHOLD_GRAVITY;
        private final Handler mainHandler = new Handler(Looper.getMainLooper());


        @Override
        public void onSensorChanged(SensorEvent event) {
            if (listener == null) return;

            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            // remove g for each axis
            float gX = x / SensorManager.GRAVITY_EARTH;
            float gY = y / SensorManager.GRAVITY_EARTH;
            float gZ = z / SensorManager.GRAVITY_EARTH;

            // gForce will be close to 1 when there is no movement
            float gForce = (float) Math.sqrt(gX * gX + gY * gY + gZ * gZ);

            if (gForce > threshold) {
                final long now = System.currentTimeMillis();

                // if shakes are too close
                if (lastShakeTimestamp + SHAKE_SLOP_TIME_MS > now) {
                    return;
                }

                // reset shake count after period of no shakes
                if (lastShakeTimestamp + SHAKE_COUNT_RESET_TIME_MS < now) {
                    shakeCount = 0;
                }

                lastShakeTimestamp = now;
                shakeCount++;

                // Notify on main thread to avoid UI issues
                mainHandler.post(() -> {
                    if (listener != null) {
                        listener.onShake();
                    }
                });
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    }
}