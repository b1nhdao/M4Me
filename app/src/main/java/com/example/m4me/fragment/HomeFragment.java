package com.example.m4me.fragment;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.example.m4me.activity.MainActivity;
import com.example.m4me.R;
import com.example.m4me.adapter.PlaylistAdapter_Home_Horizontally;
import com.example.m4me.adapter.SongAdapter_Home_Horizontally;
import com.example.m4me.model.Playlist;
import com.example.m4me.model.Song;
import com.example.m4me.service.MusicService;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link HomeFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HomeFragment extends Fragment {

    private String mParam1;
    private String mParam2;
    private RecyclerView rv_song, rv_playlist_1;
    private FirebaseFirestore db = MainActivity.db;
    private List<Song> songList = new ArrayList<>();
    private List<Playlist> playlistList = new ArrayList<>();
    Button btn_testService, btn_testServiceStop;

    SongAdapter_Home_Horizontally adapterSong;
    PlaylistAdapter_Home_Horizontally adapterPlaylist1;

    public HomeFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment HomeFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static HomeFragment newInstance(String param1, String param2) {
        HomeFragment fragment = new HomeFragment();
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
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        db = FirebaseFirestore.getInstance();

        rv_song = view.findViewById(R.id.rv_song);
        rv_playlist_1 = view.findViewById(R.id.rv_playlist_1);
        btn_testService = view.findViewById(R.id.btn_testService);
        btn_testServiceStop = view.findViewById(R.id.btn_testServiceStop);

        rv_song.setLayoutManager(new GridLayoutManager(getContext(),3, RecyclerView.HORIZONTAL, false));
        adapterSong = new SongAdapter_Home_Horizontally(getContext(), songList);
        rv_song.setAdapter(adapterSong);
        getSongsFromDatabase();

        rv_playlist_1.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        adapterPlaylist1 = new PlaylistAdapter_Home_Horizontally(getContext(), playlistList);
        rv_playlist_1.setAdapter(adapterPlaylist1);
        getPlaylistsFromDatabase();

        return view;
    }

    private void getPlaylistsFromDatabase() {
        db.collection("playlists").limit(6).addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                if (error != null){
                    Log.w("Listen", "listen failed: " + error);
                }
                if (value != null){
                    playlistList.clear();
                    for (QueryDocumentSnapshot document : value) {
                        Playlist playlist = document.toObject(Playlist.class);
                        Log.w("Listen playlist", "data: " + playlist.getSongIDs());

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
                                        playlist.setTagNames(tagNames);
                                        adapterPlaylist1.notifyDataSetChanged();
                                    }
                                }).addOnFailureListener(e -> {
                                    Log.e("GetTags", "Error getting tag: ", e);
                                    if (pendingTags.decrementAndGet() == 0) {
                                        playlist.setTagNames(tagNames);
                                        adapterPlaylist1.notifyDataSetChanged();
                                    }
                                });
                            }
                        }
                        else {
                            playlist.setTagNames(new ArrayList<>());
                        }
                        playlistList.add(playlist);
                    }
                }
                else{
                    Log.w("GetPlaylists", "Error getting documents. null");
                }
            }
        });
    }

    private void getSongsFromDatabase() {
        db.collection("songs")
                .limit(6)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Song song = document.toObject(Song.class);

                            // Handle Artist reference
                            DocumentReference artistRef = document.getDocumentReference("Artist");
                            if (artistRef != null){
                                artistRef.get().addOnSuccessListener(snapshot -> {
                                    if(snapshot.exists()){
                                        String artistName = snapshot.getString("displayName");
                                        song.setArtistName(artistName);
                                    }
                                    adapterSong.notifyDataSetChanged();
                                });
                            }

                            // get tags references
                            List<DocumentReference> tagRefs = (List<DocumentReference>) document.get("Tags");
                            if (tagRefs != null && !tagRefs.isEmpty()) {
                                List<String> tagNames = new ArrayList<>();
                                AtomicInteger pendingTags = new AtomicInteger(tagRefs.size());

                                for (DocumentReference tagRef : tagRefs) {
                                    tagRef.get().addOnSuccessListener(tagSnapshot -> {
                                        if (tagSnapshot.exists()) {
                                            String tagName = tagSnapshot.getString("Name");
                                            if (tagName != null) {
                                                tagNames.add(tagName);
                                            }
//                                            Log.d("GetTag", "getSongsFromDatabase: " + tagName);
                                        }

                                        // Check if all tag requests are completed
                                        if (pendingTags.decrementAndGet() == 0) {
                                            song.setTagNames(tagNames);
                                            adapterSong.notifyDataSetChanged();
                                        }
                                    }).addOnFailureListener(e -> {
                                        Log.e("GetTags", "Error getting tag: ", e);
                                        if (pendingTags.decrementAndGet() == 0) {
                                            song.setTagNames(tagNames);
                                            adapterSong.notifyDataSetChanged();
                                        }
                                    });
                                }
                            } else {
                                song.setTagNames(new ArrayList<>());
                            }
//                            Log.d("GetSong", "getSongsFromDatabase: ", song.getTagNames()));
                            songList.add(song);
                        }
                    } else {
                        Log.w("GetSongs", "Error getting documents.", task.getException());
                    }
                });
    }
}