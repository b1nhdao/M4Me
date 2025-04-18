package com.example.m4me.model;

import java.util.Date;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;


public class Comment {
    private String ID;
    private String UserName;
    private String Content;
    @ServerTimestamp
    private Timestamp timestamp;


    public Comment(){

    }


    public String getID() {
        return ID;
    }

    public void setID(String ID) {
        this.ID = ID;
    }

    public String getUserName() {
        return UserName;
    }

    public void setUserName(String userName) {
        UserName = userName;
    }

    public String getContent() {
        return Content;
    }

    public void setContent(String content) {
        Content = content;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public String getFormattedDate() {
        Date date = timestamp != null ? timestamp.toDate() : new Date();
        return android.text.format.DateFormat.format("MMM dd, yyyy hh:mm a", date).toString();
    }
}
