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
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

class PlaylistAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements ItemMoveCallback.ItemTouchHelperContract {
    MainActivity activity;

    PlaylistAdapter(MainActivity _activity) {
        super();
        activity = _activity;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = activity.getLayoutInflater();
        //View itemView = inf.inflate(R.layout.video_item, parent, false);
        View itemView = inf.inflate(R.layout.video_item_big, parent, false);
        return new RecyclerView.ViewHolder(itemView) {};
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        View itemView = holder.itemView;
        int pos = holder.getAdapterPosition();
        boolean playing = activity.currentPlaylistIndex == activity.playingPlaylistIndex && activity.youTube.playingVideoIndex == pos;

        LinearLayout layout = itemView.findViewById(R.id.layout);
        ImageView thumbnail = itemView.findViewById(R.id.videoThumbnail);
        ImageView thumbnailMini = itemView.findViewById(R.id.videoThumbnailMini);
        TextView title = itemView.findViewById(R.id.videoTitle);
        ImageView options = itemView.findViewById(R.id.videoOptions);
        CardView card = itemView.findViewById(R.id.cardMini);
        CheckBox checkBox = itemView.findViewById(R.id.checkBox);

        setItemOnClickListener(layout, pos);
        setItemOnLongClickListener(layout, pos);

        layout.setBackgroundResource(activity.currentPlaylistIndex == activity.playingPlaylistIndex && activity.youTube.playingVideoIndex == pos
                ? R.drawable.list_item_playing
                : R.drawable.list_item);

        title.setTextColor(activity.getColor(playing ? R.color.green2: R.color.grey1));
        YouTubeVideo thisVideo = activity.currentPlaylist.getVideoAt(pos);
        if (activity.searchMode && activity.foundItemIndex == pos) {
            SpannableString spannableString = new SpannableString(thisVideo.title);
            spannableString.setSpan(new ForegroundColorSpan(activity.getColor(R.color.yellow)),
                    activity.foundAtStart,
                    activity.foundAtEnd,
                    Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            title.setText(spannableString, TextView.BufferType.SPANNABLE);
        } else title.setText(thisVideo.title);
        Glide.with(activity).load(thisVideo.getThumbnailUrl()).into(thumbnail);
        Glide.with(activity).load(thisVideo.getThumbnailUrl()).into(thumbnailMini);
        OnlinePlaylistsUtils.setDimensions(activity, card, activity.isPortrait ? 128 : 80, activity.isPortrait ? 72 : 45, 0);
        //OnlinePlaylistsUtils.setDimensions(activity, card, -1, activity.isPortrait ? 200 : 100, 0);
        //OnlinePlaylistsUtils.setDimensions(activity, title, LinearLayout.LayoutParams.WRAP_CONTENT, activity.isPortrait ? ViewGroup.LayoutParams.WRAP_CONTENT : LinearLayout.LayoutParams.MATCH_PARENT, 1);
        options.setVisibility(activity.selectionMode || activity.listSortMode || activity.searchMode ? View.GONE : View.VISIBLE);
        checkBox.setVisibility(activity.selectionMode ? View.VISIBLE : View.GONE);
        checkBox.setChecked(activity.selectedItems.contains(position));

        PopupMenu popupMenu = activity.getVideoPopupMenu(options, pos);
        options.setOnClickListener(view -> popupMenu.show());
    }

    @Override
    public int getItemCount() {
        return activity.currentPlaylist.getLength();
    }

    public void insertItem(int index) {
        if (activity.youTube.playingVideoIndex != -1 && index <= activity.youTube.playingVideoIndex && activity.currentPlaylistIndex == activity.playingPlaylistIndex) {
            activity.youTube.playingVideoIndex++;
            activity.playingVideo = activity.currentPlaylist.getVideoAt(activity.youTube.playingVideoIndex);
        }

        this.notifyItemInserted(index);
        this.notifyItemRangeChanged(index, activity.listOfPlaylists.getLength()-index);
    }

    public void removeItem(int index) {
        if (activity.playingVideo != null && activity.currentPlaylistIndex == activity.playingPlaylistIndex) {
            if (index < activity.youTube.playingVideoIndex) {
                activity.youTube.playingVideoIndex = activity.currentPlaylist.getIndexOf(activity.playingVideo);
            }
            if (index == activity.youTube.playingVideoIndex) {
                activity.closePlayer();
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
                if ((activity.currentPlaylistIndex == activity.playingPlaylistIndex && position == activity.youTube.playingVideoIndex))
                    if (activity.youTube.isPlaying) activity.youTube.pause();
                    else activity.youTube.play();
                else activity.playVideo(position, true);
            }
            if (activity.searchMode) activity.setSearchMode(false);
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
        activity.currentPlaylist.moveVideo(fromPosition, toPosition);

        int positionMin = Math.min(fromPosition, toPosition);
        int positionMax = Math.max(fromPosition, toPosition);

        if (activity.currentPlaylistIndex == activity.playingPlaylistIndex && activity.playingVideo != null)
            activity.youTube.playingVideoIndex = activity.currentPlaylist.getIndexOf(activity.playingVideo);

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
        activity.removeVideo(viewHolder.getAdapterPosition());
    }

    @Override
    public Context getContext() {
        return activity;
    }
}