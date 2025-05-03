package com.example.m4me.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.m4me.R;
import com.example.m4me.adapter.ItemAdapter;
import com.example.m4me.model.Playlist;
import com.example.m4me.model.Tag;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class PlaylistManagerActivity extends AppCompatActivity {

    private FirebaseAuth auth = FirebaseAuth.getInstance();
    private FirebaseUser user = auth.getCurrentUser();

    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    private ImageView img_addPlaylist;
    private RecyclerView rv_playlist;

    private List<Playlist> playlistCreatedList = new ArrayList<>();

    private ItemAdapter adapter;

    private Dialog dialog;

    private List<Tag> tagList = new ArrayList<>();
    private List<Tag> selectedTags = new ArrayList<>();

    private Uri imageUri = null;

    private String thumbnailURL = "";

    private static final String TAG = "Cloudinary playlist";

    // dialog related things
    EditText edt_playlistTitle;
    TextView tv_thumbnailName, tv_tags;
    Button btn_pickThumbnail, btn_pickTag, btn_createPlaylist;
    ImageView img_thumbnail;

    private static final int PICK_IMAGE_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_playlist_manager);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        img_addPlaylist = findViewById(R.id.img_addPlaylist);
        rv_playlist = findViewById(R.id.rv_playlist);

        getUserPlaylists(user.getUid());
        adapter = new ItemAdapter(this, playlistCreatedList, ItemAdapter.Type.PLAYLIST, 1);
        rv_playlist.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        rv_playlist.setAdapter(adapter);

        dialog = new Dialog(this);
        dialog.setContentView(R.layout.create_playlist_dialog);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawableResource(R.drawable.custom_dialog_background);
        dialog.setCancelable(true);

        edt_playlistTitle = dialog.findViewById(R.id.edt_playlistTitle);
        tv_thumbnailName = dialog.findViewById(R.id.tv_thumbnailName);
        tv_tags = dialog.findViewById(R.id.tv_tags);
        btn_pickThumbnail = dialog.findViewById(R.id.btn_pickThumbnail);
        btn_pickTag = dialog.findViewById(R.id.btn_pickTag);
        img_thumbnail = dialog.findViewById(R.id.img_thumbnail);
        btn_createPlaylist = dialog.findViewById(R.id.btn_createPlaylist);

        btn_pickTag.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTagSelectionDialog();
            }
        });

        img_addPlaylist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.show();
            }
        });

        btn_pickThumbnail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickImageFile();
            }
        });

        btn_createPlaylist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (edt_playlistTitle.getText().toString().isEmpty()){
                    Toast.makeText(PlaylistManagerActivity.this, "fill title pls", Toast.LENGTH_SHORT).show();
                    return;
                }

                if(imageUri == null){
                    uploadDataToFirebase("", edt_playlistTitle.getText().toString());
                }
                else {
                    uploadCloudinaryImage();
                }
            }
        });
    }

    private String generateRandomString(int length){
        String allowedChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random rand=new Random();
        StringBuilder res=new StringBuilder();
        for (int i = 0; i < length; i++) {
            int randIndex=rand.nextInt(allowedChars.length());
            res.append(allowedChars.charAt(randIndex));
        }
        return res.toString();
    }

    private void uploadDataToFirebase(String thumbnailURL, String title){
        DocumentReference artistRef = db.collection("users").document(user.getUid());

        ArrayList<DocumentReference> tagRefs = new ArrayList<>();

        ArrayList<String> SongIDs = new ArrayList<>();

        for (Tag tag : selectedTags) {
            DocumentReference tagRef = db.collection("tags").document(tag.getID());
            tagRefs.add(tagRef);
        }

//        creating song document with all fields
        String randomCollectionString = generateRandomString(12);

        Map<String, Object> playlist = new HashMap<>();
        playlist.put("Creator", artistRef);
        playlist.put("ID", randomCollectionString);
        playlist.put("Tags", tagRefs);
        playlist.put("ThumbnailUrl", thumbnailURL);
        playlist.put("Title", title);
        playlist.put("SongIDs", SongIDs);

        db.collection("playlists").document(randomCollectionString).set(playlist).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                Log.d("Upload firebase", "DocumentSnapshot successfully written!");
                Toast.makeText(PlaylistManagerActivity.this, "Upload thanh cong", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.w("Upload firebase", "Error writing document", e);
            }
        });
    }

    private void uploadCloudinaryImage(){
        MediaManager.get().upload(imageUri).callback(new UploadCallback() {
            @Override
            public void onStart(String requestId) {
                Log.d(TAG, "onStart: ");
            }

            @Override
            public void onProgress(String requestId, long bytes, long totalBytes) {
                Log.d(TAG, "onProgress: " + bytes + " / " + totalBytes);
            }

            @Override
            public void onSuccess(String requestId, Map resultData) {
                thumbnailURL = (String) resultData.get("secure_url");
                uploadDataToFirebase(thumbnailURL, edt_playlistTitle.getText().toString());
            }

            @Override
            public void onError(String requestId, ErrorInfo error) {
                Log.d(TAG, "onError: " + error);
            }

            @Override
            public void onReschedule(String requestId, ErrorInfo error) {
                Log.d(TAG, "onReschedule: ");
            }
        }).dispatch();
    }

    private void getAllTagsFromDatabase(){
        db.collection("tags").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()){
                    tagList.clear();
                    for (QueryDocumentSnapshot document : task.getResult()){
                        Tag tag = document.toObject(Tag.class);
                        tagList.add(tag);
                    }
                    if (!tagList.isEmpty()) {
                        showTagSelectionDialog();
                    } else {
                        Toast.makeText(PlaylistManagerActivity.this, "khong to tags ?", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d("get tags", "failed ");
            }
        });
    }

    private void showTagSelectionDialog() {
        // Get all tags if not already loaded
        if (tagList.isEmpty()) {
            getAllTagsFromDatabase();
            // Show loading while fetching tags
            Toast.makeText(PlaylistManagerActivity.this, "Loading tags...", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create list of tag names and tracking for selected state
        final String[] tagNames = new String[tagList.size()];
        final boolean[] checkedTags = new boolean[tagList.size()];

        // Prepare data for dialog
        for (int i = 0; i < tagList.size(); i++) {
            tagNames[i] = tagList.get(i).getName(); // Assuming Tag class has getName() method

            // Pre-check tags that are already selected
            checkedTags[i] = false;
            for (Tag selectedTag : selectedTags) {
                if (selectedTag.getID().equals(tagList.get(i).getID())) { // Assuming Tag class has getID() method
                    checkedTags[i] = true;
                    break;
                }
            }
        }

        // Create dialog builder
        AlertDialog.Builder builder = new AlertDialog.Builder(PlaylistManagerActivity.this);
        builder.setTitle("Chọn thể loại");

        // Set multiple choice items
        builder.setMultiChoiceItems(tagNames, checkedTags, new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                // Update checked state
                checkedTags[which] = isChecked;
            }
        });

        // ok button
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Clear previously selected tags
                selectedTags.clear();

                // Add newly selected tags
                StringBuilder selectedTagsText = new StringBuilder();

                for (int i = 0; i < checkedTags.length; i++) {
                    if (checkedTags[i]) {
                        selectedTags.add(tagList.get(i));

                        // Append tag name to display text
                        if (selectedTagsText.length() > 0) {
                            selectedTagsText.append(", ");
                        }
                        selectedTagsText.append(tagNames[i]);
                    }
                }

                // Update the TextView
                if (selectedTagsText.length() > 0) {
                    tv_tags.setText(selectedTagsText.toString());
                } else {
                    tv_tags.setText("No tags selected");
                }
            }
        });

        // cancel button
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        // Show the dialog
        builder.create().show();
    }

    private void getUserPlaylists(String userID){
        DocumentReference creatorRef = db.document("users/"+userID);

        db.collection("playlists").whereEqualTo("Creator", creatorRef).addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                if(value != null){
                    playlistCreatedList.clear();
                    for(QueryDocumentSnapshot document : value){
                        Playlist playlist = document.toObject(Playlist.class);

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
                        }
                        playlistCreatedList.add(playlist);
                        if (playlistCreatedList.size() == value.size()){
                            adapter.notifyDataSetChanged();
                        }
                    }
                }
            }
        });
    }

    private void pickImageFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*"); // Lọc các file âm thanh
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK){
            if (data != null){
                imageUri = data.getData();
                String fileName = getFileNameFromUri(this, imageUri);
                Glide.with(this).load(imageUri).into(img_thumbnail);
                tv_thumbnailName.setText(fileName);
                Log.d("FILE_NAME", "Tên file: " + fileName);
            }
        }
    }

    private String getFileNameFromUri(Context context, Uri uri) {
        String result = null;

        if (uri.getScheme().equals("content")) {
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex);
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        return result;
    }
}