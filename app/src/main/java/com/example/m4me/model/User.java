package com.example.m4me.model;

import java.io.Serializable;
import java.util.List;

public class User implements Serializable {
    String email;
    String displayName;
    List<String> favouriteSongs;

    public User (){

    }

    public User(String email, String displayName) {
        this.email = email;
        this.displayName = displayName;
    }

    public List<String> getFavouriteSongs() {
        return favouriteSongs;
    }

    public void setFavouriteSongs(List<String> favouriteSongs) {
        this.favouriteSongs = favouriteSongs;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
