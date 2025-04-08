package com.example.m4me.activity;

import android.app.Activity;
import android.content.Context;
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

import java.util.HashMap;
import java.util.Map;

public class UploadActivity extends AppCompatActivity {

    private EditText edt_title;
    private TextView tv_tag, tv_fileNameThumbnail, tv_fileNameSong;
    private Button btn_pickTag, btn_pickThumbnail, btn_pickSong, btn_uploadSong;
    private ImageView img_thumbnail;

    private Uri audioUri = null;
    private Uri imageUri = null;

    String thumbnailURL = "";
    String songURL = "";

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

        btn_uploadSong.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (audioUri != null){
                    uploadCloudinarySong();
                    if (imageUri != null){
                        uploadCloudinaryImage();
                    }
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