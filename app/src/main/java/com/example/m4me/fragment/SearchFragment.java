package com.example.m4me.fragment;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.example.m4me.R;
import com.example.m4me.activity.MainActivity;
import com.example.m4me.adapter.SearchItemAdapter_Search_Vertically;
import com.example.m4me.model.Playlist;
import com.example.m4me.model.Song;
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
 * Use the {@link SearchFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SearchFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters

    private EditText edt_keyword;
    private ImageView img_search;
    private ToggleButton tgbtn_song, tgbtn_user, tgbtn_album, tgbtn_playlist;
    private FirebaseFirestore db = MainActivity.db;
    private RecyclerView rv_result;

    private List<Song> songList = new ArrayList<>();
    private List<Playlist> playlistList = new ArrayList<>();

    SearchItemAdapter_Search_Vertically adapter;

    private ToggleButton[] toggleButtons;

    public SearchFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment SearchFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static SearchFragment newInstance(String param1, String param2) {
        SearchFragment fragment = new SearchFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
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
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        edt_keyword = view.findViewById(R.id.edt_keyword);
        img_search = view.findViewById(R.id.img_search);
        tgbtn_album = view.findViewById(R.id.tgbtn_album);
        tgbtn_user = view.findViewById(R.id.tgbtn_user);
        tgbtn_playlist = view.findViewById(R.id.tgbtn_playlist);
        tgbtn_song = view.findViewById(R.id.tgbtn_song);

        rv_result = view.findViewById(R.id.rv_result);

        tgbtn_song.setChecked(true);
        toggleButtons = new ToggleButton[]{tgbtn_song, tgbtn_user, tgbtn_album, tgbtn_playlist};

        tgbtn_song.setChecked(true);

        db = FirebaseFirestore.getInstance();

        rv_result.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false));
//        adapter = new SearchItemAdapter_Search_Vertically(getContext(), playlistList, SearchItemAdapter_Search_Vertically.Type.PLAYLIST);
        rv_result.setAdapter(adapter);

        // thanks chat GPT, seriously
        for (ToggleButton btn : toggleButtons) {
            btn.setOnClickListener(v -> {
                for (ToggleButton otherBtn : toggleButtons) {
                    otherBtn.setChecked(otherBtn == btn);
                }
            });
        }

        img_search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchOnClick(getValueToggleButton(toggleButtons), edt_keyword.getText().toString());
            }
        });

        // legacy code to see how stupid i am, lol

//        inactiveOtherButtons(tgbtn_song, tgbtn_artist, tgbtn_album, tgbtn_playlist);
//        inactiveOtherButtons(tgbtn_artist, tgbtn_song, tgbtn_album, tgbtn_playlist);
//        inactiveOtherButtons(tgbtn_album, tgbtn_artist, tgbtn_song, tgbtn_playlist);
//        inactiveOtherButtons(tgbtn_playlist, tgbtn_artist, tgbtn_album, tgbtn_song);

        return view;
    }

    private void searchOnClick(String action ,String keyword){
        switch (action){
            case "Bài hát":
                getSongsFromDatabaseByKeyword(keyword);
                break;
            case "Playlist":
                getPlaylistsFromDatabaseByKeyword(keyword);
                break;
            case "Album":
                Toast.makeText(getContext(), "Tính năng này chưa (sẽ không) có", Toast.LENGTH_SHORT).show();
                break;
            case "User":
                break;
        }
    }

    private String getValueToggleButton(ToggleButton... toggleButtons) {
        for (ToggleButton toggleButton : toggleButtons) {
            if (toggleButton.isChecked()) {
                return toggleButton.getText().toString();
            }
        }
        return "Maybe something wrong, please just choose a category that you want to search again :D";
    }

    // i know you will laugh at me when i have 4 (actually 3 cuz "Album" doesnt even exist) different functions to fetch data

    private void getPlaylistsFromDatabaseByKeyword(String keyword){
        playlistList.clear();
        String keywordLower = keyword.toLowerCase();

        db.collection("playlists").addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                if (error != null){
                    Log.d("getSongsByKeyword", "failed: ", error);
                }
                if (value != null){
                    for (QueryDocumentSnapshot document : value) {
                        Playlist playlist = document.toObject(Playlist.class);
                        String playlistTitleLower = playlist.getTitle().toLowerCase();

//                        Log.w("Listen playlist", "data: " + playlist.getTitle());

                        if (playlistTitleLower.contains(keywordLower)){
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
//                                            Log.d("GetTag", "getSongsFromDatabase: " + tagName);
                                        }

                                        // Check if all tag requests are completed
                                        if (pendingTags.decrementAndGet() == 0) {
                                            playlist.setTagNames(tagNames);
                                            adapter.notifyDataSetChanged();
                                        }
                                    }).addOnFailureListener(e -> {
                                        Log.e("GetTags", "Error getting tag: ", e);
                                        if (pendingTags.decrementAndGet() == 0) {
                                            playlist.setTagNames(tagNames);
                                            adapter.notifyDataSetChanged();
                                        }
                                    });
                                } // loop getting tags
                            } // check if tagRef null
                            else {
                                playlist.setTagNames(new ArrayList<>());
                                adapter.notifyDataSetChanged();
                            }
                            playlistList.add(playlist);
                        } // check if contain keyword
                    } // loop document
                    adapter = new SearchItemAdapter_Search_Vertically(getContext(), playlistList, SearchItemAdapter_Search_Vertically.Type.PLAYLIST);
                    rv_result.setAdapter(adapter);
                } // if value != null
                else{
                    Log.w("GetPlaylists", "Error getting documents. null");
                }
            }
        });
    }

    private void getSongsFromDatabaseByKeyword(String keyword){
        songList.clear();
        String keywordLower = keyword.toLowerCase();

        db.collection("songs").addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                if (error != null){
                    Log.d("getSongsByKeyword", "failed: ", error);
                }
                if (value != null){
                    for (QueryDocumentSnapshot document : value) {
                        Song song = document.toObject(Song.class);
                        String playlistTitleLower = song.getTitle().toLowerCase();

//                        Log.w("Listen playlist", "data: " + playlist.getTitle());

                        if (playlistTitleLower.contains(keywordLower)){
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
//                                            Log.d("GetTag", "getSongsFromDatabase: " + tagName);
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
                                } // loop getting tags
                            } // check if tagRef null
                            else {
                                song.setTagNames(new ArrayList<>());
                                adapter.notifyDataSetChanged();
                            }
                            songList.add(song);
                        } // check if contain keyword
                    } // loop document
                    adapter = new SearchItemAdapter_Search_Vertically(getContext(), songList, SearchItemAdapter_Search_Vertically.Type.SONG);
                    rv_result.setAdapter(adapter);
                } // if value != null
                else{
                    Log.w("GetPlaylists", "Error getting documents. null");
                }
            }
        });
    }

    // FIXME: please make this function work and make the code cleaner
//    private void getDataFromDatabaseByKeyword(String collection, String keyword){
//
//    }

    // legacy code to see how stupid i am

//    public void inactiveOtherButtons(ToggleButton toggleButton, ToggleButton toggleButton1, ToggleButton toggleButton2, ToggleButton toggleButton3) {
//        toggleButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                toggleButton1.setChecked(false);
//                toggleButton2.setChecked(false);
//                toggleButton3.setChecked(false);
//            }
//        });
//    }

//    public String getValueToggleButton(ToggleButton toggleButton, ToggleButton toggleButton1, ToggleButton toggleButton2, ToggleButton toggleButton3) {
//        if (toggleButton.isChecked()) {
//            return toggleButton.getText().toString();
//        }
//        if (toggleButton1.isChecked()) {
//            return toggleButton1.getText().toString();
//        }
//        if (toggleButton2.isChecked()) {
//            return toggleButton2.getText().toString();
//        }
//        if (toggleButton3.isChecked()) {
//            return toggleButton3.getText().toString();
//        } else
//            return "Maybe something wrong, please just choose a category that you want to search again :D";
//    }
}