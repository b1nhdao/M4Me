package com.example.m4me.helpers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.m4me.adapter.ItemAdapter;
import com.example.m4me.model.Playlist;
import com.example.m4me.model.Song;
import com.example.m4me.model.User;
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

public class SearchHelper {

    Context context;
    FirebaseFirestore db;
    GetTagsHelper tagsHelper = new GetTagsHelper(context, db);

    public SearchHelper(Context context, FirebaseFirestore db) {
        this.context = context;
        this.db  = db;
    }

    public void getPlaylistsByKeyword(String keyword, List<Playlist> playlistList, ItemAdapter adapter){
        String keywordLower = keyword.toLowerCase();
        db.collection("playlists").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if(task.isSuccessful()){
                    playlistList.clear();
                    for (QueryDocumentSnapshot document : task.getResult()){
                        Playlist playlist = document.toObject(Playlist.class);
                        String playlistTitleLower = playlist.getTitle().toLowerCase();

                        if (playlistTitleLower.contains(keywordLower)){
                            DocumentReference artistRef = document.getDocumentReference("Creator");
                            if (artistRef != null){
                                artistRef.get().addOnSuccessListener(snapshot -> {
                                    if(snapshot.exists()){
                                        String creator = snapshot.getString("displayName");
                                        playlist.setCreatorName(creator);
                                    }
                                    adapter.notifyDataSetChanged();
                                });
                            }

                            tagsHelper.getTagRefs(document, playlist::setTagNames, adapter);
                            playlistList.add(playlist);
                            if (playlistList.size() == task.getResult().size()){
                                adapter.notifyDataSetChanged();
                            }
                        } // check if contain keyword

                    } // end for loop getting document in value
                } // end check if task
            }
        });
    }

    public void getSongsByKeyword(String keyword, List<Song> songList, ItemAdapter adapter){
        String keywordLower = keyword.toLowerCase();
        db.collection("songs").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()){
                    songList.clear();
                    for (QueryDocumentSnapshot document : task.getResult()){
                        Song song = document.toObject(Song.class);
                        String songTitleToLower = song.getTitle().toLowerCase();

                        if (songTitleToLower.contains(keywordLower)){
                            tagsHelper.getArtistRefs(document, song, adapter);

                            //tag ref
                            tagsHelper.getTagRefs(document, song::setTagNames, adapter);
                            songList.add(song);
                            if(songList.size() == task.getResult().size()) {
                                adapter.notifyDataSetChanged();
                            }
                        } // check if contain keyword
                    } // end for loop getting doc in value
                }
            }
        });
    }

    public void getUsersByKeyword(String keyword, List<User> userList, ItemAdapter adapter){
        userList.clear();
        String keywordLower = keyword.toLowerCase();

        db.collection("users").addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                if (error != null) {
                    Log.w("getUser", "error: ", error);
                    return;
                }

                if (value != null) {
                    for (QueryDocumentSnapshot document : value) {
                        User user = document.toObject(User.class);

                        String userDisplayLower = user.getDisplayName().toLowerCase();

                        if (userDisplayLower.contains(keywordLower)) {
                            userList.add(user);
                        }
                    }

                    adapter.notifyDataSetChanged();
                } else {
                    Log.w("GetUserList", "Error getting documents. null");
                }
            }
        });
    }

    public void getSongsByTagTitle(String tagTitle, List<Song> songList, ItemAdapter adapter) {
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
                                querySongsWithMultipleTags(TagRefs, songList, adapter);
                                adapter.notifyDataSetChanged();
                            }

                        } else {
                            Log.w("TagSongs", "Error getting tags", task.getException());
                        }
                    }
                });
    }

    // once again, thanks chatGPT (it sucks)
    private void querySongsWithMultipleTags(List<DocumentReference> tagRefs, List<Song> songList, ItemAdapter adapter) {
        db.collection("songs").whereArrayContainsAny("Tags", tagRefs).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    // Process each song document
                    songList.clear();

                    for (QueryDocumentSnapshot document : task.getResult()) {
                        Song song = document.toObject(Song.class);

                        // Get the tag references for this song
                        tagsHelper.getTagRefs(document, song::setTagNames, adapter);
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

}
