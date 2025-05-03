package com.example.m4me.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.m4me.R;

import java.util.List;

public class TagAdapter extends RecyclerView.Adapter<TagAdapter.MyViewHolder> {

    Context context;
    List<String> tagNameList;

    public TagAdapter(Context context, List<String> tagNameList) {
        this.context = context;
        this.tagNameList = tagNameList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.tag_item_horizontally, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        String tagName = tagNameList.get(position);
        holder.tv_tag.setText(tagName);

        holder.tv_tag.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                TODO: open search fragment and show all the songs with this tagName
                Toast.makeText(context, tagName, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return tagNameList.size();
    }

    class MyViewHolder extends RecyclerView.ViewHolder {
        TextView tv_tag;
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            tv_tag = itemView.findViewById(R.id.tv_tag);
        }
    }
}
