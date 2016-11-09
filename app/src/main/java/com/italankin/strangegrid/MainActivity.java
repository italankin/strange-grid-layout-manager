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

import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements ClickListener.OnItemClickListener {

    private static final String KEY_DATASET = "dataset";
    private static final String KEY_COLUMNS = "columns";

    private static final int[] COLUMNS = {4};
    private static final int SIZE = 10000;

    private ArrayList<Integer> dataset;
    private int[] columns = COLUMNS;
    private RecyclerView list;
    private StrangeGridLayoutManager layoutManager;
    private DataAdapter adapter;
    private AlertDialog alertDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        list = (RecyclerView) findViewById(R.id.list);

        if (savedInstanceState != null) {
            dataset = savedInstanceState.getIntegerArrayList(KEY_DATASET);
            columns = savedInstanceState.getIntArray(KEY_COLUMNS);
        } else {
            dataset = generateDataset(SIZE);
        }

        final int margin = getResources().getDimensionPixelSize(R.dimen.child_padding);
        layoutManager = new StrangeGridLayoutManager(this);
        layoutManager.setColumnCounts(columns);
        layoutManager.setChildMargins(margin, margin);
        list.setLayoutManager(layoutManager);

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
                dataset = generateDataset(dataset == null ? SIZE : dataset.size());
                adapter.setDataset(dataset);
                return true;
            case R.id.action_size:
                showChangeSizeDialog();
                return true;
            case R.id.action_length:
                showChangeLengthDialog();
                return true;
            case R.id.action_goto:
                showGoToDialog();
                return true;
        }
        return false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isFinishing() && alertDialog != null) {
            alertDialog.dismiss();
            alertDialog = null;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putIntegerArrayList(KEY_DATASET, dataset);
        outState.putIntArray(KEY_COLUMNS, columns);
    }

    @Override
    public void onItemClick(RecyclerView recyclerView, View view, int position) {
        int color = dataset.get(position);
        String s = String.format(Locale.getDefault(), "#%02x%02x%02x",
                Color.red(color), Color.green(color), Color.blue(color));
        Toast.makeText(MainActivity.this, s, Toast.LENGTH_SHORT).show();
    }

    private void showChangeLengthDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.change_length);

        LinearLayout layout = new LinearLayout(this);
        int p = getResources().getDimensionPixelSize(R.dimen.child_padding);
        layout.setPadding(p, p, p, p);
        final EditText editText = new EditText(this);
        editText.setInputType(InputType.TYPE_CLASS_NUMBER);
        editText.setHint(R.string.hint_change_length);
        layout.addView(editText, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        builder.setView(layout);

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String s = editText.getText().toString();
                int size;
                try {
                    size = Integer.parseInt(s);
                    if (size > 10000) {
                        size = 10000;
                    } else if (size < 0) {
                        size = 0;
                    }
                } catch (NumberFormatException e) {
                    size = 0;
                }
                if (size == dataset.size()) {
                    return;
                }
                if (size < dataset.size()) {
                    dataset = new ArrayList<>(dataset.subList(0, size));
                } else {
                    dataset.addAll(generateDataset(size - dataset.size()));
                }
                adapter.setDataset(dataset);
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);
        alertDialog = builder.show();
    }

    private void showChangeSizeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.change_size);

        LinearLayout layout = new LinearLayout(this);
        int p = getResources().getDimensionPixelSize(R.dimen.child_padding);
        layout.setPadding(p, p, p, p);
        final EditText editText = new EditText(this);
        editText.setInputType(InputType.TYPE_CLASS_TEXT);
        editText.setHint(R.string.hint_change_size);
        layout.addView(editText, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        builder.setView(layout);

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String[] a = editText.getText().toString().split(",");
                int[] columns = new int[a.length];
                for (int i = 0, c = a.length; i < c; i++) {
                    try {
                        int value = Integer.parseInt(a[i]);
                        columns[i] = Math.max(1, value);
                    } catch (Exception e) {
                        columns[i] = 1;
                    }
                }
                MainActivity.this.columns = columns;
                layoutManager.setColumnCounts(columns);
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);
        alertDialog = builder.show();
    }

    private void showGoToDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.go_to);

        LinearLayout layout = new LinearLayout(this);
        int p = getResources().getDimensionPixelSize(R.dimen.child_padding);
        layout.setPadding(p, p, p, p);
        final EditText editText = new EditText(this);
        editText.setInputType(InputType.TYPE_CLASS_NUMBER);
        editText.setHint(R.string.hint_go_to_pos);
        layout.addView(editText, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        builder.setView(layout);

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String s = editText.getText().toString();
                int pos;
                try {
                    pos = Integer.parseInt(s) - 1;
                    if (pos > dataset.size()) {
                        pos = dataset.size() - 1;
                    } else if (pos < 0) {
                        pos = 0;
                    }
                } catch (NumberFormatException e) {
                    pos = 0;
                }
                list.scrollToPosition(pos);
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);
        alertDialog = builder.show();
    }

}
