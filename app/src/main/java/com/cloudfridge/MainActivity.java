package com.cloudfridge;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    static final int REQUEST_IMAGE_CAPTURE = 1;

    private RecyclerView recyclerView;
    private ExampleAdapter adapter;
    private RecyclerView.LayoutManager layoutManager;
    private ArrayList<ExampleItem> exampleList = new ArrayList<>();

    private String currentPhotoPath;
    private String username;
    private  UserData data;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dispatchTakePictureIntent();
            }
        });

        username = getIntent().getStringExtra("username");

        try {
            getUserInfo();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        buildRecyclerView();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        if (id == R.id.action_logout) {
            Log.d(TAG, "Logging out user " + getIntent().getStringExtra("username"));
            Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    private File createImageFile() throws IOException {
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        String list[] = storageDir.list();
        Log.d("FILES", String.valueOf(list.length));
        if (list != null) {
            for (int i = 0; i < list.length; ++i) {
                Log.d("FILE:", "/" + list[i]);
            }
        }

        File image = File.createTempFile("Cloud-Fridge", ".jpg", storageDir);

        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this, this.getApplicationContext().getPackageName() + ".provider", photoFile);
                takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            File uploadFile = new File(storageDir, currentPhotoPath);
            Log.d(TAG, "Upload File: " + uploadFile.getAbsoluteFile());
            try {
                Bitmap imageBitmap = BitmapFactory.decodeFile(currentPhotoPath);
                Log.d("BITMAP", "Width: " + imageBitmap.getWidth());
                Log.d("BITMAP", "Height: " + imageBitmap.getHeight());

                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                String attachmentName = "bitmap";
                String attachmentFileName = timeStamp + ".png";
                String crlf = "\r\n";
                String twoHyphens = "--";
                String boundary =  "*****";

                URL url = new URL("https://us-central1-cloud-fridge.cloudfunctions.net/app/api/upload/" + username);
                HttpURLConnection httpUrlConnection = (HttpURLConnection) url.openConnection();
                httpUrlConnection.setUseCaches(false);
                httpUrlConnection.setDoOutput(true);

                httpUrlConnection.setRequestMethod("POST");
                httpUrlConnection.setRequestProperty("Connection", "Keep-Alive");
                httpUrlConnection.setRequestProperty("Cache-Control", "no-cache");
                httpUrlConnection.setRequestProperty(
                        "Content-Type", "multipart/form-data;boundary=" + boundary);

                DataOutputStream request = new DataOutputStream(
                        httpUrlConnection.getOutputStream());

                request.writeBytes(twoHyphens + boundary + crlf);
                request.writeBytes("Content-Disposition: form-data; name=\"" +
                        attachmentName + "\";filename=\"" +
                        attachmentFileName + "\"" + crlf);
                request.writeBytes(crlf);

                ByteArrayOutputStream bao = new ByteArrayOutputStream();
                imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, bao);
                byte[] pixels = bao.toByteArray();

                request.write(pixels);
                request.writeBytes(crlf);
                request.writeBytes(twoHyphens + boundary +
                        twoHyphens + crlf);
                request.flush();
                request.close();

                InputStream responseStream = new
                        BufferedInputStream(httpUrlConnection.getInputStream());

                BufferedReader responseStreamReader =
                        new BufferedReader(new InputStreamReader(responseStream));

                String line = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ((line = responseStreamReader.readLine()) != null) {
                    stringBuilder.append(line).append("\n");
                }
                String response = stringBuilder.toString();
                Log.d("RESPONSE", response);

                responseStreamReader.close();
                responseStream.close();
                httpUrlConnection.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (storageDir.isDirectory()) {
                String list[] = storageDir.list();
                Log.d("FILES", String.valueOf(list.length));

                if (list != null) {
                    for (int i = 0; i < list.length; ++i) {
                        new File(storageDir, list[i]).delete();
                    }
                }
            }
        }
    }

    public void buildRecyclerView() {
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        adapter = new ExampleAdapter(exampleList);

        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener( new ExampleAdapter.OnItemClickListener() {
            @Override
            public void onDeleteClick(int position) throws JSONException, IOException {
                exampleList.remove(position);
                adapter.notifyItemRemoved(position);

                // Update the database
                JSONObject obj = new JSONObject();
                JSONArray contents = new JSONArray();

                for(ExampleItem exampleItem: exampleList) {
                    JSONObject tmp = new JSONObject();
                    tmp.put(exampleItem.getText1(), exampleItem.getText2());
                    contents.put(tmp);
                }

                String url = "https://us-central1-cloud-fridge.cloudfunctions.net/app/api/update/" + username;
                URL urlObj = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) urlObj.openConnection();

                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestMethod("PUT");

                obj.put("name", username);
                obj.put("password", data.getPassword());
                obj.put("fridge_contents", contents);

                OutputStreamWriter wr = new OutputStreamWriter(connection.getOutputStream());
                wr.write(obj.toString());
                wr.flush();
                Log.d(TAG, "Send 'HTTP POST' request to : " + url);

                Integer httpResult = connection.getResponseCode();
                Log.d(TAG, httpResult.toString());

                if(httpResult == 500) {
                    Toast.makeText(MainActivity.this, "Request to server failed.", Toast.LENGTH_SHORT).show();
                }
                else {
                    Log.d(TAG, "Updated user: " + username);
                }
            }
        });
    }

    public void getUserInfo() throws IOException {
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
            Gson gson = new Gson();
            data = gson.fromJson(response.toString(), UserData.class);

            for(int i = 0; i < data.getFridge_contents().length; i++) {
                for (Map.Entry<String, Object> entry : data.getFridge_contents()[i].entrySet()){
                    int img_res;
                    switch (entry.getKey()){
                        case "Apple": img_res = R.drawable.apple;
                            break;
                        case "Orange": img_res = R.drawable.orange;
                            break;
                        case "Beef": img_res = R.drawable.beef;
                            break;
                        case "Chicken": img_res = R.drawable.chicken;
                            break;
                        default: img_res = R.drawable.common_google_signin_btn_icon_dark_normal;
                            break;
                    }

                    exampleList.add(new ExampleItem(img_res, entry.getKey(), "Expires: " + entry.getValue().toString()));
                }
            }
        }
    }
}