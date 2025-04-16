package com.example.m4me.activity;

import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.m4me.R;
import com.example.m4me.adapter.SongAdapter_Playlist_Vertically;
import com.example.m4me.model.Song;
import com.example.m4me.model.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class FavouriteSongsActivity extends AppCompatActivity {

    private RecyclerView rv_song;
    private SongAdapter_Playlist_Vertically adapter;

    private FirebaseAuth auth = FirebaseAuth.getInstance();
    private FirebaseUser user = auth.getCurrentUser();
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    private List<Song> favouriteSongList = new ArrayList<>();
    private User mUser;
    private List<Song> songList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_favourite_songs);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        rv_song = findViewById(R.id.rv_song);
        something(user.getUid());

        adapter = new SongAdapter_Playlist_Vertically(this, songList, 1);
        rv_song.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        rv_song.setAdapter(adapter);
    }


    List<String> favouriteIDs = new ArrayList<>();
    private void something(String userID){
        db.collection("users").document(userID).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()){
                    mUser = task.getResult().toObject(User.class);
                    favouriteIDs = mUser.getFavouriteSongs();
                    getSongsFromDatabaseByListSongIDs(favouriteIDs);
                }
            }
        });
    }

    private void getSongsFromDatabaseByListSongIDs(List<String> songIDs){
        songList.clear();
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

                        song.setFavourite(favouriteIDs.contains(song.getID()));
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
}