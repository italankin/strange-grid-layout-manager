package test.stupidgrid;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class StupidActivity extends AppCompatActivity {

    private static final int[] COLUMNS = {3, 2};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_stupid);
        RecyclerView list = (RecyclerView) findViewById(R.id.list);

        final int margin = getResources().getDimensionPixelSize(R.dimen.child_padding);
        StupidLayoutManager layoutManager = new StupidLayoutManager(this);
        layoutManager.setColumnCounts(COLUMNS);
        layoutManager.setChildMargins(margin, margin);
        layoutManager.setCenterRemainingViews(true);
        list.setLayoutManager(layoutManager);

        final List<Integer> dataset = generateDataset(30);
        final StupidAdapter adapter = new StupidAdapter(this, dataset);
        list.addOnItemTouchListener(new StupidAnimateTouchListener(this));

        list.addOnItemTouchListener(new StupidClickListener(this, new StupidClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(RecyclerView recyclerView, View view, int position) {
                int color = dataset.get(position);
                String s = String.format(Locale.getDefault(), "#%02X%02X%02X",
                        Color.red(color), Color.green(color), Color.blue(color));
                Toast.makeText(StupidActivity.this, s, Toast.LENGTH_SHORT).show();
            }
        }));

        list.setAdapter(adapter);
    }

    private List<Integer> generateDataset(int size) {
        Random random = new Random();
        List<Integer> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(0xff000000 | random.nextInt());
        }
        return list;
    }

}
