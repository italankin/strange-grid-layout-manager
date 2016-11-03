package test.strangegrid;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements ClickListener.OnItemClickListener {

    private static final String KEY_DATASET = "dataset";

    private static final int[] COLUMNS = {3, 2};
    private static final int SIZE = 10000;

    private ArrayList<Integer> dataset;
    private DataAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        RecyclerView list = (RecyclerView) findViewById(R.id.list);

        final int margin = getResources().getDimensionPixelSize(R.dimen.child_padding);
        StrangeGridLayoutManager layoutManager = new StrangeGridLayoutManager(this);
        layoutManager.setColumnCounts(COLUMNS);
        layoutManager.setChildMargins(margin, margin);
        list.setLayoutManager(layoutManager);

        if (savedInstanceState != null && savedInstanceState.containsKey(KEY_DATASET)) {
            dataset = savedInstanceState.getIntegerArrayList(KEY_DATASET);
        } else {
            dataset = generateDataset(SIZE);
        }
        adapter = new DataAdapter(this);
        adapter.setDataset(dataset);
        list.setAdapter(adapter);

        list.addOnItemTouchListener(new AnimateTouchListener(this));
        list.addOnItemTouchListener(new ClickListener(this, this));
    }

    private ArrayList<Integer> generateDataset(int size) {
        Random random = new Random();
        ArrayList<Integer> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(0xff000000 | random.nextInt());
        }
        return list;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                dataset = generateDataset(SIZE);
                adapter.setDataset(dataset);
                return true;
        }
        return false;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putIntegerArrayList(KEY_DATASET, dataset);
    }

    @Override
    public void onItemClick(RecyclerView recyclerView, View view, int position) {
        int color = dataset.get(position);
        String s = String.format(Locale.getDefault(), "#%02X%02X%02X",
                Color.red(color), Color.green(color), Color.blue(color));
        Toast.makeText(MainActivity.this, s, Toast.LENGTH_SHORT).show();
    }

}
