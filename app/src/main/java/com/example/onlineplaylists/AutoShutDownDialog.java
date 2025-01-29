package com.example.onlineplaylists;

import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

public class AutoShutDownDialog {
    AlertDialog dialog;
    AlertDialog.Builder builder;
    MainActivity activity;
    int minutes;

    AutoShutDownDialog(MainActivity _activity) {
        activity = _activity;
        builder = new AlertDialog.Builder(activity, R.style.Theme_OnlinePlaylistsDialogDark);
        builder.setTitle("Otomatik kapanma");
        View view = activity.getLayoutInflater().inflate(R.layout.auto_shut_down, null);
        TextView textView = view.findViewById(R.id.textView);
        SeekBar seekBar = view.findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                minutes = (seekBar.getProgress() + 1) * 10;
                textView.setText(String.format("%d dakika sonra", minutes));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        builder.setView(view);
        builder.setPositiveButton("Başlat", (dialog1, which) ->
                activity.startTimer(minutes * 60000L));
        builder.setNegativeButton("İptal", null);
        dialog = builder.create();
    }

    public void show() {
        dialog.show();
    }
}
