package com.cloudfridge;

import android.content.Intent;
import android.os.Bundle;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.View;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        EditText editTextUsername = findViewById(R.id.editTextUsername);
        EditText editTextPassword = findViewById(R.id.editTextPassword);
        Button buttonSignIn = findViewById(R.id.buttonSignIn);
        Button buttonSignUp = findViewById(R.id.buttonSignUp);

        buttonSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(editTextUsername.getText().toString().isEmpty() || editTextPassword.getText().toString().isEmpty()){
                    Toast.makeText(LoginActivity.this, "Please enter your Username and Password", Toast.LENGTH_SHORT).show();
                }
                else{
                    loginUser(editTextUsername.getText().toString(),editTextPassword.getText().toString());
                }
            }
        });

        buttonSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(editTextUsername.getText().toString().isEmpty() || editTextPassword.getText().toString().isEmpty()){
                    Toast.makeText(LoginActivity.this, "Please enter your Username and Password", Toast.LENGTH_SHORT).show();
                }
                else{
                    registerUser(editTextUsername.getText().toString(),editTextPassword.getText().toString());
                }
            }
        });
    }

    public void loginUser(String username, String password){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Users")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                if (document.getId().equals(username)) {
                                    if (password.equals(document.get("password").toString())) {
                                        Log.d(TAG,"Logging in user " + username);
                                        //TODO: Store user info in Shared preferences
                                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                                        startActivity(intent);
                                    }
                                    else {
                                        Toast.makeText(LoginActivity.this, "Incorrect Password.\nPlease try again.", Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    Log.d(TAG,document.getId() + " => " + document.getData());
                                }
                            }
                        } else {
                            Log.w(TAG, "Error getting documents", task.getException());
                        }
                    }
                });
    }

    public void registerUser(String username, String password){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        // Check if user already exists
        db.collection("Users")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                if (document.getId().equals(username)) {
                                    Toast.makeText(LoginActivity.this, "User already exists. \nPlease enter a different username.", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                            }
                        } else {
                            Log.w(TAG, "Error getting documents", task.getException());
                        }
                    }
                });

        // Add new user
        Map<String, Object> newUser = new HashMap<>();
        newUser.put("password", password);

        db.collection("Users").document(username)
                .set(newUser)
                .addOnSuccessListener(documentReference -> Log.d(TAG, "DocumentSnapshot added"))
                .addOnFailureListener(e -> Log.w(TAG, "Error writing document", e));

        //TODO: Store user info in Shared preferences
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent);
    }
}