package com.example.onlineplaylists;


import java.util.ArrayList;
import java.util.HashMap;

public class Playlist {
    public String title;
    public int icon;
    private ArrayList<YouTubeVideo> videos;

    Playlist() {
        title = "";
        icon = R.drawable.baseline_featured_play_list_24;
        videos = new ArrayList<>();
    }

    Playlist(String _title) {
        title = _title;
        icon = R.drawable.baseline_featured_play_list_24;
        videos = new ArrayList<>();
    }

    Playlist(String _title, int _icon) {
        title = _title;
        icon = _icon;
        videos = new ArrayList<>();
    }

    public Playlist fromJson (String _json) {
        videos = new ArrayList<>();
        HashMap<String, Object> map = Json.toMap(_json);
        ArrayList<String> sourceList = Json.toList(map.get("videos").toString());
        title = map.get("title").toString();
        icon = map.containsKey("icon") ? Integer.parseInt(map.get("icon").toString()) : R.drawable.baseline_featured_play_list_24;
        YouTubeVideo ytv;

        for (String i: sourceList) {
            ytv = new YouTubeVideo().fromJson(i);
            videos.add(ytv);
        }

        return this;
    }

    public void addVideo(YouTubeVideo _video) {
        if (videos.isEmpty()) videos.add(_video); else videos.add(0, _video);
    }

    public void addVideoTo(YouTubeVideo _video, int to) {
        videos.add(to, _video);
    }

    public void removeVideo(int index) {
        videos.remove(index);
    }

    public void moveVideo(int from, int to) {
        if (from == to) return;
        if (from < to) to--;
        YouTubeVideo videoToMove = videos.get(from);
        videos.remove(from);
        if (videos.isEmpty()) videos.add(videoToMove); else videos.add(to, videoToMove);
    }

    public YouTubeVideo getVideoAt(int index) {
        return videos.get(index);
    }

    public int getLength() {
        return videos.size();
    }

    public String getJson(){
        HashMap<String, Object> map = new HashMap<>();
        ArrayList<String> list = new ArrayList<>();
        for (YouTubeVideo i : videos) {
            list.add(i.getJson());
        }
        map.put("title", title);
        map.put("icon", String.valueOf(icon));
        map.put("videos", Json.valueOf(list));
        return Json.valueOf(map);
    }
}
