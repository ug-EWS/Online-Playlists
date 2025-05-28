package com.example.onlineplaylists;

import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;

public class AutoShutDownDialog {
    private final AlertDialog dialog;
    private final MainActivity activity;
    private float minutes;
    private final EditText editText;

    AutoShutDownDialog(MainActivity _activity) {
        activity = _activity;

        View view = activity.getLayoutInflater().inflate(R.layout.auto_shut_down, null);
        editText = view.findViewById(R.id.editTextNumber);

        dialog = new AlertDialog.Builder(activity, R.style.Theme_OnlinePlaylistsDialog)
                .setTitle(R.string.auto_shut_down_title)
                .setView(view)
                .setPositiveButton(R.string.dialog_button_start, (dialog1, which) -> {
                    String text = editText.getText().toString();
                    if (text.isEmpty()) minutes = 60;
                    else minutes = Float.parseFloat(text);
                    if (minutes <= 0) minutes = 0.1F;
                    activity.startTimer((long)(minutes * 60000));
                })
                .setNegativeButton(R.string.dialog_button_cancel, null)
                .create();
    }

    public void show() {
        dialog.show();
        editText.requestFocus();
    }
}