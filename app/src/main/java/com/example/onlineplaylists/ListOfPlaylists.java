package com.example.onlineplaylists;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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
        addPlaylistTo(p, 0);
    }

    public void addPlaylistTo(Playlist p, int to) {
        if (playlists.size() < to) to = 0;
        playlists.add(to, p);
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

    public void removePlaylists(@NonNull ArrayList<Integer> indexes) {
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

    public String mergePlaylists(@NonNull ArrayList<Integer> indexes) {
        indexes.sort(Comparator.naturalOrder());
        Playlist basePlaylist = playlists.get(indexes.get(0));
        for (int i = indexes.size() - 1; i > 0; i--) {
            Playlist playlist = playlists.get(indexes.get(i));
            for (int j = playlist.getLength() - 1; j >= 0; j--) {
                YouTubeVideo video = playlist.getVideoAt(j);
                if (!basePlaylist.contains(video))
                    basePlaylist.addVideo(video);
            }
            removePlaylist(indexes.get(i));
        }
        return basePlaylist.title;
    }

    public boolean isEmpty() {
        return playlists.isEmpty();
    }
}