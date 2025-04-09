package com.example.m4me.fragment;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;

import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.example.m4me.R;
import com.example.m4me.activity.LoginActivity;
import com.example.m4me.activity.MainActivity;
import com.example.m4me.activity.SongPlayingActivity;
import com.example.m4me.activity.UploadActivity;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;


public class LibraryFragment extends Fragment {


    private CardView cardView_likedSongs, cardView_playlist, cardView_following, cardView_upload;
    private ImageView imgAvatar;
    private FirebaseAuth auth = FirebaseAuth.getInstance();
    private FirebaseFirestore db = MainActivity.db;
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

    @SuppressLint("MissingInflatedId")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_library, container, false);

        cardView_likedSongs = view.findViewById(R.id.cardView_likedSongs);
        cardView_playlist = view.findViewById(R.id.cardView_playlist);
        cardView_following = view.findViewById(R.id.cardView_following);
        cardView_upload = view.findViewById(R.id.cardView_upload);
        imgAvatar = view.findViewById(R.id.imgAvatar);
        cardView_upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), UploadActivity.class);
                getContext().startActivity(intent);
            }
        });
        cardView_playlist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Fragment playlistFragment = new PlaylistFragment();
                FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.replace(R.id.main, playlistFragment);
                fragmentTransaction.addToBackStack(null);
                fragmentTransaction.commit();
            }
        });
        cardView_likedSongs.setOnClickListener(new View.OnClickListener() {
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
        imgAvatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showOptionMenu();
            }
        });
        return view;
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