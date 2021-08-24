package com.water.scrollscreenshot.listener;

import android.graphics.Bitmap;

public interface ImageLoadListener {
    void onImageLoaded(int w, int h, Bitmap b);
}
