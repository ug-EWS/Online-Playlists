package com.example.onlineplaylists;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

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
        View itemView = inf.inflate(R.layout.video_item, parent, false);
        return new RecyclerView.ViewHolder(itemView) {};
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        View itemView = holder.itemView;
        int pos = holder.getAdapterPosition();

        ImageView thumbnail = itemView.findViewById(R.id.videoThumbnail);
        TextView title = itemView.findViewById(R.id.videoTitle);
        ImageView options = itemView.findViewById(R.id.videoOptions);
        CardView card = itemView.findViewById(R.id.card);

        setItemOnClickListener(itemView, pos);

        itemView.setBackgroundResource(activity.currentPlaylistIndex == activity.playingPlaylistIndex && activity.playingVideoIndex == pos
                ? R.drawable.list_item_playing
                : R.drawable.list_item);

        YouTubeVideo thisVideo = activity.currentPlaylist.getVideoAt(pos);
        title.setText(thisVideo.title);
        title.setTextColor(activity.currentPlaylistIndex == activity.playingPlaylistIndex && activity.playingVideoIndex == pos ? Color.GREEN : Color.WHITE);
        Glide.with(activity).load(thisVideo.getThumbnailUrl()).into(thumbnail);
        card.setVisibility(activity.showThumbnails ? View.VISIBLE : View.GONE);

        PopupMenu popupMenu = activity.getVideoPopupMenu(options, pos);
        options.setOnClickListener(view -> popupMenu.show());
    }

    @Override
    public int getItemCount() {
        return activity.currentPlaylist.getLength();
    }
    public void insertItem(int index) {
        if (activity.playingVideoIndex != -1 && index <= activity.playingVideoIndex && activity.currentPlaylistIndex == activity.playingPlaylistIndex) {
            activity.playingVideoIndex++;
            activity.playingVideo = activity.currentPlaylist.getVideoAt(activity.playingVideoIndex);
        }
        if (activity.cutVideo != null && index <= activity.cutVideoIndex && activity.currentPlaylistIndex == activity.cutPlaylistIndex) {
            activity.cutVideoIndex = activity.currentPlaylist.getIndexOf(activity.cutVideo);
        }
        this.notifyItemInserted(index);
        this.notifyItemRangeChanged(index, activity.listOfPlaylists.getLength()-index);
    }
    public void removeItem(int index) {
        if (activity.playingVideo != null && activity.currentPlaylistIndex == activity.playingPlaylistIndex) {
            if (index < activity.playingVideoIndex) {
                activity.playingVideoIndex = activity.currentPlaylist.getIndexOf(activity.playingVideo);
            }
            if (index == activity.playingVideoIndex) {
                activity.closePlayer();
            }
        }
        if (activity.cutVideo != null && activity.currentPlaylistIndex == activity.cutPlaylistIndex) {
            if (index < activity.cutVideoIndex) {
                activity.cutVideoIndex = activity.currentPlaylist.getIndexOf(activity.cutVideo);
            }
            if (index == activity.cutVideoIndex) {
                activity.cutPlaylist = null;
                activity.cutVideo = null;
            }
        }
        this.notifyItemRemoved(index);
        this.notifyItemRangeChanged(index, activity.listOfPlaylists.getLength()-index);
    }
    private void setItemOnClickListener(View v, int position) {
        v.setOnClickListener(view -> {
            if ((activity.currentPlaylistIndex == activity.playingPlaylistIndex && position == activity.playingVideoIndex))
                if (activity.isPlaying) activity.youTubePlayer.pause(); else activity.youTubePlayer.play();
            else activity.playVideo(position, true);});
    }

    @Override
    public void onRowMoved(int fromPosition, int toPosition) {
        activity.currentPlaylist.moveVideo(fromPosition, toPosition);

        int positionMin = Math.min(fromPosition, toPosition);
        int positionMax = Math.max(fromPosition, toPosition);

        if (activity.currentPlaylistIndex == activity.playingPlaylistIndex && activity.playingVideo != null)
            activity.playingVideoIndex = activity.currentPlaylist.getIndexOf(activity.playingVideo);

        if (activity.currentPlaylistIndex == activity.cutVideoIndex && activity.cutVideo != null)
            activity.cutVideoIndex = activity.currentPlaylist.getIndexOf(activity.cutVideo);

        notifyItemMoved(fromPosition, toPosition);
        notifyItemRangeChanged(positionMin, positionMax - positionMin + 1);

    }

    @Override
    public void onRowSelected(RecyclerView.ViewHolder viewHolder) {
        activity.vibrator.vibrate(50);
    }

    @Override
    public void onRowClear(RecyclerView.ViewHolder viewHolder) {
    }
}
