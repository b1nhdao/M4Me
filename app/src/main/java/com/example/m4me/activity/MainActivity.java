package com.example.m4me.activity;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.bumptech.glide.Glide;
import com.example.m4me.R;
import com.example.m4me.fragment.HomeFragment;
import com.example.m4me.fragment.LibraryFragment;
import com.example.m4me.fragment.SearchFragment;
import com.example.m4me.model.Song;
import com.example.m4me.service.MusicService;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.datatype.Artwork;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottom_nav_view;
    public static FirebaseFirestore db;

    private RelativeLayout layout_bottom;
    private ImageView img_song, img_play_or_pause, img_clear, img_setting;
    private TextView tv_songTitle, tv_songArtist;

    private SeekBar seekBar;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable runnable;

    private Song mSong;
    private boolean isPlaying;

    private Dialog dialog;
    private CheckBox cb_settingLightSensor, cb_settingShakeSensor;


    private static final String fName = "settings.xml";
    private SharedPreferences sharedPreferences;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            if(bundle == null){
                return;
            }
            mSong = (Song) bundle.get("object_song");
            isPlaying = bundle.getBoolean("status_player");
            int actionMusic = bundle.getInt("action_music");

            handleLayoutMusic(actionMusic);
        }
    };

    private BroadcastReceiver seekbarReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long currentPosition = intent.getLongExtra("current_position", 0);
            long duration = intent.getLongExtra("duration", 0);

            seekBar.setMax((int) duration);
            seekBar.setProgress((int) currentPosition);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();

        db = FirebaseFirestore.getInstance();

        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, new IntentFilter("send_data_to_activity"));
        LocalBroadcastManager.getInstance(this).registerReceiver(seekbarReceiver, new IntentFilter("update_seekbar"));

        layout_bottom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SongPlayingActivity.class);
                Bundle bundle = new Bundle();
                bundle.putSerializable("object_song", mSong);
                intent.putExtras(bundle);
                startActivity(intent);
            }
        });

        loadFragment(new HomeFragment());

        handleBottomNavigationBarClick();

        // dialog
        dialog = new Dialog(this);
        dialog.setContentView(R.layout.setting_dialog);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawableResource(R.drawable.custom_dialog_background);
        dialog.setCancelable(true);

        cb_settingLightSensor = dialog.findViewById(R.id.cb_settingLightSensor);
        cb_settingShakeSensor = dialog.findViewById(R.id.cb_settingShakeSensor);


        img_setting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.show();
            }
        });

        cb_settingLightSensor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSettings();
            }
        });

        cb_settingShakeSensor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSettings();
            }
        });

        sharedPreferences = getSharedPreferences(fName, MODE_PRIVATE);
        readSettings();
    }

    private void initViews(){
        bottom_nav_view = findViewById(R.id.bottom_nav_view);
        layout_bottom = findViewById(R.id.layout_bottom);
        img_song = findViewById(R.id.img_song);
        img_play_or_pause = findViewById(R.id.img_play_or_pause);
        img_clear = findViewById(R.id.img_clear);
        tv_songArtist = findViewById(R.id.tv_songArtist);
        tv_songTitle = findViewById(R.id.tv_songTitle);
        seekBar = findViewById(R.id.seekBar);
        img_setting = findViewById(R.id.img_setting);
    }

    private void handleBottomNavigationBarClick(){
        bottom_nav_view.setOnItemSelectedListener(item -> {
            if(item.getItemId() == R.id.home){
                loadFragment(new HomeFragment());
                return true;
            }
            if(item.getItemId() == R.id.search){
                loadFragment(new SearchFragment());
                return true;
            }
            if(item.getItemId() == R.id.library){
                loadFragment(new LibraryFragment());
                return true;
            }
            return false;
        });
    }

    private void loadFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.main, fragment);
        fragmentTransaction.commit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(seekbarReceiver);
        handler.removeCallbacks(runnable);
    }

    private void handleLayoutMusic(int action){
        switch (action){
            case MusicService.ACTION_START:
                layout_bottom.setVisibility(View.VISIBLE);
                showInfoSong();
                setStatusButtonPlayOrPause();
                startSeekBarUpdater();
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
                layout_bottom.setVisibility(View.INVISIBLE);
                stopSeekBarUpdater();
                break;
        }
    }


    private void showInfoSong(){
        if(mSong == null){
            return;
        }
        tv_songTitle.setText(mSong.getTitle());
        tv_songArtist.setText(mSong.getArtistName());

        if (mSong.getSourceURL() != null){
            Glide.with(MainActivity.this).load(mSong.getThumbnailUrl()).into(img_song);
        }
        else {
            loadThumbnailFrom(mSong);
        }

        img_play_or_pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isPlaying){
                    sendActionToService(MusicService.ACTION_PAUSE);
                }
                else {
                    sendActionToService(MusicService.ACTION_RESUME);
                }
            }
        });

        img_clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendActionToService(MusicService.ACTION_CLEAR);
            }
        });

        startSeekBarUpdater();

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser){
                    sendActionToService(MusicService.ACTION_SEEK, progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

//    SeekBar things

    private void startSeekBarUpdater() {
        runnable = new Runnable() {
            @Override
            public void run() {
                updateSeekBar();
                handler.postDelayed(this, 1000); // cập nhật mỗi 1 giây
            }
        };
        handler.post(runnable);
    }

    private void stopSeekBarUpdater() {
        if (handler != null && runnable != null) {
            handler.removeCallbacks(runnable);
        }
    }

    private void updateSeekBar() {
        Intent intent = new Intent(this, MusicService.class);
        intent.setAction("get_current_position");
        startService(intent);
    }

//    End Seekbar things

    private void setStatusButtonPlayOrPause(){
        if(isPlaying){
            img_play_or_pause.setImageResource(R.drawable.pause_circle_24px);
        }
        else{
            img_play_or_pause.setImageResource(R.drawable.play_circle_24px);
        }
    }

    private void sendActionToService(int action){
        Intent intent = new Intent(this, MusicService.class);
        intent.putExtra("action_music_service", action);
        startService(intent);
    }

    private void sendActionToService(int action, int duration){
        Intent intent = new Intent(this, MusicService.class);
        intent.putExtra("action_music_service", action);
        intent.putExtra("seek_position", duration);
        startService(intent);
    }

    private void loadThumbnailFrom(Song song){
        try {
            //  jAudiotagger to read metadata
            AudioFile audioFile = AudioFileIO.read(new File(song.getFilePath()));
            Tag tag = audioFile.getTag();

            if (tag != null) {
                // thumbnail
                Artwork artwork = tag.getFirstArtwork();
                if (artwork != null) {
                    byte[] artworkData = artwork.getBinaryData();
                    Bitmap thumbnail = BitmapFactory.decodeByteArray(artworkData, 0, artworkData.length);
                    Glide.with(this).load(thumbnail).into(img_song);
                }
            }
        } catch (Exception e) {
            Log.e("SongLoader", "Error reading audio file: " );
        }
    }

    private void saveSettings(){
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("light_sensor", cb_settingLightSensor.isChecked());
        editor.putBoolean("shake_sensor", cb_settingShakeSensor.isChecked());
        editor.commit();
    }

    private void readSettings(){
        Boolean lightSensor = sharedPreferences.getBoolean("light_sensor", false);
        Boolean shakeSensor = sharedPreferences.getBoolean("shake_sensor", false);
        cb_settingLightSensor.setChecked(lightSensor);
        cb_settingShakeSensor.setChecked(shakeSensor);
    }
}