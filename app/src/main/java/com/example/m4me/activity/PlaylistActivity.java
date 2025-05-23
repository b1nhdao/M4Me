package com.example.m4me.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.m4me.R;
import com.example.m4me.adapter.SongPlaylistAdapter;
import com.example.m4me.model.Playlist;
import com.example.m4me.model.Song;
import com.example.m4me.model.User;
import com.example.m4me.service.MusicService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;

import java.io.File;
import java.io.FilenameFilter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class PlaylistActivity extends AppCompatActivity {

    private Playlist playlist;
    private List<Song> songList = new ArrayList<>();

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private FirebaseUser user;

    private RecyclerView rv_song;
    private TextView tv_playlistTitle;
    private ImageView img_playlistThumbnail, img_playlistFavourite;
    private Button btn_playNow;

    private Set<String> userFavoriteSongIDs = new HashSet<>();

    private User mUser = new User();

    private SongPlaylistAdapter adapter;

    private int specialCode;

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
            if (bundle.containsKey("object_playlist")){
                playlist = (Playlist) bundle.get("object_playlist");
                specialCode = bundle.getInt("manager_code", 0);

                tv_playlistTitle.setText(playlist.getTitle());

                if(playlist.getThumbnailURL() != null){
                    Glide.with(this).load(playlist.getThumbnailURL()).into(img_playlistThumbnail);
                }

                adapter = new SongPlaylistAdapter(this, songList, 1, specialCode, playlist.getID());
                user = mAuth.getCurrentUser();

                getPlaylistFromDatabaseByPlaylistID(playlist.getID());
                getUserFavoriteSongIDs();
            }
            else if (bundle.containsKey("object_offline_playlist"))
            {
                songList = (List<Song>) bundle.get("object_offline_playlist");
                tv_playlistTitle.setText("cac bai hat da tai");

                songList = getDownloadedSongs();
                adapter = new SongPlaylistAdapter(this, songList, 3);
            }

            rv_song.setLayoutManager(new LinearLayoutManager(this));
            rv_song.setAdapter(adapter);



        } else {
            Log.e("PlaylistActivity", "Playlist is null");
            finish();
        }

        btn_playNow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickChangeActivity(songList);
                startMusicService(songList);
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
        bundle.putSerializable("object_song", songList.get(0));
        intent.putExtras(bundle);
        startActivity(intent);
    }

    private void startMusicService(List<Song> songlist){
        Intent intent = new Intent(PlaylistActivity.this, MusicService.class);
        Bundle bundle = new Bundle();
        bundle.putSerializable("list_object_song", (Serializable) songList);
//        bundle.putString("key_test", "alicia meu");
        intent.putExtras(bundle);
        startService(intent);
    }

    private void getPlaylistFromDatabaseByPlaylistID(String playlistID){
        db.collection("playlists").whereEqualTo("ID", playlistID).addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                if (error != null){
                    Log.w("playlist listener", "failed: ", error);
                }
                if(value != null){
                    for (QueryDocumentSnapshot document : value){
                        playlist = document.toObject(Playlist.class);
                    }
                }
                else {
                    Log.w("current data playlist", "null");
                }
                getSongsFromDatabaseByListSongIDs(playlist.getSongIDs());
            }
        });
    }

    private void getSongsFromDatabaseByListSongIDs(List<String> songIDs){
        songList.clear();

        if (songIDs == null || songIDs.isEmpty()) {
            adapter.notifyDataSetChanged();
            Log.d("GetSongs", "Song ID list is empty. No query executed.");
            return;
        }

        db.collection("songs").whereIn("ID", songIDs).addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                if (error != null) {
                    Log.w("GetSongs", "Listen failed.", error);
                    return;
                }

                if (value != null && !value.isEmpty()) {
                    for (QueryDocumentSnapshot document : value) {
                        Song song = document.toObject(Song.class);

                        song.setFavourite(userFavoriteSongIDs.contains(song.getID()));
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

                        List<DocumentReference> tagRefs = (List<DocumentReference>) document.get("Tags");
                        if (tagRefs != null && !tagRefs.isEmpty()) {
                            List<String> tagNames = new ArrayList<>();
                            AtomicInteger pendingTags = new AtomicInteger(tagRefs.size());

                            for (DocumentReference tagReference : tagRefs) {
                                tagReference.get().addOnSuccessListener(tagSnapshot -> {
                                    if (tagSnapshot.exists()) {
                                        String tagName = tagSnapshot.getString("Name");
                                        if (tagName != null) {
                                            tagNames.add(tagName);
                                        }
                                    }

                                    // Check if all tag requests are completed
                                    if (pendingTags.decrementAndGet() == 0) {
                                        song.setTagNames(tagNames);
                                        adapter.notifyDataSetChanged();
                                    }
                                }).addOnFailureListener(e -> {
                                    Log.e("GetTags", "Error getting tag: ", e);
                                    if (pendingTags.decrementAndGet() == 0) {
                                        song.setTagNames(tagNames);
                                        adapter.notifyDataSetChanged();
                                    }
                                });
                            }
                        }
                        else {
                            song.setTagNames(new ArrayList<>());
                        }
                        songList.add(song);
                    }
                } else {
                    Log.d("GetSongs", "Current data: null");
                }
            }
        });
    }

    private void getUserFavoriteSongIDs() {
        if (user == null || user.getEmail() == null) return;

        db.collection("users").whereEqualTo("email", user.getEmail())
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.w("GetFavorites", "Listen failed.", error);
                        return;
                    }

                    userFavoriteSongIDs.clear();

                    if (value != null && !value.isEmpty()) {
                        for (QueryDocumentSnapshot document : value) {
                            mUser = document.toObject(User.class);
                            List<String> favoriteIDs = mUser.getFavouriteSongs();
                            if (favoriteIDs != null) {
                                userFavoriteSongIDs.addAll(favoriteIDs);
                            }
                            break;
                        }

                        updateSongFavoriteStatus();
                    }
                });
    }

    private void updateSongFavoriteStatus() {
        for (Song song : songList) {
            song.setFavourite(userFavoriteSongIDs.contains(song.getID()));
        }
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private List<Song> getDownloadedSongs(){
        List<Song> downloadedSongList = new ArrayList<>();
        File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

        File[] files = downloadDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".mp3");
            }
        });

        if (files != null) {
            for (File file : files) {
                try {
                    //  jAudiotagger to read metadata
                    AudioFile audioFile = AudioFileIO.read(file);
                    Tag tag = audioFile.getTag();

                    if (tag != null) {
                        Song song = new Song();

                        song.setTitle(tag.getFirst(FieldKey.TITLE));
                        song.setArtistName(tag.getFirst(FieldKey.ARTIST));
                        song.setFilePath(file.getAbsolutePath());

                        downloadedSongList.add(song);
                    }
                } catch (Exception e) {
                    Log.e("SongLoader", "Error reading audio file: " + file.getName(), e);

                    Song song = new Song();
                    song.setTitle(file.getName());
                    song.setFilePath(file.getAbsolutePath());
                    downloadedSongList.add(song);
                }
            }
        }
        return downloadedSongList;
    }
}