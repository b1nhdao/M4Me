package com.example.m4me.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.m4me.R;
import com.example.m4me.activity.SongPlayingActivity;
import com.example.m4me.activity.UpdateActivity;
import com.example.m4me.model.Song;
import com.example.m4me.service.MusicService;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.datatype.Artwork;

import java.io.File;
import java.io.Serializable;
import java.util.List;

public class SongAdapter_Playlist_Vertically extends RecyclerView.Adapter<SongAdapter_Playlist_Vertically.MyViewHolder> {

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();

    private Context context;
    private List<Song> songList;
    private int activityCode;
    private int specialCode;
    private String platlistID;

    public SongAdapter_Playlist_Vertically(Context context, List<Song> songList, int activityCode) {
        this.context = context;
        this.songList = songList;
        this.activityCode = activityCode;
    }

    public SongAdapter_Playlist_Vertically(Context context, List<Song> songList, int activityCode, int specialCode, String platlistID) {
        this.context = context;
        this.songList = songList;
        this.activityCode = activityCode;
        this.specialCode = specialCode;
        this.platlistID = platlistID;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.song_item_in_playlist_vertically, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Song song = songList.get(position);

        // offline
        if (activityCode == 3){
            Glide.with(context).load(song.getThumbnailBitmap()).into(holder.img_thumbnail);
            holder.tv_songTitle.setText(shortenString(song.getTitle()));
            holder.tv_songArtist.setText(song.getArtistName());

            loadThumbnailFrom(song, holder.img_thumbnail);

            holder.img_remove.setImageResource(R.drawable.baseline_delete_24);
            holder.img_remove.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    deleteSong(song.getFilePath());
                    if (position < songList.size()){
                        songList.remove(position);
                        notifyItemRemoved(position);
                        if (position < songList.size()){
                            notifyItemRangeChanged(position, songList.size() - position);
                        }
                        Toast.makeText(context, "deleted", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        // idk
        if (activityCode <= 2){
            if (song.getThumbnailUrl() != ""){
                Glide.with(context).load(song.getThumbnailUrl()).into(holder.img_thumbnail);
            }
            holder.tv_songTitle.setText(shortenString(song.getTitle()));
            holder.tv_songArtist.setText(song.getArtistName());

            List<String> tags = song.getTagNames();
            if (tags != null && !tags.isEmpty()) {
                TagAdapter_Global_Horizontally tagAdapter = new TagAdapter_Global_Horizontally(context, tags);
                LinearLayoutManager layoutManager = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);
                holder.rv_tags.setLayoutManager(layoutManager);
                holder.rv_tags.setAdapter(tagAdapter);
            } else {
                holder.rv_tags.setAdapter(null);
            }

            // the normal one
            if(activityCode == 1){
                if (specialCode == 1){
                    holder.img_remove.setImageResource(R.drawable.baseline_delete_24);
                    holder.img_remove.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            removeSongFromPlaylist(position, platlistID);
                        }
                    });
                }
                else {
                    if (song.isFavourite()){
                        holder.img_isFavourite.setImageResource(R.drawable.baseline_favorite_24);
                    }
                    else {
                        holder.img_isFavourite.setImageResource(R.drawable.baseline_favorite_border_24);
                    }
                }

                holder.img_isFavourite.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        updateFavourite(song);
                    }
                });
            }

            // playlist management
            if (activityCode == 2){
                holder.cardView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        PopupMenu popupMenu = new PopupMenu(context, holder.cardView);
                        MenuInflater inflater = popupMenu.getMenuInflater();
                        inflater.inflate(R.menu.library_options_menu, popupMenu.getMenu());
                        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                if(item.getItemId() == R.id.update){
                                    Log.d("menuclick", "onMenuItemClick: update");
                                    updateMenuOnClick(song);
                                    return true;
                                }
                                if(item.getItemId() == R.id.delete){
                                    Log.d("menuclick", "onMenuItemClick: delete");
                                    removeUploadedSongFromDatabase(position);
                                    return true;
                                }
                                return false;
                            }
                        });
                        popupMenu.show();
                        return false;
                    }
                });
            }
        }

        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int currentSongIndex = position;
                startMusicService(currentSongIndex);
                clickChangeActivity(song);
            }
        });
    }

    private void loadThumbnailFrom(Song song, ImageView img){
        try {
            //  jAudiotagger to read metadata
            AudioFile audioFile = AudioFileIO.read(new File(song.getFilePath()));
            Tag tag = audioFile.getTag();

            if (tag != null) {
             // thumbnail
                Artwork artwork = tag.getFirstArtwork();
                if (artwork != null) {
                    byte[] artworkData = artwork.getBinaryData();
                    Bitmap thumbnail = BitmapFactory.decodeByteArray(artworkData, 0, artworkData.length);
                    Glide.with(context).load(thumbnail).into(img);
                }
            }
        } catch (Exception e) {
            Log.e("SongLoader", "Error reading audio file: " );
        }
    }

    public boolean deleteSong(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            return file.delete();
        }
        return false;
    }

    private void removeSongFromPlaylist(int position, String playlistID){
        String songID = songList.get(position).getID();

        db.collection("playlists").document(playlistID).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                //gett all current songs in that playlist
                List<String> currentSongIDs = (List<String>) task.getResult().get("SongIDs");
                if (currentSongIDs.contains(songID)){
                    currentSongIDs.remove(songID);
                }
                db.collection("playlists").document(playlistID).update("SongIDs", currentSongIDs).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Toast.makeText(context, "done", Toast.LENGTH_SHORT).show();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(context, "failed", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void removeUploadedSongFromDatabase(int position){
        Song song = songList.get(position);
        String songID = song.getID();

        db.collection("songs").document(songID).delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                if (position < songList.size()){
                    songList.remove(position);
                    notifyItemRemoved(position);
                    if (position < songList.size()){
                        notifyItemRangeChanged(position, songList.size() - position);
                    }
                    Toast.makeText(context, "deleted", Toast.LENGTH_SHORT).show();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(context, "delete failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateMenuOnClick(Song song){
        Intent intent = new Intent(context, UpdateActivity.class);
        Bundle bundle = new Bundle();
        bundle.putSerializable("object_song", song);
        intent.putExtras(bundle);
        context.startActivity(intent);
    }

    private void updateFavourite(Song song) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(context, "Please login to add favorites", Toast.LENGTH_SHORT).show();
            return;
        }

        String userEmail = currentUser.getEmail();
        if (userEmail == null) {
            return;
        }

        db.collection("users").whereEqualTo("email", userEmail)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {

                        String userId = queryDocumentSnapshots.getDocuments().get(0).getId();
                        String songId = song.getID();

                        List<String> currentFavorites = (List<String>) queryDocumentSnapshots.getDocuments().get(0).get("favouriteSongs");

                        if (currentFavorites != null) {
                            if (currentFavorites.contains(songId)) {
                                // remove fav
                                currentFavorites.remove(songId);
                                song.setFavourite(false);
                                Toast.makeText(context, "Removed from favorites", Toast.LENGTH_SHORT).show();
                            } else {
                                // fav
                                currentFavorites.add(songId);
                                song.setFavourite(true);
                                Toast.makeText(context, "Added to favorites", Toast.LENGTH_SHORT).show();
                            }

                            db.collection("users").document(userId)
                                    .update("favouriteSongs", currentFavorites)
                                    .addOnSuccessListener(aVoid -> notifyDataSetChanged())
                                    .addOnFailureListener(e ->
                                            Toast.makeText(context, "Failed to update favorites", Toast.LENGTH_SHORT).show());
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(context, "Error retrieving user data", Toast.LENGTH_SHORT).show());
    }

    private void startMusicService(int currentSongIndex){
        Intent intent = new Intent(context, MusicService.class);
        Bundle bundle = new Bundle();
        bundle.putSerializable("list_object_song", (Serializable) songList);
        bundle.putInt("current_song_index", currentSongIndex);
        intent.putExtras(bundle);
        ContextCompat.startForegroundService(context, intent);
    }

    private void clickChangeActivity(Song song){
        Intent intent = new Intent(context, SongPlayingActivity.class);
        Bundle bundle = new Bundle();
        bundle.putSerializable("list_object_song", (Serializable) songList);
        bundle.putSerializable("object_song", song);
        intent.putExtras(bundle);
        context.startActivity(intent);
    }

    private String shortenString(String s){
        if (s.length() >= 32){
            return s.substring(0,32) + "...";
        }
        return s;
    }

    @Override
    public int getItemCount() {
        return songList.size();
    }

    class MyViewHolder extends RecyclerView.ViewHolder {
        private CardView cardView;
        private ImageView img_thumbnail, img_options;
        private TextView tv_songTitle, tv_songArtist;
        private RecyclerView rv_tags;
        private ImageView img_isFavourite, img_remove;
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            img_thumbnail = itemView.findViewById(R.id.img_thumbnail);
            tv_songTitle = itemView.findViewById(R.id.tv_songTitle);
            tv_songArtist = itemView.findViewById(R.id.tv_songArtist);
            rv_tags = itemView.findViewById(R.id.rv_tags);
            img_isFavourite = itemView.findViewById(R.id.img_isFavourite);
            img_remove = itemView.findViewById(R.id.img_remove);
        }
    }
}
