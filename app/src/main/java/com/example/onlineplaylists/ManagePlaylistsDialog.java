package com.example.onlineplaylists;

import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

class ManagePlaylistsDialog {
    MainActivity activity;

    AlertDialog.Builder builder;
    AlertDialog dialog;
    RecyclerView recyclerView;

    ListOfPlaylists listOfPlaylists;
    Playlist currentPlaylist;
    YouTubeVideo video;

    ArrayList<CharSequence> items;
    ArrayList<Integer> forVideos;
    ArrayList<Integer> originalIndexes;
    boolean[] contains;
    int length;

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

        length = items.size();
        contains = new boolean[length];
        recyclerView.setAdapter(new ManagePlaylistsAdapter());

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

        length = items.size();
        contains = new boolean[length];
        recyclerView.setAdapter(new ManagePlaylistsAdapter());

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
        recyclerView = new RecyclerView(activity);
        recyclerView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        recyclerView.setLayoutManager(new LinearLayoutManager(activity));

        builder.setView(recyclerView);
    }

    private void copyVideo() {
        for (int i = 0; i < length; i++) {
            Playlist playlist = listOfPlaylists.getPlaylistAt(originalIndexes.get(i));
            if (contains[i]) {
                playlist.addVideo(video);
                if (activity.playingPlaylistIndex == originalIndexes.get(i)) activity.youTube.playingVideoIndex++;
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
                if (activity.playingPlaylistIndex == originalIndexes.get(i)) activity.youTube.playingVideoIndex += forVideos.size();
            }
        }
    }

    public void show() {
        if (items.isEmpty()) activity.showMessage(R.string.no_playlist_found);
        else dialog.show();
    }

    private class ManagePlaylistsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        Integer[] icons = {
                R.drawable.baseline_featured_play_list_24,
                R.drawable.baseline_favorite_24,
                R.drawable.baseline_library_music_24,
                R.drawable.baseline_videogame_asset_24,
                R.drawable.baseline_movie_creation_24};
        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new RecyclerView.ViewHolder(activity.getLayoutInflater().inflate(R.layout.playlist_item, parent, false)) {};
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            View view = holder.itemView;
            int pos = holder.getAdapterPosition();
            LinearLayout layout = view.findViewById(R.id.layout);
            CheckBox checkBox = view.findViewById(R.id.checkBox);
            ImageView icon = view.findViewById(R.id.playlistIcon);
            TextView title = view.findViewById(R.id.playlistTitle);
            TextView size = view.findViewById(R.id.playlistSize);

            Playlist playlist = listOfPlaylists.getPlaylistAt(originalIndexes.get(pos));
            icon.setImageResource(icons[playlist.icon]);
            title.setText(playlist.title);
            size.setText(String.valueOf(playlist.getLength()).concat(" video"));
            checkBox.setChecked(contains[pos]);
            setOnClickListener(layout, pos);
        }

        @Override
        public int getItemCount() {
            return length;
        }

        private void setOnClickListener(View view, int position) {
            view.setOnClickListener(v -> {
                contains[position] = !contains[position];
                notifyItemChanged(position);
            });
        }
    }
}