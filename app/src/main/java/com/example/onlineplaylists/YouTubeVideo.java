package com.example.onlineplaylists;

import java.util.HashMap;

public class YouTubeVideo {
    public String title;
    public String id;
    public int musicStartSeconds;

    YouTubeVideo() {
        title = "";
        id = "";
        musicStartSeconds = 0;
    }

    YouTubeVideo(String _title, String _id) {
        title = _title;
        id = _id;
        musicStartSeconds = 0;
    }

    public YouTubeVideo fromJson(String _json) {
        HashMap<String, Object> map = Json.toMap(_json);
        title = (String) map.get("title");
        id = (String) map.get("id");
        musicStartSeconds = (int)(double)(Double) map.getOrDefault("start", 0);
        return this;
    }

    public String getVideoUrl() {
        return "https://www.youtube.com/watch?v=".concat(id);
    }

    public String getThumbnailUrl() {
        return "https://img.youtube.com/vi/".concat(id).concat("/0.jpg");
    }

    public static String getVideoUrlFrom(String _id) {
        return "https://www.youtube.com/watch?v=".concat(_id);
    }

    public static String getVideoIdFrom(String _url) {
        return _url
                .replace("https://www.youtube.com/watch?v=","")
                .replace("https://m.youtube.com/watch?v=", "")
                .substring(0, 11);
    }

    public String getJson() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("title", title);
        map.put("id", id);
        map.put("start", musicStartSeconds);
        return Json.valueOf(map);
    }
}