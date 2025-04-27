package com.example.m4me.sensor;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.PowerManager;
import android.util.Log;

import com.example.m4me.activity.BlackScreenActivity;

public class LightSensor {
    private static final String TAG = "LightSensorManager";
    private static final float LIGHT_THRESHOLD = 20.0f; // Adjust based on testing

    private final Context context;
    private final SensorManager sensorManager;
    private final Sensor lightSensor;
    private final SensorEventListener lightSensorListener;
    private boolean isDarkMode = false;
    private OnLightChangeListener lightChangeListener;

    // Interface for callbacks
    public interface OnLightChangeListener {
        void onDarkDetected();
        void onLightDetected();
    }

    public LightSensor(Context context) {
        this.context = context;
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        lightSensor = sensorManager != null ? sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) : null;

        // Initialize the light sensor listener
        lightSensorListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                float lightLevel = event.values[0];
                Log.d(TAG, "Light level: " + lightLevel);

                // If it's too dark (likely in pocket)
                if (lightLevel < LIGHT_THRESHOLD && !isDarkMode) {
                    turnOffScreen();
                    isDarkMode = true;
                    if (lightChangeListener != null) {
                        lightChangeListener.onDarkDetected();
                    }
                }
                // If it's bright enough and we were in dark mode
                else if (lightLevel >= LIGHT_THRESHOLD && isDarkMode) {
                    releaseWakeLock();
                    isDarkMode = false;
                    if (lightChangeListener != null) {
                        lightChangeListener.onLightDetected();
                    }
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
                // Not needed for this implementation
            }
        };
    }

    public void setOnLightChangeListener(OnLightChangeListener listener) {
        this.lightChangeListener = listener;
    }

    public boolean hasLightSensor() {
        return lightSensor != null;
    }

    public void startMonitoring() {
        if (sensorManager != null && lightSensor != null) {
            sensorManager.registerListener(
                    lightSensorListener,
                    lightSensor,
                    SensorManager.SENSOR_DELAY_NORMAL
            );
            Log.d(TAG, "Light sensor started");
        } else {
            Log.d(TAG, "Light sensor not available");
        }
    }

    public void stopMonitoring() {
        if (sensorManager != null && lightSensorListener != null) {
            sensorManager.unregisterListener(lightSensorListener);
            Log.d(TAG, "Light sensor stopped");
        }
        releaseWakeLock();
    }

    private void turnOffScreen() {
        if (isDarkMode) return;

        Intent intent = new Intent(context, BlackScreenActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
        isDarkMode = true;
        if (lightChangeListener != null) {
            lightChangeListener.onDarkDetected();
        }
    }

    private void releaseWakeLock() {
        if (!isDarkMode) return;

        Intent intent = new Intent("ACTION_CLOSE_BLACK_SCREEN");
        context.sendBroadcast(intent);
        isDarkMode = false;
        if (lightChangeListener != null) {
            lightChangeListener.onLightDetected();
        }
    }

    public void release() {
        stopMonitoring();
        releaseWakeLock();
    }
}