package com.example.onlineplaylists;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

public class Playlist {
    public String title;
    public int icon;
    private ArrayList<YouTubeVideo> videos;

    Playlist() {
        title = "";
        icon = 0;
        videos = new ArrayList<>();
    }

    Playlist(String _title) {
        title = _title;
        icon = 0;
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

    public void removeVideos(ArrayList<Integer> indexes) {
        indexes.sort(Comparator.reverseOrder());
        for (Integer i : indexes) videos.remove((int) i);
    }

    public void moveVideoDep(int from, int to) {
        if (from == to) return;
        if (from < to) to--;
        YouTubeVideo videoToMove = videos.get(from);
        videos.remove(from);
        if (videos.isEmpty()) videos.add(videoToMove); else videos.add(to, videoToMove);
    }

    public void moveVideo(int from, int to) {
        if (from < to) {
            for (int i = from; i < to; i++) {
                Collections.swap(videos, i, i + 1);
            }
        } else {
            for (int i = from; i > to; i--) {
                Collections.swap(videos, i, i - 1);
            }
        }
    }

    public YouTubeVideo getVideoAt(int index) {
        return videos.get(index);
    }

    public int getIndexOf(YouTubeVideo video) {
        return videos.indexOf(video);
    }

    public int getLength() {
        return videos.size();
    }

    public boolean contains(YouTubeVideo _video) {
        return contains(_video.id);
    }

    public boolean contains(String _id) {
        boolean _contains = false;
        for (YouTubeVideo i : videos) {
            if (i.id.equals(_id)) {
                _contains = true;
                break;
            }
        }
        return _contains;
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

    public boolean isEmpty() {
        return videos.isEmpty();
    }
}
