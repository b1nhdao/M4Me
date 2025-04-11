package com.example.m4me.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.bumptech.glide.Glide;
import com.example.m4me.R;
import com.example.m4me.model.Song;
import com.example.m4me.sensor.ShakeSensor;
import com.example.m4me.service.MusicService;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class SongPlayingActivity extends AppCompatActivity {

    private List<Song> songList = new ArrayList<>();
    private Song currentSong;
    int songDuration;

    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable runnable;

    private TextView tv_songArtist, tv_songTitle, tv_currentTime ,tv_endTime;
    private ImageView img_songThumbnail;

    private ImageView img_forward, img_play_or_pause, img_skip, img_favourite, img_comment, img_loop, img_options;
    private SeekBar seekBar;

    private boolean isPlaying;
    private boolean isLooping;

    private ShakeSensor shakeManager;

    private void setupShakeDetector() {
        shakeManager = new ShakeSensor(this, new ShakeSensor.OnShakeListener() {
            @Override
            public void onShake() {
                sendActionToService(MusicService.ACTION_NEXT);
            }
        });

        if (!shakeManager.hasAccelerometer()) {
            Toast.makeText(this, "get a new phone bruh !", Toast.LENGTH_SHORT).show();
        }
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            if(bundle == null){
                return;
            }
            currentSong = (Song) bundle.get("object_song");
            isPlaying = bundle.getBoolean("status_player");
            isLooping = bundle.getBoolean("status_loop");
            int actionMusic = bundle.getInt("action_music");

            handleLayoutMusic(actionMusic);
        }
    };

    private BroadcastReceiver seekbarReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long currentPosition = intent.getLongExtra("current_position", 0);
            long duration = intent.getLongExtra("duration", 0);

//            update timer
            songDuration = (int) duration / 1000;
            tv_endTime.setText(formatTime(songDuration));
            tv_currentTime.setText(formatTime((int) currentPosition / 1000));

            seekBar.setMax((int) duration);
            seekBar.setProgress((int) currentPosition);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_song_playing);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();

        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, new IntentFilter("send_data_to_activity"));
        LocalBroadcastManager.getInstance(this).registerReceiver(seekbarReceiver, new IntentFilter("update_seekbar"));

        setupShakeDetector();
        shakeManager.start();

        if (shakeManager != null) {
            shakeManager.registerServiceClearListener();
        }

        Bundle bundle = getIntent().getExtras();
        if(bundle != null){
            songList = (List<Song>) bundle.get("list_object_song");
            currentSong = (Song) bundle.get("object_song");
            updateUIInitiate(currentSong);
        }
        else{
            Log.d("SongList", "no bundle found");
        }

        startSeekBarUpdater();

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser){
                    sendActionToService(MusicService.ACTION_SEEK, progress);
                    tv_currentTime.setText(formatTime(progress / 1000));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        img_forward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendActionToService(MusicService.ACTION_PREV);
            }
        });

        img_skip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendActionToService(MusicService.ACTION_NEXT);
            }
        });

        img_play_or_pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnPlayOrPauseOnClick(isPlaying);
            }
        });

        img_favourite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                adding this song to user/favouriteSong
            }
        });

        img_comment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                open comments widget
            }
        });

        img_loop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendActionToService(MusicService.ACTION_LOOP);
            }
        });
    }

    private void sendActionToService(int action){
        Intent intent = new Intent(this, MusicService.class);
        intent.putExtra("action_music_service", action);
        startService(intent);
    }

    private void sendActionToService(int action, int progress){
        Intent intent = new Intent(this, MusicService.class);
        intent.putExtra("action_music_service", action);
        intent.putExtra("seek_position", progress);
        startService(intent);
    }


    private void btnPlayOrPauseOnClick(boolean isPlaying){
        if (isPlaying){
            sendActionToService(MusicService.ACTION_PAUSE);
        }
        else {
            sendActionToService(MusicService.ACTION_RESUME);
        }
    }

    private void updateUI(){
        if (currentSong != null){
            Glide.with(this).load(currentSong.getThumbnailUrl()).into(img_songThumbnail);
            tv_songArtist.setText(currentSong.getArtistName());
            tv_songTitle.setText(currentSong.getTitle());
            tv_endTime.setText(formatTime(songDuration));
        }
    }

//    idk why
    private void updateUIInitiate(Song song){
        Glide.with(this).load(song.getThumbnailUrl()).into(img_songThumbnail);
        tv_songArtist.setText(song.getArtistName());
        tv_songTitle.setText(song.getTitle());
    }

    private String formatTime(int duration) {
        int minutes = (duration / 60);
        int seconds = (duration % 60);
        if(seconds < 10){
            return minutes + ":0" + seconds;
        }
        else {
            return (minutes + ":" + seconds);
        }
    }

    private void handleLayoutMusic(int action){
        switch (action){
            case MusicService.ACTION_START:
                setStatusButtonPlayOrPause();
                startSeekBarUpdater();
                updateUI();
                break;
            case MusicService.ACTION_PAUSE:
                isPlaying = false;
                setStatusButtonPlayOrPause();
                break;
            case MusicService.ACTION_RESUME:
                isPlaying = true;
                setStatusButtonPlayOrPause();
                break;
            case MusicService.ACTION_CLEAR:
                stopSeekBarUpdater();
                break;
            case MusicService.ACTION_LOOP:
                setStatusButtonLoop();
                break;
        }
    }

    private void setStatusButtonPlayOrPause(){
        if(isPlaying){
            img_play_or_pause.setImageResource(R.drawable.pause_circle_24px);
        }
        else{
            img_play_or_pause.setImageResource(R.drawable.play_circle_24px);
        }
    }

    private void setStatusButtonLoop(){
        if(isLooping){
            img_loop.setImageResource(R.drawable.baseline_repeat_on_24);
        }
        else{
            img_loop.setImageResource(R.drawable.baseline_repeat_24);
        }
    }

    private void startSeekBarUpdater() {
        runnable = new Runnable() {
            @Override
            public void run() {
                updateSeekBar();
//                tv_currentTime.setText(formatTime(player.getCurrentPosition() / 1000));
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(runnable);
    }

    private void updateSeekBar() {
        Intent intent = new Intent(this, MusicService.class);
        intent.setAction("get_current_position");
        startService(intent);
    }

    private void stopSeekBarUpdater() {
        if (handler != null && runnable != null) {
            handler.removeCallbacks(runnable);
        }
    }

    private void initViews(){
        tv_songArtist = findViewById(R.id.tv_songArtist);
        tv_songTitle = findViewById(R.id.tv_songTitle);
        img_songThumbnail = findViewById(R.id.img_songThumbnail);
        img_forward = findViewById(R.id.img_forward);
        img_play_or_pause = findViewById(R.id.img_play_or_pause);
        img_skip = findViewById(R.id.img_skip);
        img_favourite = findViewById(R.id.img_favourite);
        img_comment = findViewById(R.id.img_comment);
        img_loop = findViewById(R.id.img_loop);
        img_options = findViewById(R.id.img_options);
        seekBar = findViewById(R.id.seekBar);
        tv_currentTime = findViewById(R.id.tv_currentTime);
        tv_endTime = findViewById(R.id.tv_endTime);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (shakeManager != null) {
            shakeManager.cleanup();
        }
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(seekbarReceiver);
    }
}