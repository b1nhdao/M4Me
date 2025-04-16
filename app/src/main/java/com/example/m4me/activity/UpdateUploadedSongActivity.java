package com.example.m4me.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
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

import com.bumptech.glide.Glide;
import com.example.m4me.R;
import com.example.m4me.model.Song;
import com.example.m4me.model.Tag;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.List;

public class UpdateUploadedSongActivity extends AppCompatActivity {

    private EditText edt_title;
    private TextView tv_tag, tv_fileNameThumbnail;
    private Button btn_pickTag, btn_pickThumbnail, btn_updateSong;
    private ImageView img_thumbnail;

    private Song song;

    private List<Tag> tagList = new ArrayList<>();
    private List<Tag> selectedTags = new ArrayList<>();
    private List<String> tagNames = new ArrayList<>();

    private String thumbnailURL = "";
    private String songID;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    private static final int PICK_IMAGE_REQUEST = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_update_uploaded_song);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();

        Bundle bundle = getIntent().getExtras();
        if (bundle != null){
            song = (Song) bundle.get("object_song");
            songID = song.getID();
            edt_title.setText(song.getTitle());
            thumbnailURL = song.getThumbnailUrl();
            tagNames = song.getTagNames();
            if(tagNames != null){
                tv_tag.setText(tagNames.toString());
            }
            Glide.with(this).load(song.getThumbnailUrl()).into(img_thumbnail);
        }

        btn_pickTag.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTagSelectionDialog();
            }
        });

        btn_pickThumbnail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickImageFile();
            }
        });

        convertStringToTag(tagNames);

        btn_updateSong.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateSongFromFirebase(songID, edt_title.getText().toString(), thumbnailURL, selectedTags);
            }
        });
    }

    private void convertStringToTag(List<String> tagNames) {
        selectedTags.clear(); // Nếu bạn muốn reset lại danh sách trước khi thêm

        for (String tagName : tagNames) {
            db.collection("tags")
                    .whereEqualTo("Name", tagName)
                    .get()
                    .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                        @Override
                        public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                            if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                                List<Tag> tags = queryDocumentSnapshots.toObjects(Tag.class);
                                selectedTags.addAll(tags);
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e("TagFetchError", "Error getting tag: " + tagName, e);
                        }
                    });
        }
    }


    private void initViews(){
        edt_title = findViewById(R.id.edt_title);
        tv_tag = findViewById(R.id.tv_tag);
        tv_fileNameThumbnail = findViewById(R.id.tv_fileNameThumbnail);
        btn_pickTag = findViewById(R.id.btn_pickTag);
        btn_pickThumbnail = findViewById(R.id.btn_pickThumbnail);
        btn_updateSong = findViewById(R.id.btn_updateSong);
        img_thumbnail = findViewById(R.id.img_thumbnail);
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
                Uri imageUri = data.getData();
                String fileName = getFileNameFromUri(this, imageUri);
                Glide.with(this).load(imageUri).into(img_thumbnail);
                tv_fileNameThumbnail.setText(fileName);
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

    private void showTagSelectionDialog() {
        // Get all tags if not already loaded
        if (tagList.isEmpty()) {
            getAllTagsFromDatabase();
            // Show loading while fetching tags
            Toast.makeText(UpdateUploadedSongActivity.this, "Loading tags...", Toast.LENGTH_SHORT).show();
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
        AlertDialog.Builder builder = new AlertDialog.Builder(UpdateUploadedSongActivity.this);
        builder.setTitle("Select Tags");

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
                    tv_tag.setText(selectedTagsText.toString());
                } else {
                    tv_tag.setText("No tags selected");
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
                        Toast.makeText(UpdateUploadedSongActivity.this, "khong to tags ?", Toast.LENGTH_SHORT).show();
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

    private void updateSongFromFirebase(String songID, String title, String thumbnailURL, List<Tag> tags){

        ArrayList<DocumentReference> tagRefs = new ArrayList<>();

        for (Tag tag : tags) {
            DocumentReference tagRef = db.collection("tags").document(tag.getID());
            tagRefs.add(tagRef);
        }

        db.collection("songs").document(songID).update("Title", title, "ThumbnailUrl", thumbnailURL, "Tags", tagRefs).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    Toast.makeText(UpdateUploadedSongActivity.this, "DONE", Toast.LENGTH_SHORT).show();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d("updatesong", "onFailure: " + e.toString());
                Toast.makeText(UpdateUploadedSongActivity.this, "Failed for some reason ?", Toast.LENGTH_SHORT).show();
            }
        });
    }
}