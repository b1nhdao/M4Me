package com.example.m4me.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.m4me.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    EditText edt_email, edt_displayName, edt_password, edt_passwordConfirm;
    Button btn_reginter;

    FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();

        mAuth = FirebaseAuth.getInstance();
        btn_reginter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = edt_email.getText().toString();
                String password = edt_password.getText().toString();
                String confirmPassword = edt_passwordConfirm.getText().toString();
                String displayName = edt_displayName.getText().toString();
                if (!email.isEmpty() && !password.isEmpty() && !displayName.isEmpty() && password.equals(confirmPassword)){
                    registerAccountFirebase(edt_email.getText().toString(), edt_password.getText().toString());
                    Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                    startActivity(intent);
                }
                else {
                    Toast.makeText(RegisterActivity.this, "hinh nhu co loi, kiem tra lai xem", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void initViews(){
        btn_reginter = findViewById(R.id.btn_reginter);
        edt_email = findViewById(R.id.edt_email);
        edt_displayName = findViewById(R.id.edt_displayName);
        edt_password = findViewById(R.id.edt_password);
        edt_passwordConfirm = findViewById(R.id.edt_passwordConfirm);
    }

    private void registerAccountFirebase(String email, String password){
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful()){
                    Log.d("CreateUser", "CreateUser:successful");
                    FirebaseUser user = mAuth.getCurrentUser();
                    UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                            .setDisplayName(edt_displayName.getText().toString())
                            .build();

                    user.updateProfile(profileUpdates)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        Log.d("update user profile", "User profile updated.");
                                        uploadDataToFirebaseFirestore(user);
                                    }
                                }
                            });
                    Toast.makeText(RegisterActivity.this, user.getEmail(), Toast.LENGTH_SHORT).show();
                }
                else {
                    Log.w("CreateUser", "CreateUser:failure", task.getException());
                    Toast.makeText(RegisterActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void uploadDataToFirebaseFirestore(FirebaseUser user){
        ArrayList<DocumentReference> tagRefs = new ArrayList<>();

        Map<String, Object> userIn = new HashMap<>();
        userIn.put("displayName", user.getDisplayName());
        userIn.put("email", user.getEmail());
        userIn.put("favouriteSongs", tagRefs);

        db.collection("users").document(user.getUid()).set(userIn).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                Log.d("Upload firebase", "DocumentSnapshot successfully written!");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.w("Upload firebase", "Error writing document", e);
            }
        });
    }
}