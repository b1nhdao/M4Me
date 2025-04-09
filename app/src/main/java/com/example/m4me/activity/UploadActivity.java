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
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.m4me.R;
import com.example.m4me.model.Song;
import com.example.m4me.model.Tag;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

public class UploadActivity extends AppCompatActivity {

    private EditText edt_title;
    private TextView tv_tag, tv_fileNameThumbnail, tv_fileNameSong;
    private Button btn_pickTag, btn_pickThumbnail, btn_pickSong, btn_uploadSong;
    private ImageView img_thumbnail;

    private Uri audioUri = null;
    private Uri imageUri = null;

    private List<Tag> tagList = new ArrayList<>();
    private List<Tag> selectedTags = new ArrayList<>();

    private String thumbnailURL = "";
    private String songURL = "";

    private FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    private static final String TAG = "cloudinary";

    private static final int PICK_AUDIO_REQUEST = 1;
    private static final int PICK_IMAGE_REQUEST = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_upload);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();

        btn_pickThumbnail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickImageFile();
            }
        });

        btn_pickSong.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickAudioFile();
            }
        });

        btn_pickTag.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTagSelectionDialog();
            }
        });

        btn_uploadSong.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (audioUri != null){
                    uploadCloudinarySong();
                }
                else {
                    Toast.makeText(UploadActivity.this, "Hãy chọn file nhạc bạn muốn", Toast.LENGTH_SHORT).show();
                }

            }
        });
    }

    private void initViews(){
        edt_title = findViewById(R.id.edt_title);
        tv_tag = findViewById(R.id.tv_tag);
        tv_fileNameThumbnail = findViewById(R.id.tv_fileNameThumbnail);
        tv_fileNameSong = findViewById(R.id.tv_fileNameSong);
        btn_pickTag = findViewById(R.id.btn_pickTag);
        btn_pickThumbnail = findViewById(R.id.btn_pickThumbnail);
        btn_pickSong = findViewById(R.id.btn_pickSong);
        btn_uploadSong = findViewById(R.id.btn_uploadSong);
        img_thumbnail = findViewById(R.id.img_thumbnail);
    }

    private void showTagSelectionDialog() {
        // Get all tags if not already loaded
        if (tagList.isEmpty()) {
            getAllTagsFromDatabase();
            // Show loading while fetching tags
            Toast.makeText(UploadActivity.this, "Loading tags...", Toast.LENGTH_SHORT).show();
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
        AlertDialog.Builder builder = new AlertDialog.Builder(UploadActivity.this);
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

    private void uploadDataToFirebase(String sourceURL, String thumbnailURL, String title){
        DocumentReference artistRef = db.collection("users").document(user.getUid());

        ArrayList<DocumentReference> tagRefs = new ArrayList<>();

        for (Tag tag : selectedTags) {
            DocumentReference tagRef = db.collection("tags").document(tag.getID());
            tagRefs.add(tagRef);
        }

//        creating song document with all fields
        String randomCollectionString = generateRandomString(12);

        Map<String, Object> song = new HashMap<>();
        song.put("Artist", artistRef);
        song.put("ArtistName", user.getDisplayName());
        song.put("ID", randomCollectionString);
        song.put("PlayedCounter", 0);
        song.put("SourceURL", sourceURL);
        song.put("Tags", tagRefs);
        song.put("ThumbnailUrl", thumbnailURL);
        song.put("Title", title);

        db.collection("songs").document(randomCollectionString).set(song).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                Log.d("Upload firebase", "DocumentSnapshot successfully written!");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.w("Upload firebase", "Error writing document", e);
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

    private void uploadCloudinaryImage(){
        MediaManager.get().upload(imageUri).callback(new UploadCallback() {
            @Override
            public void onStart(String requestId) {
                Log.d(TAG, "onStart: ");
                Toast.makeText(UploadActivity.this, "Đang tải ...", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onProgress(String requestId, long bytes, long totalBytes) {
                Log.d(TAG, "onProgress: " + bytes + " / " + totalBytes);
            }

            @Override
            public void onSuccess(String requestId, Map resultData) {
                thumbnailURL = (String) resultData.get("secure_url");
                Toast.makeText(UploadActivity.this, "Upload thành công", Toast.LENGTH_SHORT).show();
                uploadDataToFirebase(songURL, thumbnailURL, edt_title.getText().toString());
                Log.d(TAG, "onSuccess thumbnail:  " + thumbnailURL);
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

    private void uploadCloudinarySong(){
        MediaManager.get().upload(audioUri).option("resource_type", "auto").callback(new UploadCallback() {
            @Override
            public void onStart(String requestId) {
                Log.d(TAG, "onStart: ");
                Toast.makeText(UploadActivity.this, "Đang tải ...", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onProgress(String requestId, long bytes, long totalBytes) {
                Log.d(TAG, "onProgress: " + bytes + " / " + totalBytes);
            }

            @Override
            public void onSuccess(String requestId, Map resultData) {
                songURL = (String) resultData.get("secure_url");
                if(imageUri != null){
                    uploadCloudinaryImage();
                }
                else {
                    uploadDataToFirebase(songURL, thumbnailURL, edt_title.getText().toString());
                }
                Toast.makeText(UploadActivity.this, "Upload thành công", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "onSuccess song: " + songURL);
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
                        Toast.makeText(UploadActivity.this, "khong to tags ?", Toast.LENGTH_SHORT).show();
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

    private void pickAudioFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("audio/*"); // Lọc các file âm thanh

        startActivityForResult(intent, PICK_AUDIO_REQUEST);
    }

    private void pickImageFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*"); // Lọc các file âm thanh
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_AUDIO_REQUEST && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                audioUri = data.getData();
                String fileName = getFileNameFromUri(this, audioUri);
                tv_fileNameSong.setText(fileName);
                edt_title.setText(fileName);
                Log.d("FILE_NAME", "Tên file: " + fileName);
            }
        }
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK){
            if (data != null){
                imageUri = data.getData();
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
}