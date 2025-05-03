package com.example.m4me.adapter;

import android.content.Context;
import android.content.Intent;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.m4me.R;
import com.example.m4me.activity.PlaylistActivity;
import com.example.m4me.activity.SongPlayingActivity;
import com.example.m4me.activity.UpdateActivity;
import com.example.m4me.model.Playlist;
import com.example.m4me.model.Song;
import com.example.m4me.model.User;
import com.example.m4me.service.MusicService;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.MyViewHolder> {

    public enum Type {
        SONG,
        PLAYLIST,
        USER,
        CREATEDLIST
    }

    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    private Context context;
    private List<Song> songList;
    private List<Playlist> playlistList;
    private List<User> userList;
    private Type type;
    private String songToAddID;

    // code 1 = from upload playlist manager activity
    private int specialCode;

    public ItemAdapter(Context context, Object dataList, Type type) {
        this.context = context;
        this.type = type;

        if (type == Type.SONG) {
            this.songList = (List<Song>) dataList;
        } else if (type == Type.PLAYLIST || type == Type.CREATEDLIST) {
            this.playlistList = (List<Playlist>) dataList;
        } else if (type == Type.USER) {
            this.userList = (List<User>) dataList;
        }
    }

    public ItemAdapter(Context context, Object dataList, Type type, String songID){
        this.context = context;
        this.type = type;
        if(type == Type.CREATEDLIST){
            this.playlistList = (List<Playlist>) dataList;
            this.songToAddID = songID;
        }
    }

    public ItemAdapter(Context context, Object dataList, Type type, int specialCode){
        this.context = context;
        this.type = type;
        if(type == Type.PLAYLIST){
            this.playlistList = (List<Playlist>) dataList;
            this.specialCode = specialCode;
        }
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.search_item_vertically, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        switch (type){
            case SONG:
                Song song = songList.get(position);
                holder.tv_title.setText(shortenString(song.getTitle(), 32));
                holder.tv_songArtist.setText(song.getArtistName());
                if (song.getThumbnailUrl() != ""){
                    Glide.with(context).load(song.getThumbnailUrl()).into(holder.img_thumbnail);
                }

                List<String> songTags = song.getTagNames();
                if (songTags != null && !songTags.isEmpty()) {
                    TagAdapter tagAdapter = new TagAdapter(context, songTags);
                    LinearLayoutManager layoutManager = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);
                    holder.rv_tags.setLayoutManager(layoutManager);
                    holder.rv_tags.setAdapter(tagAdapter);
                } else {
                    holder.rv_tags.setAdapter(null);
                }

                holder.cardView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startSongMusicService(song);
                        clickSongChangeActivity(song);
                    }
                });

                break;

            case PLAYLIST:
            case CREATEDLIST:
                Playlist playlist = playlistList.get(position);
                holder.tv_title.setText(shortenString(playlist.getTitle(), 32));
                holder.tv_songArtist.setText(playlist.getCreatorName());
                if (playlist.getThumbnailURL() != null){
                    Glide.with(context).load(playlist.getThumbnailURL()).into(holder.img_thumbnail);
                }

                List<String> playlistTags = playlist.getTagNames();
                if (playlistTags != null && !playlistTags.isEmpty()) {
                    TagAdapter tagAdapter = new TagAdapter(context, playlistTags);
                    LinearLayoutManager layoutManager = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);
                    holder.rv_tags.setLayoutManager(layoutManager);
                    holder.rv_tags.setAdapter(tagAdapter);
                } else {
                    holder.rv_tags.setAdapter(null);
                }

                if (specialCode == 1){
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
                                        updateMenuOnClick(playlist);
                                        return true;
                                    }
                                    if(item.getItemId() == R.id.delete){
                                        Log.d("menuclick", "onMenuItemClick: delete");
                                        removeCreatedPlaylistFromDatabase(position);
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

                if (type == Type.PLAYLIST){
                    holder.cardView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            clickPlaylistChangeActivity(playlist);
                        }
                    });
                }
                else {
                    holder.cardView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            addSongIDToPlaylist(playlist.getID(), songToAddID);
//                            Toast.makeText(context, "Clicked on created list", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                break;


            case USER:
                User user = userList.get(position);
                holder.tv_title.setText(user.getDisplayName());
                holder.tv_songArtist.setText(user.getEmail());

                holder.cardView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
//                        TODO: open UserActivity
                    }
                });

                break;
        }
    }

    private void updateMenuOnClick(Playlist playlist){
        Intent intent = new Intent(context, UpdateActivity.class);
        Bundle bundle = new Bundle();
        bundle.putSerializable("object_playlist", playlist);
        intent.putExtras(bundle);
        context.startActivity(intent);
    }

    private void removeCreatedPlaylistFromDatabase(int position){
        Playlist playlist = playlistList.get(position);
        String playlistID = playlist.getID();
        db.collection("playlists").document(playlistID).delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                if (position < playlistList.size()){
                    playlistList.remove(position);
                    notifyItemRemoved(position);

                    if (position < playlistList.size()){
                        notifyItemRangeChanged(position, playlistList.size() - position);
                    }
                    Toast.makeText(context, "deleted", Toast.LENGTH_SHORT).show();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(context, "failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void clickSongChangeActivity(Song song){
        Intent intent = new Intent(context, SongPlayingActivity.class);
        Bundle bundle = new Bundle();
        bundle.putSerializable("object_song", song);
        intent.putExtras(bundle);
        context.startActivity(intent);
    }

    private void startSongMusicService(Song song){
        Intent intent = new Intent(context, MusicService.class);
        Bundle bundle = new Bundle();
        bundle.putSerializable("object_song", song);
        intent.putExtras(bundle);
        context.startService(intent);
    }

    private void clickPlaylistChangeActivity(Playlist playlist){
        Intent intent = new Intent(context, PlaylistActivity.class);
        Bundle bundle = new Bundle();
        bundle.putSerializable("object_playlist", playlist);
        if (specialCode == 1){
            bundle.putInt("manager_code", 1);
        }
        intent.putExtras(bundle);
        context.startActivity(intent);
    }

    private void addSongIDToPlaylist(String playlistID, String songID){
        db.collection("playlists").document(playlistID).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.getResult() != null){
                    // getting all songIDs in current working playlist
                    List<String> currentSongIDs = (List<String>) task.getResult().get("SongIDs");
                    if (currentSongIDs == null){
                        currentSongIDs = new ArrayList<>();
                    }

                    if (!currentSongIDs.contains(songID)){
                        currentSongIDs.add(songID);

                        // update song list
                        db.collection("playlists").document(playlistID).update("SongIDs", currentSongIDs).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unused) {
                                Toast.makeText(context, "DONE", Toast.LENGTH_SHORT).show();
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(context, e + "", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } // if add to playlist
                    else {
                        Toast.makeText(context, "Bai hat da co trong playlist roi", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(context, e + "", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String shortenString (String s, int maxLength){
        if (s.length() >= maxLength){
            return s.substring(0, maxLength) + "...";
        }
        else return s;
    }

    @Override
    public int getItemCount() {
        switch (type){
            case SONG:
                return songList.size();
            case PLAYLIST:
            case CREATEDLIST:
                return playlistList.size();
            case USER:
                return userList.size();
            default:
                return 0;
        }
    }

    class MyViewHolder extends RecyclerView.ViewHolder {
        ImageView img_thumbnail;
        TextView tv_title, tv_songArtist;
        RecyclerView rv_tags;
        CardView cardView;
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            img_thumbnail = itemView.findViewById(R.id.img_thumbnail);
            tv_title = itemView.findViewById(R.id.tv_title);
            tv_songArtist = itemView.findViewById(R.id.tv_songArtist);
            rv_tags = itemView.findViewById(R.id.rv_tags);
        }
    }
}
