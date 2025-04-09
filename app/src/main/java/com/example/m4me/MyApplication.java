package com.example.m4me;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.util.Log;

import com.cloudinary.android.MediaManager;

import java.util.HashMap;
import java.util.Map;

public class MyApplication extends Application {
    public static final String CHANNEL_ID = "channel_service";

    @Override
    public void onCreate() {
        super.onCreate();

        createChannelNotification();
        initConfig();
    }

    private void createChannelNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Channel Service", NotificationManager.IMPORTANCE_LOW);

            channel.setSound(null, null);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if(manager != null){
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void initConfig() {
        try {
            Map config = new HashMap();
            config.put("cloud_name", getString(R.string.cloudinary_cloud_name));
            config.put("api_key", getString(R.string.cloudinary_api_key));
            config.put("api_secret", getString(R.string.cloudinary_api_secret));
            MediaManager.init(this, config);
        } catch (IllegalStateException e) {
            Log.d("cloudinary", "MediaManager already initialized");

        }
    }
}
