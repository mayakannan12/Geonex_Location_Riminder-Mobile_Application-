package com.example.geonex;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

public class SwipeToDeleteCallback extends ItemTouchHelper.SimpleCallback {

    private final ReminderAdapter adapter;
    private final Context context;
    private final Paint paint = new Paint();
    private final OnSwipeListener swipeListener;

    public interface OnSwipeListener {
        void onSwipedLeft(Reminder reminder, int position);
        void onSwipedRight(Reminder reminder, int position);
    }

    public SwipeToDeleteCallback(Context context, ReminderAdapter adapter, OnSwipeListener listener) {
        super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        this.context = context;
        this.adapter = adapter;
        this.swipeListener = listener;

        paint.setAntiAlias(true);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView,
                          @NonNull RecyclerView.ViewHolder viewHolder,
                          @NonNull RecyclerView.ViewHolder target) {
        return false; // We don't support drag & drop
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        int position = viewHolder.getAdapterPosition();
        Reminder reminder = adapter.getReminderAt();

        if (direction == ItemTouchHelper.LEFT) {
            // Swipe left - Delete
            swipeListener.onSwipedLeft(reminder, position);
        } else if (direction == ItemTouchHelper.RIGHT) {
            // Swipe right - Mark as completed
            swipeListener.onSwipedRight(reminder, position);
        }
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                            @NonNull RecyclerView.ViewHolder viewHolder,
                            float dX, float dY, int actionState, boolean isCurrentlyActive) {

        View itemView = viewHolder.itemView;

        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            if (dX > 0) {
                // Swiping right - Mark as completed (Green)
                drawRightSwipe(c, itemView, dX);
            } else if (dX < 0) {
                // Swiping left - Delete (Red)
                drawLeftSwipe(c, itemView, dX);
            }
        }

        super.onChildDraw(c, recyclerView, viewHolder, dX / 3, dY, actionState, isCurrentlyActive);
    }

    private void drawRightSwipe(Canvas c, View itemView, float dX) {
        // Draw green background
        paint.setColor(Color.parseColor("#22C55E")); // Success green

        RectF background = new RectF(
                (float) itemView.getLeft(),
                (float) itemView.getTop(),
                (float) itemView.getLeft() + dX,
                (float) itemView.getBottom()
        );
        c.drawRect(background, paint);

        // Draw check icon
        paint.setColor(Color.WHITE);
        paint.setTextSize(60);
        paint.setTextAlign(Paint.Align.CENTER);

        // Calculate position for icon
        float textX = itemView.getLeft() + 100;
        float textY = itemView.getTop() + (itemView.getBottom() - itemView.getTop()) / 2 + 20;

        c.drawText("✓", textX, textY, paint);

        // Draw text
        paint.setTextSize(40);
        c.drawText("Complete", textX + 60, textY, paint);
    }

    private void drawLeftSwipe(Canvas c, View itemView, float dX) {
        // Draw red background
        paint.setColor(Color.parseColor("#EF4444")); // Error red

        RectF background = new RectF(
                (float) itemView.getRight() + dX,
                (float) itemView.getTop(),
                (float) itemView.getRight(),
                (float) itemView.getBottom()
        );
        c.drawRect(background, paint);

        // Draw trash icon
        paint.setColor(Color.WHITE);
        paint.setTextSize(60);
        paint.setTextAlign(Paint.Align.CENTER);

        // Calculate position for icon
        float textX = itemView.getRight() - 100;
        float textY = itemView.getTop() + (itemView.getBottom() - itemView.getTop()) / 2 + 20;

        c.drawText("🗑️", textX - 80, textY, paint);

        // Draw text
        paint.setTextSize(40);
        c.drawText("Delete", textX - 140, textY, paint);
    }
}