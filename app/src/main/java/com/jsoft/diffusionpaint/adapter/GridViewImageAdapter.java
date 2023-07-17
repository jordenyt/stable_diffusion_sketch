package com.jsoft.diffusionpaint.adapter;

import android.app.AlertDialog;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.jsoft.diffusionpaint.MainActivity;
import com.jsoft.diffusionpaint.R;
import com.jsoft.diffusionpaint.dto.Sketch;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class GridViewImageAdapter extends BaseAdapter {

    private final MainActivity activity;
    private List<Sketch> sketches;

    static SimpleDateFormat promptDateFormat = new SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault());

    public GridViewImageAdapter(MainActivity activity, List<Sketch> sketches) {
        this.activity = activity;
        this.sketches = sketches;
    }

    @Override
    public int getCount() {
        return sketches.size();
    }

    @Override
    public Object getItem(int i) {
        return this.sketches.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = activity.getLayoutInflater();
            convertView = inflater.inflate(R.layout.thumbnail_sketch, parent, false);
        }

        Sketch currentSketch = sketches.get(position);

        TextView textViewItem = convertView.findViewById(R.id.sketch_description);
        ImageView imageView = convertView.findViewById(R.id.sketch_preview);

        imageView.setImageBitmap(currentSketch.getImgPreview());
        textViewItem.setText(currentSketch.getPrompt());

        if (currentSketch.getPrompt()==null || currentSketch.getPrompt().isEmpty()) {
            textViewItem.setText(promptDateFormat.format(currentSketch.getCreateDate()));
        }

        if (currentSketch.getChildren() == null) {
            imageView.setOnClickListener(view -> activity.gotoDrawingActivity(currentSketch.getId()));
        } else {
            textViewItem.setText("[PROJECT " + currentSketch.getId() + "]");
            imageView.setOnClickListener(view -> activity.showGrid(currentSketch.getId()));
        }
        imageView.setOnLongClickListener(v -> showDialog(currentSketch));

        return convertView;
    }

    public boolean showDialog(Sketch s) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage("Are you sure you want to delete this item?");
        builder.setPositiveButton("Yes", (dialog, id) -> activity.deleteSketch(s));
        builder.setNegativeButton("No", (dialog, id) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
        return true;
    }
}
