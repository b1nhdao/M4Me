package com.example.m4me.boardcastReceiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.m4me.service.MusicService;

public class MyReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        int actionMusic = intent.getIntExtra("action_music", 0);

        Intent intentService = new Intent(context, MusicService.class);
        intentService.putExtra("action_music_service", actionMusic);
        context.startService(intentService);
    }
}