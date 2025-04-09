package com.example.m4me.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.m4me.R;
import com.example.m4me.activity.LoginActivity;
import com.example.m4me.activity.MainActivity;
import com.example.m4me.adapter.SongAdapter_Playlist_Vertically;
import com.example.m4me.model.Song;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class LikedSongFragment extends Fragment {
    private RecyclerView rvLikedSong;
    private FirebaseAuth auth = FirebaseAuth.getInstance();
    private FirebaseFirestore db = MainActivity.db;
    private SongAdapter_Playlist_Vertically adapter;
    private List<Song> listLikedSong = new ArrayList<>();
    public LikedSongFragment() {
        // Required empty public constructor
    }

    public static LikedSongFragment newInstance(String param1, String param2) {
        LikedSongFragment fragment = new LikedSongFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_liked_song, container, false);
        rvLikedSong = view.findViewById(R.id.rvLikedSong);
        loadLikedSong();
        rvLikedSong.setLayoutManager(new GridLayoutManager(getContext(),1,GridLayoutManager.VERTICAL,false));
        adapter = new SongAdapter_Playlist_Vertically(getContext(),listLikedSong);
        rvLikedSong.setAdapter(adapter);

        return  view;

    }

    private void loadLikedSong(){
        String userId = auth.getCurrentUser().getUid();
        Log.e("LikedSong",userId);
        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<DocumentReference> songRefs = (List<DocumentReference>) documentSnapshot.get("favouriteSongs");
                        if (songRefs != null) {
                            fetchSongs(songRefs);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("LikedSong",e.getMessage());
                });
    }

    private void fetchSongs(List<DocumentReference> songRefs) {
        List<Task<DocumentSnapshot>> tasks = new ArrayList<>();
        for (DocumentReference songRef : songRefs) {
            tasks.add(songRef.get());
        }
        Tasks.whenAllSuccess(tasks).addOnSuccessListener(objects -> {
            for (Object obj : objects) {
                DocumentSnapshot songSnapshot = (DocumentSnapshot) obj;
                Log.e("LikedSong",obj.toString());
                if (songSnapshot.exists()) {
                    if (songSnapshot.exists()) {
                        Song song = songSnapshot.toObject(Song.class);
                        if (song != null) {
                            listLikedSong.add(song);
                            adapter.notifyDataSetChanged();
                        }
                    }

                }
            }

        }).addOnFailureListener(e -> {
            Log.e("LikedSong",e.getMessage());
        });
    }

}