package com.example.onlineplaylists;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

class ManagePlaylistsDialog {
    MainActivity activity;

    AlertDialog.Builder builder;
    AlertDialog dialog;

    ListOfPlaylists listOfPlaylists;
    Playlist currentPlaylist;
    YouTubeVideo video;

    ArrayList<CharSequence> items;
    ArrayList<Integer> forVideos;
    ArrayList<Integer> originalIndexes;
    boolean[] contains;
    CharSequence[] itemsArr;
    int length;
    int length2;

    ManagePlaylistsDialog (MainActivity _activity, int _forVideo) {
        initializeDialog(_activity);
        video = currentPlaylist.getVideoAt(_forVideo);

        items = new ArrayList<>();
        originalIndexes = new ArrayList<>();

        length = listOfPlaylists.getLength();
        for (int i = 0; i < length; i++) {
            Playlist playlist = listOfPlaylists.getPlaylistAt(i);
            if (!playlist.contains(video) && !playlist.remote) {
                items.add(playlist.title);
                originalIndexes.add(i);
            }
        }

        itemsArr = items.toArray(new CharSequence[0]);
        length2 = items.size();
        contains = new boolean[length2];

        builder.setMultiChoiceItems(itemsArr, null, (dialog1, which, isChecked) -> {
            contains[which] = isChecked;
        });

        builder.setPositiveButton(R.string.copy, (dialog1, which) -> {
            copyVideo();
            activity.showMessage(R.string.copied);
        });

        builder.setNeutralButton(R.string.move, (dialog1, which) -> {
            copyVideo();
            currentPlaylist.removeVideo(_forVideo);
            activity.playlistAdapter.removeItem(_forVideo);
            activity.showMessage(R.string.moved);
        });
        dialog = builder.create();
    }

    ManagePlaylistsDialog(MainActivity _activity, ArrayList<Integer> _forVideos) {
        initializeDialog(_activity);
        items = new ArrayList<>();
        originalIndexes = new ArrayList<>();
        forVideos = _forVideos;

        length = listOfPlaylists.getLength();

        for (int i = 0; i < length; i++) {
            Playlist playlist = listOfPlaylists.getPlaylistAt(i);
            if (activity.currentPlaylistIndex != i && !playlist.remote) {
                items.add(playlist.title);
                originalIndexes.add(i);
            }
        }

        itemsArr = items.toArray(new CharSequence[0]);
        contains = new boolean[length];

        builder.setMultiChoiceItems(itemsArr, null, (dialog1, which, isChecked) -> {
            contains[which] = isChecked;
        });

        builder.setPositiveButton(R.string.copy, (dialog1, which) -> {
            copyVideos();
            activity.setSelectionMode(false);
            activity.showMessage(R.string.copied);
        });

        builder.setNeutralButton(R.string.move, (dialog1, which) -> {
            copyVideos();
            currentPlaylist.removeVideos(forVideos);
            activity.setSelectionMode(false);
            activity.showMessage(R.string.moved);
        });
        dialog = builder.create();
    }

    private void initializeDialog(MainActivity _activity) {
        activity = _activity;
        listOfPlaylists = activity.listOfPlaylists;
        currentPlaylist = activity.currentPlaylist;

        builder = new AlertDialog.Builder(activity, R.style.Theme_OnlinePlaylistsDialog);
        builder.setTitle(R.string.add_to_playlist);
        builder.setNegativeButton(R.string.dialog_button_cancel, null);
    }

    private void copyVideo() {
        for (int i = 0; i < length2; i++) {
            Playlist playlist = listOfPlaylists.getPlaylistAt(originalIndexes.get(i));
            if (contains[i]) {
                playlist.addVideo(video);
            }
        }
    }

    private void copyVideos() {
        for (int i = 0; i < length; i++) {
            if (contains[i]) {
                Playlist playlist = listOfPlaylists.getPlaylistAt(originalIndexes.get(i));
                for (int j = forVideos.size() - 1; j >= 0; j--) {
                    YouTubeVideo youTubeVideo = activity.currentPlaylist.getVideoAt(forVideos.get(j));
                    if (!playlist.contains(youTubeVideo)) playlist.addVideo(youTubeVideo);
                }
            }
        }
    }

    public void show() {
        if (items.isEmpty()) activity.showMessage(R.string.no_playlist_found);
        else dialog.show();
    }
}