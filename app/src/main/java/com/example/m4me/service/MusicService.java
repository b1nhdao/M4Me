package com.example.m4me.service;

import static com.example.m4me.MyApplication.CHANNEL_ID;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.m4me.R;
import com.example.m4me.activity.SongPlayingActivity;
import com.example.m4me.boardcastReceiver.MyReceiver;
import com.example.m4me.model.Song;
import com.example.m4me.sensor.LightSensor;
import com.example.m4me.sensor.ShakeSensor;

import java.io.File;
import java.util.List;


public class MusicService extends Service {

    private int currentSongIndex = 0;

    public static final int ACTION_PAUSE = 1;
    public static final int ACTION_RESUME = 2;
    public static final int ACTION_CLEAR = 3;
    public static final int ACTION_START = 4;
    public static final int ACTION_SEEK = 5;
    public static final int ACTION_PREV = 6;
    public static final int ACTION_NEXT = 7;
    public static final int ACTION_LOOP = 8;
    public static final int ACTION_NO_LOOP = 9;
    public static final int ACTION_SHUFFLE = 10;

    private ExoPlayer exoPlayer;

    public static ExoPlayer exoPlayerInstance;

    private MediaPlayer mediaPlayer;

    private boolean isPlaying;
    private boolean isLooping;
    private boolean isShuffling;

    private Song mSong;
    private List<Song> mSongList;
    public static final String Channel_ID = "music_channel";

    private LightSensor lightSensor;
    private ShakeSensor shakeSensor;

    private SharedPreferences sharedPreferences;
    private static final String fName = "settings.xml";

    private Boolean lightSensorEnabled;
    private Boolean shakeSensorEnabled;

    public MusicService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("Service", "Service onCreate");

        sharedPreferences = getSharedPreferences(fName, MODE_PRIVATE);
        readSettings();

        exoPlayer = new ExoPlayer.Builder(this).build();
        exoPlayerInstance = exoPlayer;
        exoPlayer.addListener(new Player.Listener() {
            @Override
            public void onIsPlayingChanged(boolean isPlayingNow) {
                isPlaying = isPlayingNow;
                sendNotification(mSong);

                if (isPlaying) {
                    startLightSensorIfNeeded();
                } else {
                    stopLightSensorIfNeeded();
                }
            }

            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if(playbackState == Player.STATE_ENDED){
                    playNextSong();
                }
            }
        });

        setupLightSensor();
        setupShakeDetector();
        if (shakeSensorEnabled) {
            shakeSensor.start();
        }
    }

    private void setupShakeDetector() {
        shakeSensor = new ShakeSensor(this, new ShakeSensor.OnShakeListener() {
            @Override
            public void onShake() {
                if (isPlaying && shakeSensorEnabled){
                    sendActionToActivity(ACTION_NEXT);
                    playNextSong();
                }
            }
        });

        if (!shakeSensor.hasAccelerometer()) {
            Toast.makeText(this, "get a new phone bruh !", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupLightSensor() {
        lightSensor = new LightSensor(this);

        // Set up callback for light changes
        lightSensor.setOnLightChangeListener(new LightSensor.OnLightChangeListener() {
            @Override
            public void onDarkDetected() {
                // You can handle dark detection here if needed
                Log.d("MusicService", "Dark detected - screen turned off");
            }

            @Override
            public void onLightDetected() {
                // You can handle light detection here if needed
                Log.d("MusicService", "Light detected - screen can turn on");
            }
        });

        if (!lightSensor.hasLightSensor()) {
            Log.w("MusicService", "Light sensor not available on this device");
        }
    }

    private void startLightSensorIfNeeded() {
        if (lightSensor != null && isPlaying) {
            lightSensor.startMonitoring();
        }
    }

    private void stopLightSensorIfNeeded() {
        if (lightSensor != null) {
            lightSensor.stopMonitoring();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Bundle bundle = intent.getExtras();
        readSettings();
        if(bundle != null){
            Song song = (Song) bundle.get("object_song");
            if (song != null) {
                mSongList = null;
                mSong = song;
                startMusic(mSong);
                sendNotification(mSong);
            }
            List<Song> songlist = (List<Song>) bundle.get("list_object_song");
            if(songlist != null){
                mSongList = songlist;
                currentSongIndex = bundle.getInt("current_song_index", 0);
                mSong = mSongList.get(currentSongIndex);
                startMusic(mSong);
                sendNotification(mSong);
            }
            String test = bundle.getString("key_test");
            Log.d("test", "onStartCommand: " + test);
        }

        if ("get_current_position".equals(intent.getAction())) {
            sendCurrentPositionToActivity();
        }

        int actionMusic = intent.getIntExtra("action_music_service", 0);
        int seekToPosition = intent.getIntExtra("seek_position", -1);
        handleActionMusic(actionMusic, seekToPosition);

        Log.d("actionmusic:" , actionMusic + "");

        return START_NOT_STICKY;
    }

    public static ExoPlayer getExoPlayer() {
        return exoPlayerInstance;
    }

    private void startMusic(Song song) {
        if (exoPlayer != null) {
            exoPlayer.stop();
            exoPlayer.clearMediaItems();
            if (isLooping){
                exoPlayer.setRepeatMode(Player.REPEAT_MODE_ONE);
            }
            else{
                exoPlayer.setRepeatMode(Player.REPEAT_MODE_OFF);
            }
        }
        MediaItem mediaItem;

        if (song.getSourceURL() != null){
            mediaItem = MediaItem.fromUri(song.getSourceURL());
        }
        else{
            mediaItem = MediaItem.fromUri(Uri.fromFile(new File(song.getFilePath())));
        }

        exoPlayer.setMediaItem(mediaItem);
        exoPlayer.prepare();
        exoPlayer.play();
        isPlaying = true;
        startLightSensorIfNeeded();
        sendActionToActivity(ACTION_START);
    }

    private void handleActionMusic(int action, int seekToPosition){
        switch (action){
            case ACTION_PAUSE:
                pauseMusic();
                sendActionToActivity(ACTION_PAUSE);
                break;

            case ACTION_RESUME:
                resumeMusic();
                sendActionToActivity(ACTION_RESUME);
                break;

            case ACTION_CLEAR:
                Intent clearIntent = new Intent("music_service_cleared");
                LocalBroadcastManager.getInstance(this).sendBroadcast(clearIntent);
                stopSelf();
                sendActionToActivity(ACTION_CLEAR);
                break;

            case ACTION_SEEK:
                if (seekToPosition >= 0 && exoPlayer != null) {
                    exoPlayer.seekTo(seekToPosition);
                }
                break;

            case ACTION_NEXT:
                playNextSong();
                break;

            case ACTION_PREV:
                playPreviousSong();
                break;

            case ACTION_LOOP:
                toggleLoopSong();
                break;
        }
    }

    private void pauseMusic(){
        if (exoPlayer != null && exoPlayer.isPlaying()){
            exoPlayer.pause();
            isPlaying = false;
            stopLightSensorIfNeeded();
            sendNotification(mSong);
        }
    }

    private void resumeMusic(){
        if(exoPlayer != null && !exoPlayer.isPlaying()){
            exoPlayer.play();
            isPlaying = true;
            startLightSensorIfNeeded();
            sendNotification(mSong);
        }
    }

    private void playNextSong(){
        if (mSongList != null) {
            if (currentSongIndex < mSongList.size() - 1){
                currentSongIndex++;
            }
            else if (currentSongIndex == mSongList.size() - 1){
                currentSongIndex = 0;
            }
            mSong = mSongList.get(currentSongIndex);
            startMusic(mSong);
            sendNotification(mSong);
            sendActionToActivity(ACTION_NEXT);
        }
        startMusic(mSong);
        sendNotification(mSong);
        sendActionToActivity(ACTION_NEXT);
    }

    private void playPreviousSong(){
        if (mSongList != null && currentSongIndex > 0){
            currentSongIndex--;
            mSong = mSongList.get(currentSongIndex);
            startMusic(mSong);
            sendNotification(mSong);
            sendActionToActivity(ACTION_PREV);
        }
    }

    private void toggleLoopSong(){
        if (exoPlayer != null){
            if (isLooping){
                exoPlayer.setRepeatMode(Player.REPEAT_MODE_OFF);
                isLooping = false;
            }
            else{
                exoPlayer.setRepeatMode(Player.REPEAT_MODE_ONE);
                isLooping = true;
            }
            sendActionToActivity(ACTION_LOOP);
        }
    }

    private void sendNotification(Song song) {
        Intent intent = new Intent(this, SongPlayingActivity.class);
        Bundle bundle = new Bundle();
        bundle.putSerializable("object_song", mSong);
        intent.putExtras(bundle);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.custom_notification);
        remoteViews.setTextViewText(R.id.tv_songTitle, shortenString(song.getTitle(), 20));
        remoteViews.setTextViewText(R.id.tv_songArtist, song.getArtistName());
        remoteViews.setImageViewResource(R.id.img_play_or_pause, R.drawable.pause_circle_24px);
        remoteViews.setImageViewResource(R.id.img_clear, R.drawable.close_24px);
        remoteViews.setImageViewResource(R.id.img_song, R.drawable.baseline_library_music_24);

        if(isPlaying){
            remoteViews.setOnClickPendingIntent(R.id.img_play_or_pause, getPendingIntent(this, ACTION_PAUSE));
            remoteViews.setImageViewResource(R.id.img_play_or_pause, R.drawable.pause_circle_24px);
        } else {
            remoteViews.setOnClickPendingIntent(R.id.img_play_or_pause, getPendingIntent(this, ACTION_RESUME));
            remoteViews.setImageViewResource(R.id.img_play_or_pause, R.drawable.play_circle_24px);
        }

        remoteViews.setOnClickPendingIntent(R.id.img_clear, getPendingIntent(this, ACTION_CLEAR));

        Notification notification = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(R.drawable.baseline_library_music_24)
                .setCustomContentView(remoteViews)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setSound(null)
                .build();


        Glide.with(getApplicationContext())
                .asBitmap()
                .load(song.getThumbnailUrl())
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        remoteViews.setImageViewBitmap(R.id.img_song, resource);
                        startForeground(1, notification);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                    }
                });
    }

    private String shortenString(String s, int charMaxLength){
        if (s.length() >= charMaxLength){
            return s.substring(0,charMaxLength) + "...";
        }
        return s;
    }

    private PendingIntent getPendingIntent(Context context, int action){
        Intent intent = new Intent(this, MyReceiver.class);
        intent.putExtra("action_music", action);
        return PendingIntent.getBroadcast(context, action, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("Service", "Service onDestroy");
        if(exoPlayer != null){
            exoPlayer.release();
            exoPlayer = null;
        }

        if (lightSensor != null) {
            lightSensor.release();
            lightSensor = null;
        }

        if(exoPlayer != null){
            exoPlayer.release();
            exoPlayer = null;
        }
    }

    private void sendActionToActivity(int action){
        Intent intent = new Intent("send_data_to_activity");
        Bundle bundle = new Bundle();
        bundle.putSerializable("object_song", mSong);
        bundle.putBoolean("status_player", isPlaying);
        bundle.putBoolean("status_loop", isLooping);
        bundle.putInt("action_music", action);

        intent.putExtras(bundle);

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void sendCurrentPositionToActivity() {
        if (exoPlayer != null) {
            Intent intent = new Intent("update_seekbar");
            intent.putExtra("current_position", exoPlayer.getCurrentPosition());
            intent.putExtra("duration", exoPlayer.getDuration());
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        }
    }

    private void readSettings(){
        lightSensorEnabled = sharedPreferences.getBoolean("light_sensor", false);
        shakeSensorEnabled = sharedPreferences.getBoolean("shake_sensor", false);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        stopSelf();
        super.onTaskRemoved(rootIntent);
    }
}

