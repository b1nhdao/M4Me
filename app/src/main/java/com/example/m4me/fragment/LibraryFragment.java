package com.example.m4me.fragment;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;

import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.m4me.R;
import com.example.m4me.activity.FavouriteSongsActivity;
import com.example.m4me.activity.PlaylistActivity;
import com.example.m4me.activity.PlaylistManagerActivity;
import com.example.m4me.activity.UploadSongActivity;
import com.example.m4me.model.Song;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.datatype.Artwork;

import java.io.File;
import java.io.FilenameFilter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link LibraryFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class LibraryFragment extends Fragment {


    private CardView cardView_likedSongs, cardView_playlist, cardView_following, cardView_upload, cardView_downloadedSong;

    private List<Song> downloadedSongList = new ArrayList<>();

    public LibraryFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment LibraryFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static LibraryFragment newInstance(String param1, String param2) {
        LibraryFragment fragment = new LibraryFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_library, container, false);

        cardView_likedSongs = view.findViewById(R.id.cardView_likedSongs);
        cardView_playlist = view.findViewById(R.id.cardView_playlist);
        cardView_following = view.findViewById(R.id.cardView_following);
        cardView_upload = view.findViewById(R.id.cardView_upload);
        cardView_downloadedSong = view.findViewById(R.id.cardView_downloadedSong);

        cardView_upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), UploadSongActivity.class);
                startActivity(intent);
            }
        });

        cardView_likedSongs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), FavouriteSongsActivity.class);
                startActivity(intent);
            }
        });


        cardView_playlist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), PlaylistManagerActivity.class);
                startActivity(intent);
            }
        });

        cardView_downloadedSong.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                downloadedSongList = getDownloadedSongs();

                Intent intent = new Intent(getContext(), PlaylistActivity.class);
                Bundle bundle = new Bundle();
                bundle.putSerializable("object_offline_playlist", (Serializable) downloadedSongList);
                intent.putExtras(bundle);
                startActivity(intent);
            }
        });
        return view;
    }

    private List<Song> getDownloadedSongs(){
        List<Song> downloadedSongList  = new ArrayList<>();

        File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

        File[] files = downloadDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".mp3");
            }
        });

        if (files != null) {
            for (File file : files) {
                try {
                    //  jAudiotagger to read metadata
                    AudioFile audioFile = AudioFileIO.read(file);
                    Tag tag = audioFile.getTag();

                    if (tag != null) {
                        Song song = new Song();

                        song.setTitle(tag.getFirst(FieldKey.TITLE));
                        song.setArtistName(tag.getFirst(FieldKey.ARTIST));
                        song.setFilePath(file.getAbsolutePath());

                        downloadedSongList.add(song);
                    }
                } catch (Exception e) {
                    Log.e("SongLoader", "Error reading audio file: " + file.getName(), e);

                    Song song = new Song();
                    song.setTitle(file.getName());
                    song.setFilePath(file.getAbsolutePath());
                    downloadedSongList.add(song);
                }
            }
        }
        return downloadedSongList;
    }
}