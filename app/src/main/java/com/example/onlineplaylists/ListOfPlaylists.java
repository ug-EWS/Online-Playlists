package com.example.onlineplaylists;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class ListOfPlaylists {
    ArrayList<Playlist> playlists;

    ListOfPlaylists(){
        playlists = new ArrayList<>();
    }

    public ListOfPlaylists fromJson(String _json) {
        playlists = new ArrayList<>();
        ArrayList<String> list = Json.toList(_json);

        for (String i : list) {
            playlists.add(new Playlist().fromJson(i));
        }

        return this;
    }

    public void addPlaylist(Playlist p) {
        if (playlists.isEmpty()) playlists.add(p); else playlists.add(0, p);
    }

    public void addPlaylistTo(Playlist p, int to) {
        if (playlists.isEmpty()) playlists.add(p);
        else if (playlists.size() < to) playlists.add(0, p);
        else playlists.add(to, p);
    }

    public Playlist getPlaylistAt(int index) {
        return playlists.get(index);
    }

    public int getIndexOf(Playlist playlist) {
        return playlists.indexOf(playlist);
    }

    public String getJson() {
        ArrayList<String> list = new ArrayList<>();
        for (Playlist i : playlists) {
            list.add(i.getJson());
        }
        return Json.valueOf(list);
    }

    public int getLength() {
        return playlists.size();
    }

    public void removePlaylist(int index) {
        playlists.remove(index);
    }

    public void removePlaylists(ArrayList<Integer> indexes) {
        indexes.sort(Comparator.reverseOrder());
        for (Integer i : indexes) playlists.remove((int) i);
    }

    public void movePlaylistDep(int from, int to) {
        if (from == to) return;
        if (from < to) to--;
        Playlist playlistToMove = playlists.get(from);
        playlists.remove(from);
        if (playlists.isEmpty()) playlists.add(playlistToMove); else playlists.add(to, playlistToMove);
    }

    public void movePlaylist(int from, int to) {
        if (from < to) {
            for (int i = from; i < to; i++) {
                Collections.swap(playlists, i, i + 1);
            }
        } else {
            for (int i = from; i > to; i--) {
                Collections.swap(playlists, i, i - 1);
            }
        }
    }

    public void moveVideo(int from, int video, int to, boolean cut) {
        Playlist fromPlaylist = playlists.get(from);
        YouTubeVideo videoToMove = fromPlaylist.getVideoAt(video);
        if (cut) fromPlaylist.removeVideo(video);
        Playlist toPlaylist = playlists.get(to);
        toPlaylist.addVideo(videoToMove);
    }

    public boolean isEmpty() {
        return playlists.isEmpty();
    }
}
