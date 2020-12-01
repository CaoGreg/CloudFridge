package com.cloudfridge;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;

public class RecipeActivity extends AppCompatActivity {

    private static final String TAG = "RecipeActivity";
    private String username;

    private TextView m_Title;
    private TextView m_IngredientTitle;
    private TextView m_Ingredients;
    private TextView m_InstructionTitle;
    private TextView m_Instructions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe);

        username = getIntent().getStringExtra("username");

        m_Title = findViewById(R.id.title);
        m_IngredientTitle = findViewById(R.id.ingredient_title);
        m_InstructionTitle = findViewById(R.id.instruction_title);
        m_Ingredients = findViewById(R.id.ingredients);
        m_Instructions = findViewById(R.id.instructions);

        try {
            String response = HTTPGet();

            JSONObject obj = new JSONObject(response.trim());

            String ingredientsTitle = "Ingredients:";
            m_IngredientTitle.setText(ingredientsTitle);
            String instructionsTitles = "Instructions:";
            m_InstructionTitle.setText(instructionsTitles);

            String ingredients = new String();
            String instructions = new String();

            m_Title.setText(obj.get("title").toString() + "\n");

            for(Integer i = 1; i<= obj.getJSONObject("Steps").length(); i++) {
                instructions += "Step " + i + ": "
                        + obj.getJSONObject("Steps" ).getString(i.toString()) + "\n\n";
            }
            m_Instructions.setText(instructions);

            for(int i = 0; i<obj.names().length(); i++) {
                Log.d(TAG, "key = " + obj.names().getString(i) + " value = " + obj.get(obj.names().getString(i)));
                if (!obj.names().getString(i).equals("title") && !obj.names().getString(i).equals("Steps")) {
                    ingredients += obj.names().getString(i) + ": " +
                            obj.get(obj.names().getString(i)).toString() + "\n";
                }
            }
            m_Ingredients.setText(ingredients);

        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    String HTTPGet() throws IOException {
        String finalResponse = null;
        // Send HTTP Request
        String url = "https://us-central1-cloud-fridge.cloudfunctions.net/app/api/read/recipes/" + username;
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

    String cleanResponseString(HashSet<Character> removeCharacters, String string)
    {
        Character[] response = new Character[string.length()];
        int index = 0;
        for (int i = 0; i < string.length(); i++)
        {
            if (!removeCharacters.contains(string.charAt(i))) {
                response[index++] = string.charAt(i);
            }
        }
        char[] cleanResponse = new char[index - 1];
        Log.d(TAG, "Index: " + index);
        for (int i = 0; i < index - 1; i++) {
            cleanResponse[i] = response[i].charValue();
        }
        string = new String(cleanResponse);
        Log.d(TAG, "Clean String: " + string);
        return string;
    }
}