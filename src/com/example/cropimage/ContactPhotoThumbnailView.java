package com.example.cropimage;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

public class ContactPhotoThumbnailView extends ImageView {
    public ContactPhotoThumbnailView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        getLayoutParams().height = MeasureSpec.getSize(widthMeasureSpec);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
