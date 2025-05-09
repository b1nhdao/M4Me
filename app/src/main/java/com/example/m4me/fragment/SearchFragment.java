package com.example.m4me.fragment;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ToggleButton;

import com.example.m4me.R;
import com.example.m4me.activity.MainActivity;
import com.example.m4me.adapter.ItemAdapter;
import com.example.m4me.helpers.SearchHelper;
import com.example.m4me.model.Playlist;
import com.example.m4me.model.Song;
import com.example.m4me.model.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

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
    private List<User> userList = new ArrayList<>();

    ItemAdapter adapter;

    private ToggleButton[] toggleButtons;

    private SearchHelper searchHelper = new SearchHelper(getContext(), db);

    public SearchFragment() {
        // Required empty public constructor
    }

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
                adapter = new ItemAdapter(getContext(), songList, ItemAdapter.Type.SONG);
                rv_result.setAdapter(adapter);
                searchHelper.getSongsByKeyword(keyword, songList, adapter);
                break;
            case "Playlist":
                adapter = new ItemAdapter(getContext(), playlistList, ItemAdapter.Type.PLAYLIST);
                rv_result.setAdapter(adapter);
                searchHelper.getPlaylistsByKeyword(keyword, playlistList, adapter);
                break;
            case "Thể loại":
                adapter = new ItemAdapter(getContext(), songList, ItemAdapter.Type.SONG);
                rv_result.setAdapter(adapter);
                getSongsFromDatabaseByTagTitle(keyword);
                break;
            case "User":
                adapter = new ItemAdapter(getContext(), userList, ItemAdapter.Type.USER);
                rv_result.setAdapter(adapter);
                searchHelper.getUsersByKeyword(keyword, userList, adapter);
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

//    public void getSongsFromDatabaseByTagID(String tagID){
//        DocumentReference tagRef = db.collection("tags").document(tagID);
//        Log.d("tagQuery", "getSongsFromDatabaseByTagID: " + tagRef.getPath());
//
//        db.collection("songs").whereArrayContains("Tags", tagRef).addSnapshotListener(new EventListener<QuerySnapshot>() {
//            @Override
//            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
//                   if (value != null){
//                       songList.clear();
//
//                       for (QueryDocumentSnapshot document : value){
//                           Song song = document.toObject(Song.class);
//                           songList.add(song);
//                           Log.d("TagSongs", "Found song: " + song.getTitle());
//
//                           List<DocumentReference> tags = (List<DocumentReference>) document.get("Tags");
//                           Log.d("TagQuery", "Song " + document.getId() + " has " + tags.size() + " tags:");
//                           for (DocumentReference ref : tags) {
//                               Log.d("TagQuery", "  - Tag: " + ref.getPath());
//                           }
//                       } // end for loop in value
//                       Log.d("TagSongs", "Total songs found: " + songList.size());
//                       adapter.notifyDataSetChanged();
//                   } // end check value not null
//                else {
//                       Log.w("TagSongs", "Error getting songs: ", error);
//                   }
//            } // end snapshot listener
//        });
//    }

    private void getSongsFromDatabaseByTagTitle(String tagTitle) {
        String keywordLower = tagTitle.toLowerCase();

        db.collection("tags")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            songList.clear();
                            List<DocumentReference> TagRefs = new ArrayList<>();

                            // find all tags that contain the keyword
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String tagName = document.getString("Name");
                                if (tagName != null && tagName.toLowerCase().contains(keywordLower)) {
                                    TagRefs.add(document.getReference());
                                    Log.d("TagSong", "Found matching tag: " + tagName);
                                }
                            }

                            if (TagRefs.isEmpty()) {
                                Log.d("TagSongs", "No matching tags found");
                                adapter.notifyDataSetChanged();
                            }
                            else {
                                querySongsWithMultipleTags(TagRefs);
                                adapter.notifyDataSetChanged();
                            }

                        } else {
                            Log.w("TagSongs", "Error getting tags", task.getException());
                        }
                    }
                });
    }

    // once again, thanks chatGPT (it sucks)
    private void querySongsWithMultipleTags(List<DocumentReference> tagRefs) {
        db.collection("songs")
                .whereArrayContainsAny("Tags", tagRefs)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            // Process each song document
                            songList.clear();

                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Song song = document.toObject(Song.class);

                                // Get the tag references for this song
                                List<DocumentReference> songTagRefs = (List<DocumentReference>) document.get("Tags");

                                // Create a counter to track how many tag names we've retrieved
                                AtomicInteger pendingTags = new AtomicInteger(songTagRefs.size());

                                List<String> tagNames = new ArrayList<>();

//                                 For each tag reference, get the tag name
                                for (DocumentReference tagRef : songTagRefs) {
                                    tagRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                        @Override
                                        public void onSuccess(DocumentSnapshot tagDoc) {
                                            if (tagDoc.exists()) {
                                                // Get the tag name from the document
                                                String tagName = tagDoc.getString("Name");
                                                if (tagName != null) {
                                                    tagNames.add(tagName);
                                                }
                                            }

                                            // Decrement the counter of pending tags
                                            if (pendingTags.decrementAndGet() == 0) {
                                                // All tag names have been retrieved
                                                // Set the tag names in the song object
                                                song.setTagNames(tagNames);

                                                // Add the song to the list
                                                songList.add(song);
                                                Log.d("TagSongs", "Added song: " + song.getTitle() +
                                                        " with tags: " + String.join(", ", tagNames));

                                                // If this was the last song, update the UI
                                                if (songList.size() == task.getResult().size()) {
                                                    Log.d("TagSongs", "Total songs found: " + songList.size());
                                                    adapter.notifyDataSetChanged();
                                                }
                                            }
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Log.w("TagSongs", "Error getting tag document", e);

                                            // Even if we fail, decrement the counter
                                            if (pendingTags.decrementAndGet() == 0) {
                                                // All tag attempts have completed
                                                song.setTagNames(tagNames);
                                                songList.add(song);

                                                if (songList.size() == task.getResult().size()) {
                                                    adapter.notifyDataSetChanged();
                                                }
                                            }
                                        }
                                    });
                                }
                            }

                            // Handle the case where there are no songs or no tags in songs
                            if (task.getResult().isEmpty()) {
                                Log.d("TagSongs", "No songs found with the matching tags");
                                adapter.notifyDataSetChanged();
                            }
                        } else {
                            Log.w("TagSongs", "Error getting songs", task.getException());
                        }
                    }
                });
    }

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