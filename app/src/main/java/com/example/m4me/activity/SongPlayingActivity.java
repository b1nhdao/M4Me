package com.example.m4me.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.m4me.R;
import com.example.m4me.model.Song;
import com.example.m4me.service.MusicService;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class SongPlayingActivity extends AppCompatActivity {

    List<Song> songList = new ArrayList<>();
    TextView tv_songArtist, tv_songTitle;
    ImageView img_songThumbnail;

    ImageView img_forward, img_play_or_pause, img_skip, img_favourite, img_comment, img_loop, img_options;
    SeekBar seekBar;

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

        Bundle bundle = getIntent().getExtras();
        if(bundle != null){
            songList = (List<Song>) bundle.get("list_object_song");
            updateUI(songList.get(0));
            startMusicService(songList);
        }
        else{
            Log.d("SongList", "No song found");
        }

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
    }

    private void startMusicService(List<Song> songlist){
        Intent intent = new Intent(SongPlayingActivity.this, MusicService.class);
        Bundle bundle = new Bundle();
        bundle.putSerializable("list_object_song", (Serializable) songList);
        intent.putExtras(bundle);
        ContextCompat.startForegroundService(SongPlayingActivity.this, intent);
    }

    private void sendActionToService(int action){
        Intent intent = new Intent(this, MusicService.class);
        intent.putExtra("action_music_service", action);
        startService(intent);
    }

    private void updateUI(Song song){
        tv_songArtist.setText(song.getArtistName());
        tv_songTitle.setText(song.getTitle());
        Glide.with(this).load(song.getThumbnailUrl()).into(img_songThumbnail);
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
    }
}