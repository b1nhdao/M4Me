package com.example.m4me.activity;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.m4me.R;
import com.example.m4me.adapter.CommentAdapter_Comment_Vertically;
import com.example.m4me.adapter.ItemAdapter_Global_Vertically;
import com.example.m4me.model.Comment;
import com.example.m4me.model.Playlist;
import com.example.m4me.model.Song;
import com.example.m4me.sensor.ShakeSensor;
import com.example.m4me.service.MusicService;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class SongPlayingActivity extends AppCompatActivity {

    private List<Song> songList = new ArrayList<>();
    private Song currentSong;
    int songDuration;

    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable runnable;

    private TextView tv_songArtist, tv_songTitle, tv_currentTime ,tv_endTime;
    private ImageView img_songThumbnail;

    private ImageView img_forward, img_play_or_pause, img_skip, img_favourite, img_comment, img_loop, img_options;
    private SeekBar seekBar;

    private boolean isPlaying;
    private boolean isLooping;

    private ShakeSensor shakeManager;

    private FirebaseAuth auth = FirebaseAuth.getInstance();
    private FirebaseUser user = auth.getCurrentUser();
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    // dialog related things
    private Dialog dialog;
    private RecyclerView rv_playlist;
    private List<Playlist> playlistCreatedList = new ArrayList<>();

    private ItemAdapter_Global_Vertically adapter;

    // bottom sheet
    private BottomSheetBehavior<ConstraintLayout> commentBottomSheetBehavior;
    private ConstraintLayout commentBottomSheet;
    private RecyclerView rv_comment;
    private EditText edt_content;
    private ImageView img_send;
//    private ImageView btnCloseComments;
    private List<Comment> commentList = new ArrayList<>();
    private CommentAdapter_Comment_Vertically commentAdapter;


    private void setupShakeDetector() {
        shakeManager = new ShakeSensor(this, new ShakeSensor.OnShakeListener() {
            @Override
            public void onShake() {
                sendActionToService(MusicService.ACTION_NEXT);
            }
        });

        if (!shakeManager.hasAccelerometer()) {
            Toast.makeText(this, "get a new phone bruh !", Toast.LENGTH_SHORT).show();
        }
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            if(bundle == null){
                return;
            }
            currentSong = (Song) bundle.get("object_song");
            isPlaying = bundle.getBoolean("status_player");
            isLooping = bundle.getBoolean("status_loop");
            int actionMusic = bundle.getInt("action_music");

            handleLayoutMusic(actionMusic);
        }
    };

    private BroadcastReceiver seekbarReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long currentPosition = intent.getLongExtra("current_position", 0);
            long duration = intent.getLongExtra("duration", 0);

//            update timer
            songDuration = (int) duration / 1000;
            tv_endTime.setText(formatTime(songDuration));
            tv_currentTime.setText(formatTime((int) currentPosition / 1000));

            seekBar.setMax((int) duration);
            seekBar.setProgress((int) currentPosition);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_song_playing);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();

        Bundle bundle = getIntent().getExtras();
        if(bundle != null){
            songList = (List<Song>) bundle.get("list_object_song");
            currentSong = (Song) bundle.get("object_song");

            updateUIInitiate(currentSong);
        }
        else{
            Log.d("SongList", "no bundle found");
        }


        //dialog
        dialog = new Dialog(this);
        dialog.setContentView(R.layout.add_to_playlist_dialog);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawableResource(R.drawable.custom_dialog_background);
        dialog.setCancelable(true);

        rv_playlist = dialog.findViewById(R.id.rv_playlist);
        rv_playlist.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));

        //comment bottom sheet setup

        commentBottomSheet = findViewById(R.id.comment_bottom_sheet);
        commentBottomSheetBehavior = BottomSheetBehavior.from(commentBottomSheet);
        commentBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);


        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, new IntentFilter("send_data_to_activity"));
        LocalBroadcastManager.getInstance(this).registerReceiver(seekbarReceiver, new IntentFilter("update_seekbar"));

        setupShakeDetector();
        shakeManager.start();

        if (shakeManager != null) {
            shakeManager.registerServiceClearListener();
        }

        startSeekBarUpdater();

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser){
                    sendActionToService(MusicService.ACTION_SEEK, progress);
                    tv_currentTime.setText(formatTime(progress / 1000));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        img_forward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendActionToService(MusicService.ACTION_PREV);
            }
        });

        img_skip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendActionToService(MusicService.ACTION_NEXT);
            }
        });

        img_play_or_pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnPlayOrPauseOnClick(isPlaying);
            }
        });

        img_favourite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                adding this song to user/favouriteSong
            }
        });

        img_comment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imgCommentOnClick();
            }
        });

        img_loop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendActionToService(MusicService.ACTION_LOOP);
            }
        });

        img_options.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                optionsOnClick();
            }
        });

        img_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String content = edt_content.getText().toString();
                if (content.isEmpty()){
                    Toast.makeText(SongPlayingActivity.this, "trong'", Toast.LENGTH_SHORT).show();
                }
                else {
                    createComment(content);
                }
            }
        });
    }

    private void imgCommentOnClick(){
        commentAdapter = new CommentAdapter_Comment_Vertically(this, commentList);
        rv_comment.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        rv_comment.setAdapter(commentAdapter);
        commentBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        getCommentsFromSongID();
    }

    private void optionsOnClick(){
        PopupMenu popupMenu = new PopupMenu(this, img_options);
        MenuInflater inflater = popupMenu.getMenuInflater();
        inflater.inflate(R.menu.song_options_menu, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if(item.getItemId() == R.id.addToPlaylist){
                    Log.d("menuclick", "onMenuItemClick: tralalero tralala");
                    getAllPlaylistFromUser(user.getUid());
                    adapter = new ItemAdapter_Global_Vertically(SongPlayingActivity.this, playlistCreatedList, ItemAdapter_Global_Vertically.Type.CREATEDLIST, currentSong.getID());
                    rv_playlist.setAdapter(adapter);

                    dialog.show();
                    return true;
                }
                if(item.getItemId() == R.id.download){
                    Log.d("menuclick", "onMenuItemClick: bombodino crocodino");
                    return true;
                }
                return false;
            }
        });
        popupMenu.show();
    }

    private void initViews(){
        tv_songArtist = findViewById(R.id.tv_songArtist);
        tv_songTitle = findViewById(R.id.tv_songTitle);
        img_songThumbnail = findViewById(R.id.img_songThumbnail);
        img_forward = findViewById(R.id.img_forward);
        img_play_or_pause = findViewById(R.id.img_play_or_pause);
        img_skip = findViewById(R.id.img_skip);
        img_favourite = findViewById(R.id.img_favourite);
        img_comment = findViewById(R.id.img_comment);
        img_loop = findViewById(R.id.img_loop);
        img_options = findViewById(R.id.img_options);
        seekBar = findViewById(R.id.seekBar);
        tv_currentTime = findViewById(R.id.tv_currentTime);
        tv_endTime = findViewById(R.id.tv_endTime);

        rv_comment = findViewById(R.id.rv_comment);
        edt_content = findViewById(R.id.edt_content);
        img_send = findViewById(R.id.img_send);
    }

    private void sendActionToService(int action){
        Intent intent = new Intent(this, MusicService.class);
        intent.putExtra("action_music_service", action);
        startService(intent);
    }

    private void sendActionToService(int action, int progress){
        Intent intent = new Intent(this, MusicService.class);
        intent.putExtra("action_music_service", action);
        intent.putExtra("seek_position", progress);
        startService(intent);
    }

    private void btnPlayOrPauseOnClick(boolean isPlaying){
        if (isPlaying){
            sendActionToService(MusicService.ACTION_PAUSE);
        }
        else {
            sendActionToService(MusicService.ACTION_RESUME);
        }
    }

    private void updateUI(){
        if (currentSong != null){
            Glide.with(this).load(currentSong.getThumbnailUrl()).into(img_songThumbnail);
            tv_songArtist.setText(currentSong.getArtistName());
            tv_songTitle.setText(currentSong.getTitle());
            tv_endTime.setText(formatTime(songDuration));
        }
    }

//    idk why
    private void updateUIInitiate(Song song){
        Glide.with(this).load(song.getThumbnailUrl()).into(img_songThumbnail);
        tv_songArtist.setText(song.getArtistName());
        tv_songTitle.setText(song.getTitle());
    }

    private String formatTime(int duration) {
        int minutes = (duration / 60);
        int seconds = (duration % 60);
        if(seconds < 10){
            return minutes + ":0" + seconds;
        }
        else {
            return (minutes + ":" + seconds);
        }
    }

    private void handleLayoutMusic(int action){
        switch (action){
            case MusicService.ACTION_START:
                isPlaying = true;
                setStatusButtonPlayOrPause();
                startSeekBarUpdater();
                updateUI();
                break;
            case MusicService.ACTION_PAUSE:
                isPlaying = false;
                setStatusButtonPlayOrPause();
                break;
            case MusicService.ACTION_RESUME:
                isPlaying = true;
                setStatusButtonPlayOrPause();
                break;
            case MusicService.ACTION_CLEAR:
                stopSeekBarUpdater();
                break;
            case MusicService.ACTION_LOOP:
                setStatusButtonLoop();
                break;
        }
    }

    private void setStatusButtonPlayOrPause(){
        if(isPlaying){
            img_play_or_pause.setImageResource(R.drawable.pause_circle_24px);
        }
        else{
            img_play_or_pause.setImageResource(R.drawable.play_circle_24px);
        }
    }

    private void setStatusButtonLoop(){
        if(isLooping){
            img_loop.setImageResource(R.drawable.baseline_repeat_on_24);
        }
        else{
            img_loop.setImageResource(R.drawable.baseline_repeat_24);
        }
    }

    private void startSeekBarUpdater() {
        runnable = new Runnable() {
            @Override
            public void run() {
                updateSeekBar();
//                tv_currentTime.setText(formatTime(player.getCurrentPosition() / 1000));
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(runnable);
    }

    private void updateSeekBar() {
        Intent intent = new Intent(this, MusicService.class);
        intent.setAction("get_current_position");
        startService(intent);
    }

    private void stopSeekBarUpdater() {
        if (handler != null && runnable != null) {
            handler.removeCallbacks(runnable);
        }
    }

    private void getAllPlaylistFromUser(String userID){
        DocumentReference creatorRef = db.document("users/" + userID);
        db.collection("playlists").whereEqualTo("Creator", creatorRef).addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                if (value != null){
                    playlistCreatedList.clear();
                    for (QueryDocumentSnapshot document : value){
                        Playlist playlist = document.toObject(Playlist.class);
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

                                    // check if all tag requests are completed
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
                    }
                }
            }
        });
    }

    private void getCommentsFromSongID(){
        if (currentSong != null){
            db.collection("songs").document(currentSong.getID()).collection("comments").orderBy("timestamp", Query.Direction.DESCENDING).addSnapshotListener(new EventListener<QuerySnapshot>() {
                @Override
                public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                    if (value != null){
                        commentList.clear();
                        for (QueryDocumentSnapshot document : value){
                            Comment comment = document.toObject(Comment.class);
                            if (comment != null){
                                commentList.add(comment);
                                Log.d("comment", "onEvent: " + comment.getContent());
                            }
                        }
                        commentAdapter.notifyDataSetChanged();
                    }
                    else{
                        Log.d("comment", "onEvent: kkk failed");
                    }
                }
            });
        }
    }

    private void createComment(String commentContent){
        if(currentSong == null){
            return;
        }

        DocumentReference userRef = db.collection("users").document(user.getUid());

        String randomCollectionString = generateRandomString(12);

        Map<String, Object> comment = new HashMap<>();
        comment.put("User", userRef);
        comment.put("UserName", user.getDisplayName());
        comment.put("ID", randomCollectionString);
        comment.put("Content", commentContent);
        comment.put("timestamp", FieldValue.serverTimestamp());

        db.collection("songs").document(currentSong.getID()).collection("comments").add(comment).addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
            @Override
            public void onComplete(@NonNull Task<DocumentReference> task) {
                Log.d("addcomment", "onComplete: add comment done");
                edt_content.setText("");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d("addcomment", "onFailure: failed");
                Toast.makeText(SongPlayingActivity.this, "failed", Toast.LENGTH_SHORT).show();
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(seekbarReceiver);
    }
}