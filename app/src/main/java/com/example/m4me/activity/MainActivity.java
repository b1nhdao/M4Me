package com.example.m4me.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
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
import com.example.m4me.boardcastReceiver.MyReceiver;
import com.example.m4me.fragment.HomeFragment;
import com.example.m4me.fragment.LibraryFragment;
import com.example.m4me.fragment.SearchFragment;
import com.example.m4me.model.Song;
import com.example.m4me.service.MusicService;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottom_nav_view;
    public static FirebaseFirestore db;

    private RelativeLayout layout_bottom;
    private ImageView img_song, img_play_or_pause, img_clear;
    private TextView tv_songTitle, tv_songArtist;

    private Song mSong;
    private boolean isPlaying;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            if(bundle == null){
                return;
            }
            mSong = (Song) bundle.get("object_song");
            isPlaying = (boolean) bundle.getBoolean("status_player");
            int actionMusic = bundle.getInt("action_music");

            handleLayoutMusic(actionMusic);
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

        loadFragment(new HomeFragment());

        handleBottomNavigationBarClick();
    }

    private void initViews(){
        bottom_nav_view = findViewById(R.id.bottom_nav_view);
        layout_bottom = findViewById(R.id.layout_bottom);
        img_song = findViewById(R.id.img_song);
        img_play_or_pause = findViewById(R.id.img_play_or_pause);
        img_clear = findViewById(R.id.img_clear);
        tv_songArtist = findViewById(R.id.tv_songArtist);
        tv_songTitle = findViewById(R.id.tv_songTitle);
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
    }

    private void handleLayoutMusic(int action){
        switch (action){
            case MusicService.ACTION_START:
                layout_bottom.setVisibility(View.VISIBLE);
                showInfoSong();
                setStatusButtonPlayOrPause();
                break;
            case MusicService.ACTION_PAUSE:
                setStatusButtonPlayOrPause();
                break;
            case MusicService.ACTION_RESUME:
                setStatusButtonPlayOrPause();
                break;
            case MusicService.ACTION_CLEAR:
                layout_bottom.setVisibility(View.INVISIBLE);
        }
    }

    private void showInfoSong(){
        if(mSong == null){
            return;
        }
        tv_songTitle.setText(mSong.getTitle());
        tv_songArtist.setText(mSong.getArtistName());
        Glide.with(MainActivity.this).load(mSong.getThumbnailUrl()).into(img_song);

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
    }

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
}