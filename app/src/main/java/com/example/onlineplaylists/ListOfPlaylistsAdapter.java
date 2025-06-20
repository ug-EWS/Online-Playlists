package com.example.onlineplaylists;

import android.content.Context;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

class ListOfPlaylistsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements ItemMoveCallback.ItemTouchHelperContract {
    MainActivity activity;

    Integer[] icons = {
            R.drawable.baseline_featured_play_list_24,
            R.drawable.baseline_favorite_24,
            R.drawable.baseline_library_music_24,
            R.drawable.baseline_videogame_asset_24,
            R.drawable.baseline_movie_creation_24};

    ListOfPlaylistsAdapter(MainActivity _activity) {
        super();
        activity = _activity;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = activity.getLayoutInflater();
        View itemView = inf.inflate(R.layout.playlist_item, parent, false);
        return new RecyclerView.ViewHolder(itemView) {};
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        View itemView = holder.itemView;
        int pos = holder.getAdapterPosition();
        Playlist playlist = activity.listOfPlaylists.getPlaylistAt(pos);

        LinearLayout layout = itemView.findViewById(R.id.layout);
        ImageView icon = itemView.findViewById(R.id.playlistIcon);
        TextView title = itemView.findViewById(R.id.playlistTitle);
        TextView size = itemView.findViewById(R.id.playlistSize);
        ImageView options = itemView.findViewById(R.id.playlistOptions);
        CheckBox checkBox = itemView.findViewById(R.id.checkBox);

        setItemOnClickListener(layout, pos);
        setItemOnLongClickListener(layout, pos);

        icon.setBackgroundResource(activity.playingPlaylistIndex == pos ? R.drawable.playlist_icon_playing : R.drawable.playlist_icon);

        int iconIndex = playlist.icon;
        if (iconIndex > 4 || iconIndex < 0) iconIndex = 0;
        icon.setImageResource(icons[iconIndex]);
        String titleStr = playlist.title;
        if (activity.searchMode && activity.foundItemIndex == pos) {
            SpannableString spannableString = new SpannableString(titleStr);
            spannableString.setSpan(new ForegroundColorSpan(activity.getColor(R.color.yellow)),
                    activity.foundAtStart,
                    activity.foundAtEnd,
                    Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            title.setText(spannableString, TextView.BufferType.SPANNABLE);
        } else title.setText(titleStr);
        size.setText(playlist.remote ? "YouTube oynatma listesi" : String.valueOf(playlist.getLength()).concat(" video"));
        title.setTextColor(activity.getColor(activity.playingPlaylistIndex == pos ? R.color.green2 : R.color.grey1));

        checkBox.setVisibility(activity.selectionMode ? View.VISIBLE : View.GONE);
        checkBox.setChecked(activity.selectedItems.contains(position));
        options.setVisibility(activity.selectionMode || activity.listSortMode || activity.searchMode ? View.GONE : View.VISIBLE);

        PopupMenu popupMenu = activity.getPlaylistPopupMenu(options, false, pos);

        options.setOnClickListener(view -> {
            popupMenu.show();
        });
    }

    @Override
    public int getItemCount() {
        return activity.listOfPlaylists.getLength();
    }

    public void insertItem(int index) {
        if (activity.playingPlaylist != null && index <= activity.playingPlaylistIndex) {
            activity.playingPlaylistIndex = activity.listOfPlaylists.getIndexOf(activity.playingPlaylist);
        }
        this.notifyItemInserted(index);
        this.notifyItemRangeChanged(index, activity.listOfPlaylists.getLength()-index);
    }

    public void removeItem(int index) {
        if (activity.playingPlaylist != null) {
            if (index < activity.playingPlaylistIndex) {
                activity.playingPlaylistIndex = activity.listOfPlaylists.getIndexOf(activity.playingPlaylist);
            }
            if (index == activity.playingPlaylistIndex) {
                activity.closePlayer();
                activity.playingPlaylist = null;
            }
        }
        this.notifyItemRemoved(index);
        this.notifyItemRangeChanged(index, activity.listOfPlaylists.getLength()-index);
    }

    private void setItemOnClickListener(View v, int position) {
        v.setOnClickListener(view -> {
            if (activity.selectionMode) {
                if (activity.selectedItems.contains(position))
                    activity.selectedItems.remove((Integer) position);
                else activity.selectedItems.add(position);
                if (activity.selectedItems.isEmpty()) {
                    activity.setSelectionMode(false);
                } else {
                    notifyItemChanged(position);
                    activity.updateToolbar();
                }
            }
            else if (!activity.listSortMode) {
                activity.openPlaylist(position);
                if (activity.searchMode) activity.setSearchMode(false);
            }
        });
    }
    private void setItemOnLongClickListener(View _view, int position) {
        _view.setOnLongClickListener(view -> {
            if (!(activity.selectionMode || activity.listSortMode || activity.searchMode)) {
                activity.selectedItems = new ArrayList<>();
                activity.selectedItems.add(position);
                activity.setSelectionMode(true);
            }
            return true;
        });
    }

    @Override
    public boolean isSwipeEnabled() {
        return !activity.selectionMode;
    }

    @Override
    public boolean isDragEnabled() {
        return activity.listSortMode;
    }

    @Override
    public void onRowMoved(int fromPosition, int toPosition) {
        activity.listOfPlaylists.movePlaylist(fromPosition, toPosition);
        int positionMin = Math.min(fromPosition, toPosition);
        int positionMax = Math.max(fromPosition, toPosition);

        if (activity.playingPlaylist != null)
            activity.playingPlaylistIndex = activity.listOfPlaylists.getIndexOf(activity.playingPlaylist);

        notifyItemMoved(fromPosition, toPosition);
        notifyItemRangeChanged(positionMin, positionMax - positionMin + 1);
    }

    @Override
    public void onRowSelected(RecyclerView.ViewHolder viewHolder) {
    }

    @Override
    public void onRowClear(RecyclerView.ViewHolder viewHolder) {
    }

    @Override
    public void onSwipe(@NonNull RecyclerView.ViewHolder viewHolder, int i) {
        activity.removePlaylist(viewHolder.getAdapterPosition());
    }

    @Override
    public Context getContext() {
        return activity;
    }
}