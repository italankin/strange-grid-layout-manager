/*
 * Copyright 2016 Igor Talankin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
