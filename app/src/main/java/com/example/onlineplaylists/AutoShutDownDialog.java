package com.example.onlineplaylists;

import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

public class AutoShutDownDialog {
    private AlertDialog dialog;
    private AlertDialog.Builder builder;
    private MainActivity activity;
    private int minutes;
    private TextView textView;
    private SeekBar seekBar;

    AutoShutDownDialog(MainActivity _activity) {
        activity = _activity;
        builder = new AlertDialog.Builder(activity, R.style.Theme_OnlinePlaylistsDialog);
        builder.setTitle(R.string.auto_shut_down_title);
        View view = activity.getLayoutInflater().inflate(R.layout.auto_shut_down, null);
        textView = view.findViewById(R.id.textView);
        seekBar = view.findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                setMinutes();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        setMinutes();
        builder.setView(view);
        builder.setPositiveButton(R.string.dialog_button_start, (dialog1, which) ->
                activity.startTimer(minutes * 60000L));
        builder.setNegativeButton(R.string.dialog_button_cancel, null);
        dialog = builder.create();
    }

    private void setMinutes() {
        minutes = (seekBar.getProgress() + 1) * 10;
        textView.setText(String.format(activity.getString(R.string.after_minutes), minutes));
    }

    public void show() {
        dialog.show();
    }
}
