package com.cloudfridge;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;

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
import java.net.ProtocolException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    static final int REQUEST_IMAGE_CAPTURE = 1;
    public static Dialog dialog;

    private RecyclerView recyclerView;
    private ExampleAdapter adapter;
    private RecyclerView.LayoutManager layoutManager;
    private ArrayList<ExampleItem> exampleList;

    private String currentPhotoPath;
    private String username;
    private  UserData data;

    private String m_Response;
    private String m_ImageName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        exampleList = new ArrayList<>();

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { dispatchTakePictureIntent(); }
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

        if (id == R.id.action_generate_recipes) {
            Log.d(TAG, "Generating recipes for " + username);
            Intent intent = new Intent(getApplicationContext(), RecipeActivity.class);
            intent.putExtra("username", username);
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
                m_ImageName = timeStamp + ".png";
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
                        m_ImageName + "\"" + crlf);
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

                InputStream responseStream = new BufferedInputStream(httpUrlConnection.getInputStream());

                BufferedReader responseStreamReader =
                        new BufferedReader(new InputStreamReader(responseStream));

                String line = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ((line = responseStreamReader.readLine()) != null) {
                    stringBuilder.append(line).append("\n");
                }
                m_Response = stringBuilder.toString();
                Log.d("RESPONSE", m_Response);
                parseResponseToArray();

                responseStreamReader.close();
                responseStream.close();
                httpUrlConnection.disconnect();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (ProtocolException e) {
                e.printStackTrace();
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
            showDialog(MainActivity.this);
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
                JSONArray dates = new JSONArray();

                for(ExampleItem exampleItem: exampleList) {
                    JSONObject tmp = new JSONObject();
                    JSONObject date = new JSONObject();
                    String[] rawArray = exampleItem.getText2().split("[ ]");
                    tmp.put(exampleItem.getText1(), rawArray[1]);
                    date.put(exampleItem.getText1(), exampleItem.getDate());
                    contents.put(tmp);
                    dates.put(date);
                }
                updateRequest(obj, contents, dates);
            }
        });
    }

    public void getUserInfo() throws IOException {
        exampleList.clear();

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

            HashMap<String, Drawable> urls = new HashMap<>();
            for(int i = 0; i < data.getFridge_contents().length; i++) {
                ArrayList<String> keys = new ArrayList<>(data.getFridge_contents()[i].keySet());
                for (String key : keys) {
                    String imageURL = "https://us-central1-cloud-fridge.cloudfunctions.net/app/api/read/dates/" +
                            username + "/" + data.get_Upload_Dates()[i].get(key).toString();
                    if (urls.get(url) == null) {
                        Log.d(TAG, "Image: " + imageURL);
                        Drawable drawable = LoadImageFromWebOperations(imageURL);
                        urls.put(imageURL, drawable);
                        exampleList.add(new ExampleItem(drawable, key, "Expires: " + data.getFridge_contents()[i].get(key).toString(),
                                data.get_Upload_Dates()[i].get(key).toString()));
                    }
                    else
                    {
                        exampleList.add(new ExampleItem(urls.get(url), key, "Expires: " + data.getFridge_contents()[i].get(key).toString(),
                                data.get_Upload_Dates()[i].get(key).toString()));
                    }
                }
            }
        }
    }

    public void showDialog(Activity activity) {
        dialog = new Dialog(activity);
        // dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setContentView(R.layout.dialog_recycler);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int displayWidth = displayMetrics.widthPixels;
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.copyFrom(dialog.getWindow().getAttributes());
        int dialogWindowWidth = (int) (displayWidth * 0.99f);
        layoutParams.width = dialogWindowWidth;
        dialog.getWindow().setAttributes(layoutParams);

        //TODO: Pass data here from clarifai api call
        RecyclerView recyclerView = dialog.findViewById(R.id.recycler);
        DialogAdapter dialogAdapter = new DialogAdapter(MainActivity.this, parseResponseToArray());
        recyclerView.setAdapter(dialogAdapter);

        Button buttonCancel = dialog.findViewById(R.id.btnCancel);
        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        Button buttonOk = dialog.findViewById(R.id.btnOK);
        buttonOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ArrayList<FoodModel> results = dialogAdapter.getSelectedItems();
                if (!results.isEmpty())
                {
                    try {
                        String response = HTTPGet();
                        if (response != null) {
                            JSONObject obj = new JSONObject(response);
                            JSONArray contents = obj.getJSONArray("fridge_contents");
                            JSONArray dates = obj.getJSONArray("upload_date");
                            Log.d(TAG, "Received Food: " + contents.toString());
                            Log.d(TAG, "Received Dates: " + dates.toString());

                            for (FoodModel food : results) {
                                JSONObject tmp = new JSONObject();
                                JSONObject date = new JSONObject();

                                try {
                                    tmp.put(food.getName(), food.getExpiryDate());
                                    date.put(food.getName(), m_ImageName);
                                    contents.put(tmp);
                                    dates.put(date);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }

                            try {
                                Log.d(TAG, "Sent Food: " + contents.toString());
                                Log.d(TAG, "Sent Dates: " + dates.toString());
                                updateRequest(obj, contents, dates);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    } catch (IOException | JSONException e) {
                        e.printStackTrace();
                    }

                    try {
                        getUserInfo();
                        buildRecyclerView();
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    dialog.dismiss();
                }
                else
                {
                    Toast.makeText(MainActivity.this, "Please select an item", Toast.LENGTH_SHORT).show();
                }
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext(), LinearLayoutManager.VERTICAL, false));

        recyclerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        dialog.show();
    }

    void cleanResponseString(HashSet<Character> removeCharacters)
    {
        Character[] response = new Character[m_Response.length()];
        int index = 0;
        for (int i = 0; i < m_Response.length(); i++)
        {
            if (!removeCharacters.contains(m_Response.charAt(i))) {
                response[index++] = m_Response.charAt(i);
            }
        }
        char[] cleanResponse = new char[index - 1];
        Log.d(TAG, "Index: " + index);
        for (int i = 0; i < index - 1; i++) {
            cleanResponse[i] = response[i].charValue();
        }
        m_Response = new String(cleanResponse);
        Log.d(TAG, "Clean Response: " + m_Response);
    }

    String[] parseResponseToArray()
    {
        HashSet<Character> removeCharacters = new HashSet<Character>();
        removeCharacters.add('{');
        removeCharacters.add('"');
        removeCharacters.add('}');
        cleanResponseString(removeCharacters);

        String[] rawArray = m_Response.split("[|]");
        String[] messageArray = new String[rawArray.length - 1];

        rawArray[rawArray.length - 1] = rawArray[rawArray.length - 1].substring(0, rawArray[rawArray.length - 1].length() - 3);
        for (int i = 1; i < rawArray.length; i++) {
            messageArray[i - 1] = rawArray[i];
            Log.d(TAG, "Response: " + messageArray[i - 1]);
        }
        ArrayList<String> labels = new ArrayList<>();
        for (int i = 0; i < messageArray.length; i++) {
            int index = messageArray[i].indexOf(':');
            float accuracy = Float.parseFloat(messageArray[i].substring(index + 1, messageArray[i].length() - 1));
            if (Float.compare(accuracy, 0.9f) >= 0)
                labels.add(messageArray[i].substring(0, index));
            else
                break;
        }

        String[] responseLabels = new String[labels.size()];
        for (int i = 0; i < labels.size(); i++) {
            responseLabels[i] = labels.get(i);
            Log.d(TAG, "Response Label: " + responseLabels[i]);
        }
        return responseLabels;
    }

    void updateRequest(JSONObject obj, JSONArray contents, JSONArray dates) throws JSONException, IOException {
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
        obj.put("upload_date", dates);

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

    String HTTPGet() throws IOException {
        String finalResponse = null;
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

            finalResponse = response.toString();
            Log.d(TAG, "GET Response: " + finalResponse);
        }
        return finalResponse;
    }

    public static Drawable LoadImageFromWebOperations(String url) {
        try {
            InputStream is = (InputStream) new URL(url).getContent();
            Drawable d = Drawable.createFromStream(is, "src name");
            return d;
        } catch (Exception e) {
            return null;
        }
    }
}

