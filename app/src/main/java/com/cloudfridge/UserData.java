package com.cloudfridge;
import java.util.HashMap;

public class UserData {
    private String password;
    private HashMap<String, Object>[] fridge_contents;
    private HashMap<String, Object>[] upload_date;

    public UserData(String password, HashMap<String,Object>[] fridge_contents, HashMap<String,Object>[] upload_date){
        this.password = password;
        this. fridge_contents = fridge_contents;
        this.upload_date = upload_date;
    }

    public void updateFridgeContents(HashMap<String,Object>[] new_fridge_contents){
        this.fridge_contents = new_fridge_contents;
    }

    public String getPassword(){
        return this.password;
    }

    public HashMap<String,Object>[] getFridge_contents(){
        return this.fridge_contents;
    }

    public HashMap<String,Object>[] get_Upload_Dates(){
        return this.upload_date;
    }
}
