package com.example.onlineplaylists;

import androidx.appcompat.app.AlertDialog;

import java.util.ArrayList;

class ManagePlaylistsDialog {
    MainActivity activity;
    AlertDialog.Builder builder;
    AlertDialog dialog;
    YouTubeVideo video;
    ArrayList<CharSequence> items;
    ArrayList<Integer> originalIndexes;
    boolean[] contains;
    CharSequence[] itemsArr;
    int length;
    int length2;

    ManagePlaylistsDialog(MainActivity _activity, YouTubeVideo _video) {
        activity = _activity;
        video = _video;
        builder = new AlertDialog.Builder(activity, R.style.Theme_OnlinePlaylistsDialogDark);
        builder.setTitle("Oynatma listesine ekle");

        items = new ArrayList<>();
        originalIndexes = new ArrayList<>();

        length = activity.listOfPlaylists.getLength();
        for (int i = 0; i < length; i++) {
            Playlist playlist = activity.listOfPlaylists.getPlaylistAt(i);
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

        builder.setPositiveButton(R.string.dialog_button_apply, (dialog1, which) -> {
            for (int i = 0; i < length2; i++) {
                Playlist playlist = activity.listOfPlaylists.getPlaylistAt(originalIndexes.get(i));
                if (contains[i]) {
                    playlist.addVideo(video);
                    if (i == activity.currentPlaylistIndex) activity.playlistAdapter.insertItem(0);
                }
            }
            dialog.dismiss();
        });
        builder.setNegativeButton(R.string.dialog_button_cancel, (dialog1, which) -> dialog.dismiss());
        dialog = builder.create();
    }

    public void show() {
        dialog.show();
    }
}
