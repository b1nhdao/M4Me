package com.example.m4me.activity;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.m4me.R;
import com.example.m4me.fragment.HomeFragment;
import com.example.m4me.fragment.LibraryFragment;
import com.example.m4me.fragment.SearchFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    BottomNavigationView bottom_nav_view;
    public static FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();

        loadFragment(new HomeFragment());
        bottom_nav_view = findViewById(R.id.bottom_nav_view);

        bottom_nav_view.setOnItemSelectedListener(item -> {
            if(item.getItemId() == R.id.home){
                loadFragment(new HomeFragment());
                return true;
            }
            if(item.getItemId() == R.id.search){
                loadFragment(new SearchFragment());
                return true;
            }
            if(item.getItemId() == R.id.library){
                loadFragment(new LibraryFragment());
                return true;
            }
            return false;
        });
    }
    private void loadFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.main, fragment);
        fragmentTransaction.commit();
    }
}