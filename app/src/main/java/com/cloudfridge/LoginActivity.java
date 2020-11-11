package com.cloudfridge;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.auth.User;
import com.google.gson.Gson;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.StrictMode;
import android.util.Log;
import android.view.View;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.net.URLConnection;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

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
                    try {
                        loginUser(editTextUsername.getText().toString(),editTextPassword.getText().toString());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
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
                    try {
                        registerUser(editTextUsername.getText().toString(),editTextPassword.getText().toString());
                    } catch (IOException | JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    public void loginUser(String username, String password) throws IOException {
        // Send HTTP Request
        String url = "https://us-central1-cloud-fridge.cloudfunctions.net/app/api/read/" + username;
        URL urlObj = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) urlObj.openConnection();

        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");

        Log.d(TAG, "Send 'HTTP GET' request to : " + url);
        Integer responseCode = connection.getResponseCode();
        Log.d(TAG, "Response Code : " + responseCode);

        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader inputReader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = inputReader.readLine()) != null) {
                response.append(inputLine);
            }

            inputReader.close();

            if (response.toString().equals("{}")){
                Toast.makeText(LoginActivity.this, "User does not exist. \nPlease try again.", Toast.LENGTH_SHORT).show();
                return;
            }

            Log.d(TAG, "Response: " + response.toString());
            Gson gson = new Gson();
            UserData data = gson.fromJson(response.toString(),UserData.class);

            // Validate the password
            AESCrypt aesCrypt = new AESCrypt();
            try {
                String passwordHash = aesCrypt.encrypt(password);

                if (passwordHash.replaceAll("\\n", "").equals(data.getPassword().replaceAll("\\n", ""))) {
                    Log.d(TAG, "Logging in user " + username);

                    // Go to MainActivity
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    intent.putExtra("username", username);
                    startActivity(intent);
                }
                else {
                    Toast.makeText(LoginActivity.this, "Incorrect Password.\nPlease try again.", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void registerUser(String username, String password) throws IOException, JSONException {
        // Send HTTP Request
        String url = "https://us-central1-cloud-fridge.cloudfunctions.net/app/api/read/" + username;
        URL urlObj = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) urlObj.openConnection();

        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");

        Log.d(TAG, "Send 'HTTP GET' request to : " + url);
        Integer responseCode = connection.getResponseCode();
        Log.d(TAG, "Response Code : " + responseCode);

        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader inputReader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = inputReader.readLine()) != null) {
                response.append(inputLine);
            }
            inputReader.close();

            Log.d(TAG, "Response: " + response.toString());
            if (!response.toString().equals("{}")){
                Toast.makeText(LoginActivity.this, "User already exists. \nPlease enter a different username.", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        url = "https://us-central1-cloud-fridge.cloudfunctions.net/app/api/create";
        urlObj = new URL(url);
        connection = (HttpURLConnection) urlObj.openConnection();

        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestMethod("POST");

        JSONObject obj = new JSONObject();
        JSONArray contents = new JSONArray();

        AESCrypt aesCrypt = new AESCrypt();
        try {
            String passwordHash = aesCrypt.encrypt(password);

            obj.put("name", username);
            obj.put("password", passwordHash);
            obj.put("fridge_contents", contents);

            OutputStreamWriter wr = new OutputStreamWriter(connection.getOutputStream());
            wr.write(obj.toString());
            wr.flush();
            Log.d(TAG, "Send 'HTTP POST' request to : " + url);

            Integer httpResult = connection.getResponseCode();
            Log.d(TAG, httpResult.toString());

            if(httpResult == 500) {
                Toast.makeText(LoginActivity.this, "Request to server failed.", Toast.LENGTH_SHORT).show();
            }
            else {
                Log.d(TAG, "Logging in user " + username);

                // Go to MainActivity
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.putExtra("username", username);
                startActivity(intent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
