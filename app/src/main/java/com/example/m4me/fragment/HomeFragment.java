package com.example.m4me.fragment;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.example.m4me.activity.MainActivity;
import com.example.m4me.R;
import com.example.m4me.adapter.SongAdapter_Home_Horizontally;
import com.example.m4me.model.Song;
import com.example.m4me.service.MusicService;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link HomeFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HomeFragment extends Fragment {

    private String mParam1;
    private String mParam2;
    private RecyclerView rv_song;
    private FirebaseFirestore db = MainActivity.db;
    private List<Song> songList = new ArrayList<>();
    Button btn_testService, btn_testServiceStop;

    public HomeFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment HomeFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static HomeFragment newInstance(String param1, String param2) {
        HomeFragment fragment = new HomeFragment();
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
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        rv_song = view.findViewById(R.id.rv_song);
        btn_testService = view.findViewById(R.id.btn_testService);
        btn_testServiceStop = view.findViewById(R.id.btn_testServiceStop);

        btn_testService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickStartService();
            }
        });

        btn_testServiceStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickStopService();
            }
        });
        
        songList = getSongsFromDatabase();

        rv_song.setLayoutManager(new GridLayoutManager(getContext(),3, RecyclerView.HORIZONTAL, false));
        SongAdapter_Home_Horizontally adapter = new SongAdapter_Home_Horizontally(getContext(), songList);
        rv_song.setAdapter(adapter);

        return view;
    }

    private void clickStartService(){
        Song song = new Song("title", "ca si", "https://i.scdn.co/image/ab67616d00001e02a1bc26cdd8eecd89da3adc39", "https://fupkxjtaokejquyotrgc.supabase.co/storage/v1/object/sign/songs/Dunglamtraitimanhdau.mp3?token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1cmwiOiJzb25ncy9EdW5nbGFtdHJhaXRpbWFuaGRhdS5tcDMiLCJpYXQiOjE3MzIwMDQ0NjksImV4cCI6MTc2MzU0MDQ2OX0.H0Sy82U2VBm1UQLMl-NbQRlMknAsL1q_k9z2_9Z3u0k&t=2024-11-19T08%3A21%3A22.287Z");
        Intent intent = new Intent(getActivity(), MusicService.class);
        Bundle bundle = new Bundle();
        bundle.putSerializable("object_song", song);
        intent.putExtras(bundle);
        getActivity().startService(intent);
    }

    private void clickStopService(){
        Intent intent = new Intent(getActivity(), MusicService.class);
        getActivity().stopService(intent);
    }

    private List<Song> getSongsFromDatabase(){
        List<Song> songGet = new ArrayList<>();
        db.collection("songs")
                .limit(6)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Log.d("GetSongs", document.getId() + " => " + document.getData());
                                // document to object, kinda similar jsonParse
                                Song song = document.toObject(Song.class);

                                // getting artist name with referenced attribute
                                DocumentReference artistRef = document.getDocumentReference("Artist");
                                if (artistRef != null){
                                    artistRef.get().addOnSuccessListener(snapshot -> {
                                        if(snapshot.exists()){
                                            String artistName = snapshot.getString("displayName");
                                            song.setArtistName(artistName);
                                        }
                                        // tbh, idk why i should put it here
                                        rv_song.getAdapter().notifyDataSetChanged();
                                    });
                                }
                                songGet.add(song);
                            }
                        } else {
                            Log.w("GetSongs", "Error getting documents.", task.getException());
                        }
                    }
                });
        return songGet;
    }
}