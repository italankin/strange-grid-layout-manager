package com.italankin.strangegrid;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Collections;
import java.util.List;

public class DataAdapter extends RecyclerView.Adapter<DataAdapter.ItemViewHolder> {

    private final Context context;
    private List<Integer> dataset;

    public DataAdapter(Context context) {
        this.context = context;
    }

    public void setDataset(List<Integer> dataset) {
        this.dataset = dataset == null ? Collections.<Integer>emptyList() : dataset;
        notifyDataSetChanged();
    }

    @Override
    public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ItemViewHolder(new TextView(context));
    }

    @Override
    public void onBindViewHolder(ItemViewHolder holder, int position) {
        holder.bind(position, dataset.get(position));
    }

    @Override
    public long getItemId(int position) {
        return dataset.get(position);
    }

    @Override
    public int getItemCount() {
        return dataset.size();
    }

    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        private final ShapeDrawable bg;
        private final TextView textView;

        public ItemViewHolder(View itemView) {
            super(itemView);
            bg = new ShapeDrawable(new OvalShape());
            itemView.setBackground(bg);
            textView = (TextView) itemView;
            textView.setTextColor(Color.BLACK);
            textView.setGravity(Gravity.CENTER);
        }

        public void bind(int pos, Integer color) {
            bg.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
            textView.setText(String.valueOf(pos + 1));
        }
    }

}
