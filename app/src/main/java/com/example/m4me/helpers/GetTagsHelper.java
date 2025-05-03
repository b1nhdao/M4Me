package com.example.m4me.helpers;

import android.content.Context;
import android.util.Log;

import com.example.m4me.adapter.ItemAdapter;
import com.example.m4me.model.Song;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class GetTagsHelper {
    Context context;
    FirebaseFirestore db;

    public GetTagsHelper(Context context, FirebaseFirestore db) {
        this.context = context;
        this.db = db;
    }


    public List<DocumentReference> getTagRefs(QueryDocumentSnapshot document, Consumer<List<String>> setTagNamesCallback, ItemAdapter adapter){
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
                        setTagNamesCallback.accept(tagNames);
                        adapter.notifyDataSetChanged();
                    }
                }).addOnFailureListener(e -> {
                    Log.e("GetTags", "Error getting tag: ", e);
                    if (pendingTags.decrementAndGet() == 0) {
                        setTagNamesCallback.accept(tagNames);
                        adapter.notifyDataSetChanged();
                    }
                });
            } // loop getting tags
        } // check if tagRef null
        else {
            setTagNamesCallback.accept(new ArrayList<>());
        }
        return tagRefs;
    }

    public DocumentReference getArtistRefs(QueryDocumentSnapshot document, Song song, ItemAdapter adapter){
        DocumentReference artistRef = document.getDocumentReference("Artist");
        if (artistRef != null){
            artistRef.get().addOnSuccessListener(snapshot -> {
                if(snapshot.exists()){
                    String artistName = snapshot.getString("displayName");
                    song.setArtistName(artistName);
                    adapter.notifyDataSetChanged();
                }
            });
        }
        return artistRef;
    }
}
