package com.jsoft.diffusionpaint.component;

import android.app.DialogFragment;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.graphics.drawable.shapes.RectShape;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;

import com.jsoft.diffusionpaint.R;

public class SizePickerFragment extends DialogFragment {

    //For time being, the activity that calls fragment must be an instance of SizePickerListener
    //for the callback.
    public interface SizePickerListener {
        //Notify the caller about the picked size
        void onConfirmSizePick(int size);
        //Retrieve the initial size from the caller
        int getCurrentSize();
    }

    private ImageView mBrushSizeImage;
    private int mSize = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final View view = inflater.inflate(R.layout.dialog_size_picker, container);


        if (getActivity() instanceof SizePickerListener) {
            mSize = ((SizePickerListener) getActivity()).getCurrentSize();
        }
        //setupBrushSizeImage(mSize, view);
        setupSeekBar(view);
        //setupConfirmButton(view);
        return view;
    }

   /* private void setupConfirmButton(View view) {
        ImageButton imagebutton = view.findViewById(R.id.size_picker_button);
        imagebutton.setOnClickListener(new ImageButton.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (getActivity() instanceof SizePickerListener) {
                    ((SizePickerListener) getActivity()).onConfirmSizePick(mSize);
                }
                getDialog().dismiss();
            }
        });
    }*/

    private void setupSeekBar(final View view) {
        SeekBar sizeSeekBar = view.findViewById(R.id.size_picker_seek);
        sizeSeekBar.setProgressDrawable(getOverlapGradient());
        sizeSeekBar.setMax(1200);
        sizeSeekBar.setProgress(mSize * 20);
        sizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mSize = progress < 40 ? 2 : progress/20;
                //setupBrushSizeImage(mSize, view);
                if (getActivity() instanceof SizePickerListener) {
                    ((SizePickerListener) getActivity()).onConfirmSizePick(mSize);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

        });
    }

    /*private void setupBrushSizeImage(int size, View view) {
        if (mBrushSizeImage == null) {
            mBrushSizeImage = view.findViewById(R.id.size_picker_image);
        }
        ShapeDrawable circle = new ShapeDrawable( new OvalShape());
        circle.getPaint().setStrokeWidth(size);
        circle.getPaint().setColor(Color.BLACK);
        circle.setIntrinsicWidth (size);
        circle.setIntrinsicHeight (size);
        circle.setBounds(new Rect(0, 0, size, size));
        circle.setPadding(50,50,50,50);
        mBrushSizeImage.setImageDrawable(circle);
    }*/

    private ShapeDrawable getOverlapGradient() {
        LinearGradient overlap = new LinearGradient(0.f, 0.f, 800.f, 0.0f,
                new int[]{ 0xFFFFFF00, 0xFFFFFF00},
                null, Shader.TileMode.CLAMP);
        ShapeDrawable shape = new ShapeDrawable(new RectShape());
        shape.getPaint().setShader(overlap);
        return shape;
    }

}

