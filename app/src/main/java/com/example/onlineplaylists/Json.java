package com.example.onlineplaylists;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.HashMap;

public class Json {
    Json() {
    }

    public static String valueOf(ArrayList<String> list) {
        Gson gson = new Gson();
        return gson.toJson(list);
    }

    public static String valueOf(HashMap<String, Object> map) {
        Gson gson = new Gson();
        return gson.toJson(map);
    }

    public static ArrayList<HashMap<String, Object>> toListMap(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, new TypeToken<ArrayList<HashMap<String, Object>>>(){});
    }

    public static ArrayList<String> toList(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, new TypeToken<ArrayList<String>>(){});
    }

    public static HashMap<String, Object> toMap(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, new TypeToken<HashMap<String, Object>>(){});
    }
}
