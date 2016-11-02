package test.stupidgrid;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import java.util.Collections;
import java.util.List;

public class StupidAdapter extends RecyclerView.Adapter<StupidAdapter.ItemViewHolder> {

    private final Context context;
    private final List<Integer> dataset;

    public StupidAdapter(Context context, List<Integer> dataset) {
        this.context = context;
        this.dataset = dataset == null ? Collections.<Integer>emptyList() : dataset;
    }

    @Override
    public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ItemViewHolder(new View(context));
    }

    @Override
    public void onBindViewHolder(ItemViewHolder holder, int position) {
        holder.bind(dataset.get(position));
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

        public ItemViewHolder(View itemView) {
            super(itemView);
            bg = new ShapeDrawable(new OvalShape());
            itemView.setBackground(bg);
        }

        public void bind(Integer color) {
            bg.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
        }
    }

}
