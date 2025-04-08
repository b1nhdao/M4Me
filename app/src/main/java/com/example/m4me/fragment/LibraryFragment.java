package com.example.m4me.fragment;

import android.app.AlertDialog;
import android.os.Bundle;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.example.m4me.R;
import com.example.m4me.activity.MainActivity;
import com.example.m4me.adapter.PlaylistAdapter_Home_Horizontally;
import com.example.m4me.model.Playlist;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class LibraryFragment extends Fragment {
    private ConstraintLayout layoutThemPlaylist;
    private RecyclerView rvPlaylist;
    private PlaylistAdapter_Home_Horizontally adapter;
    private List<Playlist> playlists = new ArrayList<>();
    private FirebaseFirestore db = MainActivity.db;

    public LibraryFragment() {
        // Required empty public constructor
    }

    public static LibraryFragment newInstance(String param1, String param2) {
        LibraryFragment fragment = new LibraryFragment();
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
        View view = inflater.inflate(R.layout.fragment_library, container, false);
        layoutThemPlaylist = view.findViewById(R.id.layoutThemPlayList);
        rvPlaylist = view.findViewById(R.id.rv_playList);
        rvPlaylist.setLayoutManager(new GridLayoutManager(getContext(),2,GridLayoutManager.VERTICAL,false));
        adapter = new PlaylistAdapter_Home_Horizontally(getContext(),playlists);
        rvPlaylist.setAdapter(adapter);
        getPlaylistsFromDatabase();
        layoutThemPlaylist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialogAddPlaylist();
            }
        });
        return  view;

    }
    private void getPlaylistsFromDatabase() {
        db.collection("playlists")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Playlist playlist = document.toObject(Playlist.class);
                            DocumentReference tagRef = document.getDocumentReference("Tag");
                            if (tagRef != null){
                                tagRef.get().addOnSuccessListener(snapshot -> {
                                    if(snapshot.exists()){
                                        String tagName = snapshot.getString("Name");
                                        playlist.setTagName(tagName);
                                    }
                                    adapter.notifyDataSetChanged();
                                });
                            } else {
                                adapter.notifyDataSetChanged();
                            }

                            playlists.add(playlist);
                        }
                    } else {
                        Log.w("GetPlaylistsLibrary", "Error getting documents.", task.getException());
                    }
                });
    }
    private void showDialogAddPlaylist() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_play_list, null);
        EditText txt = dialogView.findViewById(R.id.edtInputTenPlaylist);

        builder.setView(dialogView)
                .setPositiveButton("Tạo", null)
                .setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        // Override button để kiểm tra input trước khi đóng dialog
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String tenPlaylist = txt.getText().toString().trim();
            if (tenPlaylist.isEmpty()) {
                txt.setError("Vui lòng nhập tên playlist");
                return;
            }

            // Tạo ID mới
            DocumentReference newPlaylistRef = db.collection("playlists").document();
            String id = newPlaylistRef.getId();

            // Tạo object Playlist mới
            Playlist newPlaylist = new Playlist();
            newPlaylist.setID(id);
            newPlaylist.setTitle(tenPlaylist);
            newPlaylist.setThumbnailURL("https://thantrieu.com/resources/arts/1121429554.webp");
            newPlaylist.setSongIDs(new ArrayList<>());
            newPlaylist.setTagName("");
            newPlaylistRef.set(newPlaylist)
                    .addOnSuccessListener(unused -> {
                        playlists.add(newPlaylist);
                        adapter.notifyItemInserted(playlists.size() - 1);
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> {
                        Log.e("AddPlaylist", "Lỗi khi thêm playlist", e);
                    });
        });
    }


}