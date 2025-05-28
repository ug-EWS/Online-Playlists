package com.example.onlineplaylists;

import android.view.View;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import java.util.ArrayList;

public class MusicStartPointDialog {
    private final AlertDialog dialog;
    private final MainActivity activity;
    private final TextView textView;
    private final SeekBar seekBar;
    private final CheckBox checkBox;

    MusicStartPointDialog(MainActivity _activity, ArrayList<Integer> _forVideos) {
        activity = _activity;

        View view = activity.getLayoutInflater().inflate(R.layout.music_start_point, null);
        textView = view.findViewById(R.id.text);
        seekBar = view.findViewById(R.id.seekBar);
        checkBox = view.findViewById(R.id.checkBox);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                textView.setText(OnlinePlaylistsUtils.getHMS(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        boolean forOneVideo = _forVideos.size() == 1;
        if (forOneVideo) {
            int forVideo = _forVideos.get(0);
            seekBar.setProgress(activity.currentPlaylist.getVideoAt(forVideo).musicStartSeconds);
        }
        checkBox.setVisibility(forOneVideo ? View.GONE : View.VISIBLE);

        dialog = new AlertDialog.Builder(activity, R.style.Theme_OnlinePlaylistsDialog)
                .setTitle("Müzik başlangıç noktası")
                .setView(view)
                .setPositiveButton(R.string.dialog_button_apply, ((dialog1, which) -> {
                    int seconds = seekBar.getProgress();
                    for (Integer i: _forVideos) {
                        YouTubeVideo video = activity.currentPlaylist.getVideoAt(i);
                        if (!(checkBox.isChecked() && video.musicStartSeconds != 0)) video.musicStartSeconds = seconds;
                    }
                    activity.showMessage(R.string.music_start_point_set);
                }))
                .setNegativeButton(R.string.dialog_button_no, null)
                .create();
    }

    public void show() {
        dialog.show();
    }

}
