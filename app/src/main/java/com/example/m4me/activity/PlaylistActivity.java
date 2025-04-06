package com.example.m4me.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.m4me.R;
import com.example.m4me.adapter.SongAdapter_Playlist_Vertically;
import com.example.m4me.model.Playlist;
import com.example.m4me.model.Song;
import com.example.m4me.service.MusicService;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class PlaylistActivity extends AppCompatActivity {

    private Playlist playlist;
    private List<Song> songList = new ArrayList<>();

    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    private RecyclerView rv_song;
    private TextView tv_playlistTitle;
    private ImageView img_playlistThumbnail, img_playlistFavourite;
    private Button btn_playNow;

    SongAdapter_Playlist_Vertically adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_playlist);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            playlist = (Playlist) bundle.get("object_playlist");
            tv_playlistTitle.setText(playlist.getTitle());
            Glide.with(this).load(playlist.getThumbnailURL()).into(img_playlistThumbnail);
            adapter = new SongAdapter_Playlist_Vertically(this, songList);
            rv_song.setLayoutManager(new LinearLayoutManager(this));
            rv_song.setAdapter(adapter);
            getSongsFromDatabaseByListSongIDs(playlist.getSongIDs());
        } else {
            Log.e("PlaylistActivity", "Playlist is null");
            finish();
        }

        btn_playNow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickChangeActivity(songList);
            }
        });
    }

    private void initViews() {
        rv_song = findViewById(R.id.rv_song);
        tv_playlistTitle = findViewById(R.id.tv_playlistTitle);
        img_playlistThumbnail = findViewById(R.id.img_playlistThumbnail);
        img_playlistFavourite = findViewById(R.id.img_playlistFavourite);
        btn_playNow = findViewById(R.id.btn_playNow);
    }

    private void clickChangeActivity(List<Song> songList){
        Intent intent = new Intent(PlaylistActivity.this, SongPlayingActivity.class);
        Bundle bundle = new Bundle();
        bundle.putSerializable("list_object_song", (Serializable) songList);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    private void getSongsFromDatabaseByListSongIDs(List<String> songsIDs){
        for (String songID: songsIDs) {
            db.collection("songs").whereEqualTo("ID", songID).get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        Song song = document.toObject(Song.class);

                        DocumentReference artistRef = document.getDocumentReference("Artist");
                        if (artistRef != null) {
                            artistRef.get().addOnSuccessListener(snapshot -> {
                                if (snapshot.exists()) {
                                    String artistName = snapshot.getString("displayName");
                                    song.setArtistName(artistName);
                                }
                                adapter.notifyDataSetChanged(); //  update adapter
                            });
                        } else {
                            adapter.notifyDataSetChanged(); // update adapter
                        }

                        songList.add(song);
                    }
//                    for (Song song: songList) {
//                        Log.w("GetSongs", song.getTitle());
//                    }
                } else {
                    Log.w("GetSongs", "Error getting documents.", task.getException());
                }
            });
        }
    }
}