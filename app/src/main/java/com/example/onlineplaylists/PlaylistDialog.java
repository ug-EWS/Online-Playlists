package com.example.onlineplaylists;

import android.content.DialogInterface;
import android.graphics.Color;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.appcompat.app.AlertDialog;

class PlaylistDialog {
    MainActivity activity;
    AlertDialog.Builder builder;
    AlertDialog dialog;
    View dialogView;
    EditText editText;
    LinearLayout iconSelector;
    Playlist toEdit;
    int whereToAdd;
    int selectedIcon;

    PlaylistDialog(MainActivity _activity, int _forPlaylist) {
        activity = _activity;
        builder = new AlertDialog.Builder(activity, R.style.Theme_OnlinePlaylistsDialogDark);
        dialogView = activity.getLayoutInflater().inflate(R.layout.add_playlist, null);
        editText = dialogView.findViewById(R.id.editText);
        iconSelector = dialogView.findViewById(R.id.iconSelector);
        View.OnClickListener onClickListener = view -> {
            selectedIcon = iconSelector.indexOfChild(view);
            for (int i = 0; i < 5; i++) {
                iconSelector.getChildAt(i).setBackgroundColor(
                        i == selectedIcon
                                ? activity.getResources().getColor(R.color.soft_red, activity.getTheme())
                                : Color.TRANSPARENT
                );
            }
        };
        for (int i = 0; i < 5; i++) {
            iconSelector.getChildAt(i).setOnClickListener(onClickListener);
        }
        builder.setNegativeButton(
                activity.getString(R.string.dialog_button_cancel),
                (dialog, which) -> dialog.dismiss()
        );
        builder.setView(dialogView);
        editText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO) {
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
                return true;
            }
            return false;
        });
        boolean _newPlaylist = _forPlaylist == -1;
        builder.setTitle(activity.getString(_newPlaylist ? R.string.add_playlist : R.string.edit_playlist));
        if (_newPlaylist) {
            iconSelector.getChildAt(0).setBackgroundColor(
                    activity.getResources().getColor(R.color.soft_red,
                            activity.getTheme()));
            builder.setPositiveButton(activity.getString(R.string.dialog_button_add), (dialog, which) -> {
                String text = editText.getText().toString();
                if (text.isEmpty()) text = editText.getHint().toString();
                activity.listOfPlaylists.addPlaylistTo(new Playlist(text, selectedIcon), whereToAdd);
                activity.listOfPlaylistsAdapter.insertItem(whereToAdd);
                dialog.dismiss();
            });
        } else {
            toEdit = activity.listOfPlaylists.getPlaylistAt(_forPlaylist);
            editText.setText(toEdit.title);
            selectedIcon = toEdit.icon;
            if (selectedIcon > 4 || selectedIcon < 0) selectedIcon = 0;
            iconSelector.getChildAt(selectedIcon).setBackgroundColor(activity.getResources().getColor(R.color.soft_red, activity.getTheme()));
            builder.setPositiveButton(activity.getString(R.string.dialog_button_apply), (dialog, which) -> {
                String text = editText.getText().toString();
                toEdit.title = text;
                toEdit.icon = selectedIcon;
                if (activity.viewMode) activity.titleText.setText(text);
                else activity.listOfPlaylistsAdapter.notifyItemChanged(_forPlaylist);
            });
        }

        dialog = builder.create();
    }

    public void show() {
        dialog.show();
    }

    public void show(int _whereToAdd) {
        whereToAdd = _whereToAdd;
        dialog.show();
    }
}
