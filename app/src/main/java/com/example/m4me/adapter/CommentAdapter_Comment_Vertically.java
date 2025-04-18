package com.example.m4me.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.m4me.R;
import com.example.m4me.model.Comment;

import java.util.List;

public class CommentAdapter_Comment_Vertically extends RecyclerView.Adapter<CommentAdapter_Comment_Vertically.MyViewHolder> {

    private Context context;
    private List<Comment> commentList;

    public CommentAdapter_Comment_Vertically(Context context, List<Comment> commentList) {
        this.context = context;
        this.commentList = commentList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.comment_item_vertically, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Comment comment = commentList.get(position);
//        holder.img_avatar wont be touched cuz it not yet being developed (im lazy, it wont exist)
        holder.tv_userName.setText(comment.getUserName());
        holder.tv_content.setText(comment.getContent());
        holder.tv_timestamp.setText(comment.getFormattedDate());
    }

    @Override
    public int getItemCount() {
        return commentList.size();
    }

    class MyViewHolder extends RecyclerView.ViewHolder {
        ImageView img_avatar;
        TextView tv_userName, tv_timestamp, tv_content;
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            img_avatar = itemView.findViewById(R.id.img_avatar);
            tv_userName = itemView.findViewById(R.id.tv_userName);
            tv_timestamp = itemView.findViewById(R.id.tv_timestamp);
            tv_content = itemView.findViewById(R.id.tv_content);
        }
    }
}
