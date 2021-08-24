package com.water.scrollscreenshot.task;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Rect;
import android.net.Uri;
import android.os.AsyncTask;

import com.water.scrollscreenshot.listener.ImageLoadListener;

import java.io.IOException;

public class BitmapLoadTask extends AsyncTask<Void, Void, Bitmap> {
    private static final String TAG = "ImageStitcher";
    private Uri mUri;
    private int mSWidth;
    private int mSHeight;
    private ImageLoadListener listener;
    private  Context mContext;
    public BitmapLoadTask(Context context, Uri uri) {
        mContext =context;
        this.mUri=uri;
    }

    @Override
    protected Bitmap doInBackground(Void... voids) {
        Bitmap bp =null;
        BitmapRegionDecoder decoder = null;
        try {
            decoder = BitmapRegionDecoder.newInstance(mContext.getContentResolver().openInputStream(mUri), true);
            mSWidth = decoder.getWidth();
            mSHeight = decoder.getHeight();

            Rect rect = new Rect(0,0,mSWidth,mSHeight);
            BitmapFactory.Options options = new BitmapFactory.Options();
            bp =  decoder.decodeRegion(rect,options);


        } catch (IOException e) {

        }finally {
            if(decoder!=null){
                decoder.recycle();
            }
        }

        return bp;
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        super.onPostExecute(bitmap);
        if(listener!=null&&bitmap!=null){
            listener.onImageLoaded(mSWidth,mSHeight,bitmap);
        }

    }
    public void setListener(ImageLoadListener l){
        listener =l;
    }


}
