package com.example.m4me.adapter;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.m4me.R;
import com.example.m4me.activity.PlaylistActivity;
import com.example.m4me.activity.SongPlayingActivity;
import com.example.m4me.model.Playlist;
import com.example.m4me.model.Song;
import com.example.m4me.service.MusicService;

import java.util.List;

public class SearchItemAdapter_Search_Vertically extends RecyclerView.Adapter<SearchItemAdapter_Search_Vertically.MyViewHolder> {

    public enum Type {
        SONG,
        PLAYLIST
    }

    private Context context;
    private List<Song> songList;
    private List<Playlist> playlistList;
    private Type type;

    public SearchItemAdapter_Search_Vertically(Context context, Object dataList, Type type) {
        this.context = context;
        this.type = type;

        if (type == Type.SONG) {
            this.songList = (List<Song>) dataList;
        } else if (type == Type.PLAYLIST) {
            this.playlistList = (List<Playlist>) dataList;
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
                Glide.with(context).load(song.getThumbnailUrl()).into(holder.img_thumbnail);

                List<String> songTags = song.getTagNames();
                if (songTags != null && !songTags.isEmpty()) {
                    TagAdapter_Global_Horizontally tagAdapter = new TagAdapter_Global_Horizontally(context, songTags);
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
                Playlist playlist = playlistList.get(position);
                holder.tv_title.setText(shortenString(playlist.getTitle(), 32));
                Glide.with(context).load(playlist.getThumbnailURL()).into(holder.img_thumbnail);

                List<String> playlistTags = playlist.getTagNames();
                if (playlistTags != null && !playlistTags.isEmpty()) {
                    TagAdapter_Global_Horizontally tagAdapter = new TagAdapter_Global_Horizontally(context, playlistTags);
                    LinearLayoutManager layoutManager = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);
                    holder.rv_tags.setLayoutManager(layoutManager);
                    holder.rv_tags.setAdapter(tagAdapter);
                } else {
                    holder.rv_tags.setAdapter(null);
                }

                holder.cardView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        clickPlaylistChangeActivity(playlist);
                    }
                });

                break;
        }
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
        intent.putExtras(bundle);
        context.startActivity(intent);
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
                return playlistList.size();
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
