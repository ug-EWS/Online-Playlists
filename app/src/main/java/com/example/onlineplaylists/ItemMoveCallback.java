package com.example.onlineplaylists;

import androidx.annotation.NonNull;
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

    interface ItemTouchHelperContract {
        boolean isDragEnabled();
        boolean isSwipeEnabled();
        void onRowMoved(int fromPosition, int toPosition);
        void onRowSelected(RecyclerView.ViewHolder myViewHolder);
        void onRowClear(RecyclerView.ViewHolder myViewHolder);
        void onSwipe(@NonNull RecyclerView.ViewHolder viewHolder, int i);

    }
}