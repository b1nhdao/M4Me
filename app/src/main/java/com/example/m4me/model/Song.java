package com.example.m4me.model;

import java.io.Serializable;

public class Song implements Serializable {
    String ID;
    String Title;
    String ArtistName;
    int PlayedCounter;
    String ThumbnailUrl;
    String SourceURL;

    public Song(String ID, String title, String artistName, int playedCounter, String thumbnailUrl, String sourceURL) {
        this.ID = ID;
        Title = title;
        ArtistName = artistName;
        PlayedCounter = playedCounter;
        ThumbnailUrl = thumbnailUrl;
        SourceURL = sourceURL;
    }

    public Song(String title, String artistName, String thumbnailUrl) {
        Title = title;
        ArtistName = artistName;
        ThumbnailUrl = thumbnailUrl;
    }

    public Song(String title, String artistName, String thumbnailUrl, String sourceURL) {
        Title = title;
        ArtistName = artistName;
        ThumbnailUrl = thumbnailUrl;
        SourceURL = sourceURL;
    }

    public Song() {
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
}