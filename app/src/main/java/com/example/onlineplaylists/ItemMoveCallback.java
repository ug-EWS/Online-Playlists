package com.example.onlineplaylists;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

public class ItemMoveCallback extends ItemTouchHelper.Callback {

    private final ItemTouchHelperContract adapter;

    ItemMoveCallback(ItemTouchHelperContract _adapter) {
        adapter = _adapter;
    }

    @Override
    public boolean isLongPressDragEnabled() {
        return adapter.isDragEnabled();
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        return adapter.isSwipeEnabled();
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int i) {
        adapter.onSwipe(viewHolder, i);
    }

    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
        int swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END;
        return makeMovementFlags(dragFlags, swipeFlags);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                          RecyclerView.ViewHolder target) {
        adapter.onRowMoved(viewHolder.getAdapterPosition(), target.getAdapterPosition());
        return true;
    }

    @Override
    public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
        if (actionState != ItemTouchHelper.ACTION_STATE_IDLE)
            adapter.onRowSelected(viewHolder);
        super.onSelectedChanged(viewHolder, actionState);
    }

    @Override
    public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);
        adapter.onRowClear(viewHolder);
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE && dX != 0 && isCurrentlyActive) {
            Drawable d = ContextCompat.getDrawable(adapter.getContext(), R.drawable.baseline_delete_forever_red_24);
            assert d != null;
            View itemView = viewHolder.itemView;
            int iconWidth = d.getIntrinsicWidth();
            int iconHeight = d.getIntrinsicHeight();
            int cellHeight = itemView.getBottom() - itemView.getTop();
            int iconTop = itemView.getTop() + (cellHeight - iconHeight) / 2;
            int iconBottom = iconTop + iconHeight;
            int margin = (int)((Math.abs(dX) - iconWidth) / 2);
            int iconLeft = dX > 0 ? itemView.getLeft() + margin : itemView.getRight() - margin - iconWidth;
            int iconRight = dX > 0 ? itemView.getLeft() + margin + iconWidth : itemView.getRight() - margin;
            d.setBounds(iconLeft, iconTop, iconRight, iconBottom);
            d.draw(c);
        }
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }

    interface ItemTouchHelperContract {
        boolean isDragEnabled();
        boolean isSwipeEnabled();
        void onRowMoved(int fromPosition, int toPosition);
        void onRowSelected(RecyclerView.ViewHolder myViewHolder);
        void onRowClear(RecyclerView.ViewHolder myViewHolder);
        void onSwipe(@NonNull RecyclerView.ViewHolder viewHolder, int i);
        Context getContext();
    }
}