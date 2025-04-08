package com.example.m4me.fragment;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.example.m4me.R;
import com.example.m4me.activity.MainActivity;
import com.example.m4me.adapter.SongAdapter_Home_Horizontally;
import com.example.m4me.model.Song;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;


public class SearchFragment extends Fragment {
    private EditText edtSearchSong;
    private RecyclerView rvSearchResults;
    private List<Song> songList = new ArrayList<>();
    private SongAdapter_Home_Horizontally adapter;
    private FirebaseFirestore db = MainActivity.db;
    public SearchFragment() {
        // Required empty public constructor
    }

    public static SearchFragment newInstance(String param1, String param2) {
        SearchFragment fragment = new SearchFragment();
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
        View view= inflater.inflate(R.layout.fragment_search, container, false);

        edtSearchSong = view.findViewById(R.id.edtSearchSong);
        rvSearchResults = view.findViewById(R.id.rvSearchResults);
        rvSearchResults.setLayoutManager(new LinearLayoutManager(getContext(),LinearLayoutManager.VERTICAL,false));
        adapter = new SongAdapter_Home_Horizontally(getContext(), songList);
        rvSearchResults.setAdapter(adapter);
        edtSearchSong.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchSongsByTitle(s.toString().trim());
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        return view;
    }
    private void searchSongsByTitle(String keyword) {
        if (keyword.isEmpty()) {
            songList.clear();
            adapter.notifyDataSetChanged();
            return;
        }
        db.collection("songs")
                .orderBy("Title")
                .startAt(keyword)
                .endAt(keyword + "\uf8ff")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    songList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Song song = doc.toObject(Song.class);
                        songList.add(song);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Log.e("SearchSong", "Lỗi tìm bài hát", e));
    }
}