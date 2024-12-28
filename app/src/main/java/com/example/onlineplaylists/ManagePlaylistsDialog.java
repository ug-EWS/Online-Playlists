package com.example.onlineplaylists;

import androidx.appcompat.app.AlertDialog;

import java.util.ArrayList;

class ManagePlaylistsDialog {
    MainActivity activity;

    AlertDialog.Builder builder;
    AlertDialog dialog;

    ListOfPlaylists listOfPlaylists;
    Playlist currentPlaylist;
    YouTubeVideo video;

    ArrayList<CharSequence> items;
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
            if (!playlist.contains(video)) {
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

        builder.setPositiveButton(R.string.dialog_button_add, (dialog1, which) -> {
            for (int i = 0; i < length2; i++) {
                Playlist playlist = listOfPlaylists.getPlaylistAt(originalIndexes.get(i));
                if (contains[i]) {
                    playlist.addVideo(video);
                }
            }
            activity.showMessage("Eklendi");
        });

        builder.setNegativeButton(R.string.dialog_button_cancel, (dialog1, which) -> dialog.dismiss());
        dialog = builder.create();
    }

    ManagePlaylistsDialog(MainActivity _activity, ArrayList<Integer> _forVideos) {
        initializeDialog(_activity);
        items = new ArrayList<>();
        originalIndexes = new ArrayList<>();

        length = listOfPlaylists.getLength();

        for (int i = 0; i < length; i++) {
            Playlist playlist = listOfPlaylists.getPlaylistAt(i);
            items.add(playlist.title);
        }

        itemsArr = items.toArray(new CharSequence[0]);
        contains = new boolean[length];

        builder.setMultiChoiceItems(itemsArr, null, (dialog1, which, isChecked) -> {
            contains[which] = isChecked;
        });

        builder.setPositiveButton(R.string.dialog_button_add, (dialog1, which) -> {
            for (int i = 0; i < length; i++) {
                Playlist playlist = listOfPlaylists.getPlaylistAt(i);
                if (contains[i]) {
                    for (int j = _forVideos.size() - 1; j >= 0; j--)
                        playlist.addVideo(activity.currentPlaylist.getVideoAt(j));
                }
            }
            activity.setSelectionMode(false);
            activity.showMessage("Eklendi");
        });

        dialog = builder.create();
    }

    private void initializeDialog(MainActivity _activity) {
        activity = _activity;
        listOfPlaylists = activity.listOfPlaylists;
        currentPlaylist = activity.currentPlaylist;

        builder = new AlertDialog.Builder(activity, R.style.Theme_OnlinePlaylistsDialogDark);
        builder.setTitle("Oynatma listesine ekle");
        builder.setNegativeButton(R.string.dialog_button_cancel, null);
    }

    public void show() {
        dialog.show();
    }
}
