package com.example.onlineplaylists;

import android.content.DialogInterface;
import android.graphics.Color;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;

class PlaylistDialog {
    MainActivity activity;
    AlertDialog.Builder builder;
    AlertDialog dialog;
    View dialogView;
    EditText editText, editText2;
    LinearLayout iconSelector;
    CheckBox checkBox;
    Playlist toEdit;
    int whereToAdd;
    int selectedIcon;

    PlaylistDialog(MainActivity _activity, int _forPlaylist) {
        activity = _activity;
        builder = new AlertDialog.Builder(activity, R.style.Theme_OnlinePlaylistsDialog);
        dialogView = activity.getLayoutInflater().inflate(R.layout.add_playlist, null);
        editText = dialogView.findViewById(R.id.editText);
        editText2 = dialogView.findViewById(R.id.editText2);
        iconSelector = dialogView.findViewById(R.id.iconSelector);
        View.OnClickListener onClickListener = view -> selectIcon(iconSelector.indexOfChild(view));
        for (int i = 0; i < 5; i++) {
            iconSelector.getChildAt(i).setOnClickListener(onClickListener);
        }
        checkBox = dialogView.findViewById(R.id.checkBox);
        checkBox.setOnCheckedChangeListener(
                (buttonView, isChecked) -> editText2.setVisibility(isChecked ? View.VISIBLE : View.GONE));
        builder.setNegativeButton(activity.getString(R.string.dialog_button_cancel), null);
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
            selectIcon(0);
            editText.setText("");
            builder.setPositiveButton(activity.getString(R.string.dialog_button_add), (dialog, which) -> {
                String text = editText.getText().toString();
                if (text.isEmpty()) text = editText.getHint().toString();
                String remoteId = null;
                if (checkBox.isChecked()) {
                    remoteId = editText2.getText().toString();
                    remoteId = Playlist.getIdFrom(remoteId);
                }
                activity.listOfPlaylists.addPlaylistTo(new Playlist(text, selectedIcon, remoteId), whereToAdd);
                activity.listOfPlaylistsAdapter.insertItem(whereToAdd);
                activity.updateNoItemsView();
                activity.listOfPlaylistsRecycler.scrollToPosition(whereToAdd);
            });
            checkBox.setChecked(false);
            checkBox.setEnabled(true);
        } else {
            toEdit = activity.listOfPlaylists.getPlaylistAt(_forPlaylist);
            editText.setText(toEdit.title);
            selectIcon(toEdit.icon);
            builder.setPositiveButton(activity.getString(R.string.dialog_button_apply), (dialog, which) -> {
                String text = editText.getText().toString();
                String remoteId = editText2.getText().toString();
                toEdit.title = text;
                toEdit.icon = selectedIcon;
                if (checkBox.isChecked() && !remoteId.isEmpty()) toEdit.remoteId = remoteId;
                if (activity.viewMode) activity.titleText.setText(text);
                else activity.listOfPlaylistsAdapter.notifyItemChanged(_forPlaylist);
            });
            checkBox.setEnabled(false);
            checkBox.setChecked(toEdit.remote);
            editText2.setText(toEdit.remoteId);
        }

        dialog = builder.create();
    }

    private void selectIcon(int index) {
        if (index > 4 || index < 0) index = 0;
        selectedIcon = index;
        for (int i = 0; i < 5; i++) {
            ImageView icon = (ImageView) iconSelector.getChildAt(i);
            icon.setBackgroundResource(i == selectedIcon ? R.drawable.icon_selector_1 : R.drawable.icon_selector);
            icon.setColorFilter(i == selectedIcon ? Color.WHITE : activity.getColor(R.color.grey6));
        }
    }

    public void show() {
        show(0);
    }

    public void show(int _whereToAdd) {
        whereToAdd = _whereToAdd;
        dialog.show();
    }
}