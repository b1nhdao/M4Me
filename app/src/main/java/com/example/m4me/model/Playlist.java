package com.example.m4me.model;

import java.io.Serializable;
import java.util.List;

public class Playlist implements Serializable {
    String ID;
    String Title;
    String ThumbnailURL;
    List<String> SongIDs;
    String TagName;
    List<String> TagNames;
    String CreatorName;

    public Playlist(){

    }

    public Playlist(String ID, String title, String thumbnailURL, List<String> songIDs, String tagName, List<String> tagNames) {
        this.ID = ID;
        Title = title;
        ThumbnailURL = thumbnailURL;
        SongIDs = songIDs;
        TagName = tagName;
        TagNames = tagNames;
    }

    public String getCreatorName() {
        return CreatorName;
    }

    public void setCreatorName(String creatorName) {
        CreatorName = creatorName;
    }

    public List<String> getTagNames() {
        return TagNames;
    }

    public void setTagNames(List<String> tagNames) {
        TagNames = tagNames;
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
