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
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.m4me.R;
import com.example.m4me.model.Song;
import com.example.m4me.service.MusicService;

import java.util.List;

public class SongAdapter_Home_Horizontally extends RecyclerView.Adapter<SongAdapter_Home_Horizontally.MyViewHolder> {

    private Context context;
    private List<Song> listSong;

    public SongAdapter_Home_Horizontally(Context context, List<Song> listSong) {
        this.context = context;
        this.listSong = listSong;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.song_item_horizontally, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Song song = listSong.get(position);
        holder.tv_songTitle.setText(song.getTitle());
        holder.tv_artistName.setText(song.getArtistName());
        Glide.with(context)
                .load(song.getThumbnailUrl())
                .into(holder.img_thumbnail);
        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickStartService(song);
            }
        });
    }

    private void clickStartService(Song song){
        Intent intent = new Intent(context, MusicService.class);
        Bundle bundle = new Bundle();
        bundle.putSerializable("object_song", song);
        intent.putExtras(bundle);
        context.startService(intent);
    }

    @Override
    public int getItemCount() {
        return listSong.size();
    }

    class MyViewHolder extends RecyclerView.ViewHolder {
        private CardView cardView;
        private ImageView img_thumbnail;
        private TextView tv_songTitle, tv_artistName;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            img_thumbnail = itemView.findViewById(R.id.img_thumbnail);
            tv_songTitle = itemView.findViewById(R.id.tv_songTitle);
            tv_artistName = itemView.findViewById(R.id.tv_songArtist);
        }
    }
}
