package com.water.scrollscreenshot.task;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import com.water.scrollscreenshot.Utils.ImageStitcher;
import com.water.scrollscreenshot.listener.TaskListener;


public class SpliceImageTask extends AsyncTask<Void, Void, Bitmap> {
    private static final String TAG = "ImageStitcher";
    private final Context mContext;
    private Intent mIntent;
    ImageStitcher mImageStitcher;
    private String[]  mPaths;
    public SpliceImageTask(Context context,ImageStitcher stitcher, String[] paths) {
        mContext =context;
        mImageStitcher=stitcher;
        mPaths = paths;
    }
    private TaskListener<Bitmap> listener;
    public void setListener(TaskListener l){
        listener=l;
    }


    @Override
    protected Bitmap doInBackground(Void... voids) {
//        Uri contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
//        int count =  mIntent.getClipData().getItemCount();
//        String paths[]=new String[count];
//        for (int i = 0; i < count; i++) {
//            Uri uri = mIntent.getClipData().getItemAt(i).getUri();
//            String path= uri.toString();
//            paths[i]=path;
//            Log.e(TAG,"getPath =  "+path);
//        }
        boolean sucess = mImageStitcher.stitcherImage(mPaths);
        Log.e(TAG,"sucess =  " +sucess);
        if(sucess){
            return mImageStitcher.getImage();
        }
        return null;
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        super.onPostExecute(bitmap);
        if(listener!=null){
            listener.onComplete(bitmap);
        }
    }


}
