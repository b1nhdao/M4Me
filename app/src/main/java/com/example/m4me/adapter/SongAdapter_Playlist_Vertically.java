package com.example.m4me.adapter;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.m4me.R;
import com.example.m4me.activity.PlaylistActivity;
import com.example.m4me.activity.SongPlayingActivity;
import com.example.m4me.model.Playlist;
import com.example.m4me.model.Song;
import com.example.m4me.service.MusicService;

import java.io.Serializable;
import java.util.List;

public class SongAdapter_Playlist_Vertically extends RecyclerView.Adapter<SongAdapter_Playlist_Vertically.MyViewHolder> {

    Context context;
    List<Song> songList;

    public SongAdapter_Playlist_Vertically(Context context, List<Song> songList) {
        this.context = context;
        this.songList = songList;
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
        Glide.with(context).load(song.getThumbnailUrl()).into(holder.img_thumbnail);
        holder.tv_songTitle.setText(shortenString(song.getTitle()));
        holder.tv_songArtist.setText(song.getArtistName());
        holder.tv_playCounter.setText(song.getPlayedCounter() + "");

        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int currentSongIndex = position;
                Toast.makeText(context, "clicked", Toast.LENGTH_SHORT).show();
                startMusicService(currentSongIndex);
                clickChangeActivity(song);
            }
        });
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
        private TextView tv_songTitle, tv_songArtist, tv_playCounter, tv_duration;
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            img_thumbnail = itemView.findViewById(R.id.img_thumbnail);
            tv_songTitle = itemView.findViewById(R.id.tv_songTitle);
            tv_songArtist = itemView.findViewById(R.id.tv_songArtist);
            tv_playCounter = itemView.findViewById(R.id.tv_playCounter);
            tv_duration = itemView.findViewById(R.id.tv_duration);
        }
    }
}
