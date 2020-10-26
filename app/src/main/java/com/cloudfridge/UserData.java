package com.cloudfridge;

import java.util.HashMap;
import java.util.Map;

public class UserData {
    String password;
    Map<String, Map<String,Object>> fridge_contents = new HashMap<>();

    public UserData(String password, Map<String, Map<String,Object>> fridge_contents){
        this.password = password;
        this. fridge_contents = fridge_contents;
    }

    public void updateFridgeContents(Map<String, Map<String,Object>> new_fridge_contents){
        this.fridge_contents = new_fridge_contents;
    }

    public String getPassword(){
        return this.password;
    }

    public Map<String, Map<String,Object>> getFridge_contents(){
        return this.fridge_contents;
    }
}
