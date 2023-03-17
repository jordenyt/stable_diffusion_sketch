package com.jsoft.diffusionpaint.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.jsoft.diffusionpaint.MainActivity;
import com.jsoft.diffusionpaint.R;
import com.jsoft.diffusionpaint.helper.Sketch;

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

        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imageView.setImageBitmap(currentSketch.getImgPreview());
        imageView.setAlpha(1f);

        textViewItem.setText(currentSketch.getPrompt());
        if (currentSketch.getPrompt()==null || currentSketch.getPrompt().isEmpty()) {
            textViewItem.setText(promptDateFormat.format(currentSketch.getCreateDate()));
        }
        textViewItem.setTextColor(Color.WHITE);
        imageView.setOnClickListener(view -> activity.gotoDrawingActivity(currentSketch.getId()));

        return convertView;
    }
}
