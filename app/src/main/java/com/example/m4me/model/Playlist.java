package com.example.m4me.model;

import java.io.Serializable;
import java.util.List;

public class Playlist implements Serializable {
    String ID;
    String Title;
    String ThumbnailURL;
    List<String> SongIDs;
    String TagName;

    public Playlist(){

    }

    public Playlist(String ID, String title, String thumbnailURL, List<String> SongIDs, String tagTitle) {
        this.ID = ID;
        Title = title;
        ThumbnailURL = thumbnailURL;
        this.SongIDs = SongIDs;
        TagName = tagTitle;
    }

    public String getID() {
        return ID;
    }

    public void setID(String ID) {
        this.ID = ID;
    }

    public String getTitle() {
        return Title;
    }

    public void setTitle(String title) {
        Title = title;
    }

    public String getThumbnailURL() {
        return ThumbnailURL;
    }

    public void setThumbnailURL(String thumbnailURL) {
        ThumbnailURL = thumbnailURL;
    }

    public List<String> getSongIDs() {
        return SongIDs;
    }

    public void setSongIDs(List<String> songIDs) {
        this.SongIDs = songIDs;
    }

    public String getTagName() {
        return TagName;
    }

    public void setTagName(String tagName) {
        TagName = tagName;
    }
}
