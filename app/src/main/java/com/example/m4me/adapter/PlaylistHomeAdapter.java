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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.m4me.R;
import com.example.m4me.activity.PlaylistActivity;
import com.example.m4me.model.Playlist;
import com.example.m4me.model.Tag;

import java.util.List;

public class PlaylistHomeAdapter extends RecyclerView.Adapter<PlaylistHomeAdapter.MyViewHolder> {

    private Context context;
    private List<Playlist> playlistList;
    private Tag tag;

    public PlaylistHomeAdapter(Context context, List<Playlist> playlistList) {
        this.context = context;
        this.playlistList = playlistList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.playlist_item_horizontally, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Playlist playlist = playlistList.get(position);
        if (playlist.getThumbnailURL() != null){
            Glide.with(context).load(playlist.getThumbnailURL()).into(holder.img_thumbnail);
        }
        holder.tv_playlistTitle.setText(shortenString(playlist.getTitle()));
        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, PlaylistActivity.class);
                Bundle bundle = new Bundle();
                bundle.putSerializable("object_playlist", playlist);
                intent.putExtras(bundle);
                context.startActivity(intent);
            }
        });
        List<String> tags = playlist.getTagNames();
        if (tags != null && !tags.isEmpty()) {
            TagAdapter tagAdapter = new TagAdapter(context, tags);
            LinearLayoutManager layoutManager = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);
            holder.rv_tags.setLayoutManager(layoutManager);
            holder.rv_tags.setAdapter(tagAdapter);
        } else {
            holder.rv_tags.setAdapter(null);
        }    }

    private String shortenString(String s){
        if (s.length() >= 32){
            return s.substring(0,32) + "...";
        }
        return s;
    }

    @Override
    public int getItemCount() {
        return playlistList.size();
    }

    class MyViewHolder extends RecyclerView.ViewHolder {
        private CardView cardView;
        private ImageView img_thumbnail;
        private TextView tv_playlistTitle;
        RecyclerView rv_tags;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            img_thumbnail = itemView.findViewById(R.id.img_thumbnail);
            tv_playlistTitle = itemView.findViewById(R.id.tv_playlistTitle);
            rv_tags = itemView.findViewById(R.id.rv_tags);
        }
    }
}
