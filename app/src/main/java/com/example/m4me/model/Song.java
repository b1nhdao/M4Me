package com.example.m4me.model;

import android.graphics.Bitmap;
import android.net.Uri;

import com.google.firebase.firestore.DocumentReference;

import java.io.File;
import java.io.Serializable;
import java.util.List;

public class Song implements Serializable {
    String ID;
    String Title;
    String ArtistName;
    int PlayedCounter;
    String ThumbnailUrl;
    String SourceURL;
    List<String> TagNames;
    boolean isFavourite = false;
    String filePath;
    Bitmap thumbnailBitmap;

    public Song(String ID, String title, String artistName, int playedCounter, String thumbnailUrl, String sourceURL, List<String> tagNames) {
        this.ID = ID;
        Title = title;
        ArtistName = artistName;
        PlayedCounter = playedCounter;
        ThumbnailUrl = thumbnailUrl;
        SourceURL = sourceURL;
        TagNames = tagNames;
    }

    public Song() {
    }

    public Bitmap getThumbnailBitmap() {
        return thumbnailBitmap;
    }

    public void setThumbnailBitmap(Bitmap thumbnailBitmap) {
        this.thumbnailBitmap = thumbnailBitmap;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public boolean isFavourite() {
        return isFavourite;
    }

    public void setFavourite(boolean favourite) {
        isFavourite = favourite;
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

    public String getArtistName() {
        return ArtistName;
    }

    public void setArtistName(String artistName) {
        ArtistName = artistName;
    }

    public int getPlayedCounter() {
        return PlayedCounter;
    }

    public void setPlayedCounter(int playedCounter) {
        PlayedCounter = playedCounter;
    }

    public String getThumbnailUrl() {
        return ThumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        ThumbnailUrl = thumbnailUrl;
    }

    public String getSourceURL() {
        return SourceURL;
    }

    public void setSourceURL(String sourceURL) {
        SourceURL = sourceURL;
    }

    public Uri getUri() {
        return Uri.fromFile(new File(filePath));
    }

    @Override
    public String toString() {
        return "Song{" +
                "ID='" + ID + '\'' +
                ", Title='" + Title + '\'' +
                ", ArtistName='" + ArtistName + '\'' +
                ", PlayedCounter=" + PlayedCounter +
                ", ThumbnailUrl='" + ThumbnailUrl + '\'' +
                ", SourceURL='" + SourceURL + '\'' +
                ", TagNames=" + TagNames +
                ", isFavourite=" + isFavourite +
                ", filePath='" + filePath + '\'' +
                ", thumbnailBitmap=" + thumbnailBitmap +
                '}';
    }
}