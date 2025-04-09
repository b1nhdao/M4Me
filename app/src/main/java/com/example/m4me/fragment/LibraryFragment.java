package com.example.m4me.fragment;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.example.m4me.R;
import com.example.m4me.activity.LoginActivity;
import com.example.m4me.activity.MainActivity;
import com.example.m4me.adapter.PlaylistAdapter_Home_Horizontally;
import com.example.m4me.model.Playlist;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class LibraryFragment extends Fragment {
    private LinearLayout llLikedSong, llYourUpload;
    ImageView imgAvatar;
    private ConstraintLayout layoutThemPlaylist;
    private RecyclerView rvPlaylist;
    private PlaylistAdapter_Home_Horizontally adapter;
    private List<Playlist> playlists = new ArrayList<>();
    private FirebaseFirestore db = MainActivity.db;
    private FirebaseAuth auth = FirebaseAuth.getInstance();
    private String displayName ="";

    public LibraryFragment() {
        // Required empty public constructor
    }

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
        llLikedSong = view.findViewById(R.id.llLikedSong);
        llYourUpload = view.findViewById(R.id.llYourUpload);
        imgAvatar = view.findViewById(R.id.imgAvatar);
        layoutThemPlaylist = view.findViewById(R.id.layoutThemPlayList);
        rvPlaylist = view.findViewById(R.id.rv_playList);
        rvPlaylist.setLayoutManager(new GridLayoutManager(getContext(),2,GridLayoutManager.VERTICAL,false));
        adapter = new PlaylistAdapter_Home_Horizontally(getContext(),playlists);
        rvPlaylist.setAdapter(adapter);
        getPlaylistsFromDatabase();
        llLikedSong.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Fragment likedSongFragment = new LikedSongFragment();
                FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.replace(R.id.main, likedSongFragment);
                fragmentTransaction.addToBackStack(null);
                fragmentTransaction.commit();

            }
        });
        layoutThemPlaylist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialogAddPlaylist();
            }
        });
        imgAvatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showOptionMenu();
            }
        });
        return  view;

    }
    private void showOptionMenu() {
        if (auth.getCurrentUser() != null) {
            String userId = auth.getCurrentUser().getUid();
            db.collection("users").document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            displayName = documentSnapshot.getString("displayName");
                        }
                        buildOptionMenu();
                    })
                    .addOnFailureListener(e -> {
                        Log.e("Library",e.getMessage());
                    });
        }
    }
    private void getPlaylistsFromDatabase() {
        db.collection("playlists")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Playlist playlist = document.toObject(Playlist.class);
                            DocumentReference tagRef = document.getDocumentReference("Tag");
                            if (tagRef != null){
                                tagRef.get().addOnSuccessListener(snapshot -> {
                                    if(snapshot.exists()){
                                        String tagName = snapshot.getString("Name");
                                        playlist.setTagName(tagName);
                                    }
                                    adapter.notifyDataSetChanged();
                                });
                            } else {
                                adapter.notifyDataSetChanged();
                            }

                            playlists.add(playlist);
                        }
                    } else {
                        Log.w("GetPlaylistsLibrary", "Error getting documents.", task.getException());
                    }
                });
    }
    private void showDialogAddPlaylist() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_play_list, null);
        EditText txt = dialogView.findViewById(R.id.edtInputTenPlaylist);

        builder.setView(dialogView)
                .setPositiveButton("Tạo", null)
                .setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String tenPlaylist = txt.getText().toString().trim();
            if (tenPlaylist.isEmpty()) {
                txt.setError("Vui lòng nhập tên playlist");
                return;
            }

            // Tạo ID mới
            DocumentReference newPlaylistRef = db.collection("playlists").document();
            String id = newPlaylistRef.getId();

            // Tạo object Playlist mới
            Playlist newPlaylist = new Playlist();
            newPlaylist.setID(id);
            newPlaylist.setTitle(tenPlaylist);
            newPlaylist.setThumbnailURL("https://thantrieu.com/resources/arts/1121429554.webp");
            newPlaylist.setSongIDs(new ArrayList<>());
            newPlaylist.setTagName("");
            newPlaylistRef.set(newPlaylist)
                    .addOnSuccessListener(unused -> {
                        playlists.add(newPlaylist);
                        adapter.notifyItemInserted(playlists.size() - 1);
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> {
                        Log.e("AddPlaylist", "Lỗi khi thêm playlist", e);
                    });
        });
    }
    private void buildOptionMenu() {
        PopupMenu optionMenu = new PopupMenu(getContext(), imgAvatar);
        optionMenu.getMenu().add("Hello, "+displayName).setEnabled(false); // Hiển thị tên, nhưng không cho phép click
        optionMenu.getMenu().add("Đăng xuất");
        optionMenu.getMenu().add("Đổi mật khẩu");

        optionMenu.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();
            if (title.equals("Đăng xuất")) {
                logout();
                return true;
            } else if (title.equals("Đổi mật khẩu")) {
                changPassword();
                return true;
            }
            return false;
        });

        optionMenu.show();
    }
    private void logout(){
        auth.signOut();
        Toast.makeText(getContext(), "Đã đăng xuất", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        getActivity().finish();
    }
    private void changPassword(){
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_change_password, null);
        builder.setView(dialogView);

        EditText etCurrentPassword = dialogView.findViewById(R.id.edtCurrentPassword);
        EditText etNewPassword = dialogView.findViewById(R.id.edtNewPassword);
        EditText etConfirmPassword = dialogView.findViewById(R.id.edtConfirmPassword);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnConfirm = dialogView.findViewById(R.id.btnConfirm);

        AlertDialog dialog = builder.create();
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            String currentPassword = etCurrentPassword.getText().toString().trim();
            String newPassword = etNewPassword.getText().toString().trim();
            String confirmPassword = etConfirmPassword.getText().toString().trim();

            // Kiểm tra dữ liệu nhập vào
            if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                Toast.makeText(getContext(), "Mật khẩu mới và xác nhận không khớp", Toast.LENGTH_SHORT).show();
                return;
            }

            if (newPassword.length() < 6) {
                Toast.makeText(getContext(), "Mật khẩu mới phải có ít nhất 6 ký tự", Toast.LENGTH_SHORT).show();
                return;
            }

            // Xác thực lại người dùng trước khi đổi mật khẩu
            FirebaseUser user = auth.getCurrentUser();
            if (user != null && user.getEmail() != null) {
                AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);
                user.reauthenticate(credential)
                        .addOnSuccessListener(aVoid -> {
                            user.updatePassword(newPassword)
                                    .addOnSuccessListener(aVoid1 -> {
                                        Toast.makeText(getContext(), "Đổi mật khẩu thành công", Toast.LENGTH_SHORT).show();
                                        dialog.dismiss();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(getContext(), "Đổi mật khẩu thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(getContext(), "Mật khẩu hiện tại không đúng", Toast.LENGTH_SHORT).show();
                        });
            }
        });
        dialog.show();
    }


}