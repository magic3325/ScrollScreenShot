package com.water.scrollscreenshot.task;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.provider.MediaStore.MediaColumns;
import android.text.format.DateUtils;
import com.water.scrollscreenshot.listener.TaskListener;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

public class SaveImageTask extends AsyncTask<Void, Void, Uri> {

    private static final String SCREENSHOT_FILE_NAME_TEMPLATE = "Screenshot_%s.png";
    private static final String SCREENSHOT_ID_TEMPLATE = "Screenshot_%s";
    private static final String SCREENSHOT_SHARE_SUBJECT_TEMPLATE = "Screenshot (%s)";

    private final Context mContext;
    private   Bitmap mBitmap;
    private final String mImageFileName;
    private final long mImageTime;
    private final String mScreenshotId;
    private TaskListener mListener;
    public SaveImageTask(Context context, Bitmap bitmap) {
        mContext =context;
        mBitmap =bitmap;
        mImageTime = System.currentTimeMillis();
        String imageDate = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date(mImageTime));
        mImageFileName = String.format(SCREENSHOT_FILE_NAME_TEMPLATE, imageDate);
        mScreenshotId = String.format(SCREENSHOT_ID_TEMPLATE, UUID.randomUUID());

    }


    @Override
    protected Uri doInBackground(Void... voids) {
        if (isCancelled()) {
            return null;
        }
         Uri uri = null;
        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

        ContentResolver resolver = mContext.getContentResolver();

        Resources r = mContext.getResources();

        try {
            // Save the screenshot to the MediaStore
            final ContentValues values = new ContentValues();
            values.put(MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES
                    + File.separator + Environment.DIRECTORY_SCREENSHOTS);
            values.put(MediaColumns.DISPLAY_NAME, mImageFileName);
            values.put(MediaColumns.MIME_TYPE, "image/png");
            values.put(MediaColumns.DATE_ADDED, mImageTime / 1000);
            values.put(MediaColumns.DATE_MODIFIED, mImageTime / 1000);
            values.put(MediaColumns.DATE_EXPIRES, (mImageTime + DateUtils.DAY_IN_MILLIS) / 1000);
            values.put(MediaColumns.IS_PENDING, 1);

           uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            try {
                // First, write the actual data for our screenshot
                try (OutputStream out = resolver.openOutputStream(uri)) {
                    if (!mBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                        throw new IOException("Failed to compress");
                    }
                }

                // Next, write metadata to help index the screenshot
                try (ParcelFileDescriptor pfd = resolver.openFile(uri, "rw", null)) {
                    final ExifInterface exif = new ExifInterface(pfd.getFileDescriptor());

                    exif.setAttribute(ExifInterface.TAG_SOFTWARE,
                            "Android " + Build.DISPLAY);

                    exif.setAttribute(ExifInterface.TAG_IMAGE_WIDTH,
                            Integer.toString(mBitmap.getWidth()));
                    exif.setAttribute(ExifInterface.TAG_IMAGE_LENGTH,
                            Integer.toString(mBitmap.getHeight()));

                    final ZonedDateTime time = ZonedDateTime.ofInstant(
                            Instant.ofEpochMilli(mImageTime), ZoneId.systemDefault());
                    exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL,
                            DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss").format(time));
                    exif.setAttribute(ExifInterface.TAG_SUBSEC_TIME_ORIGINAL,
                            DateTimeFormatter.ofPattern("SSS").format(time));

                    if (Objects.equals(time.getOffset(), ZoneOffset.UTC)) {
                        exif.setAttribute(ExifInterface.TAG_OFFSET_TIME_ORIGINAL, "+00:00");
                    } else {
                        exif.setAttribute(ExifInterface.TAG_OFFSET_TIME_ORIGINAL,
                                DateTimeFormatter.ofPattern("XXX").format(time));
                    }

                    exif.saveAttributes();
                }

                // Everything went well above, publish it!
                values.clear();
                values.put(MediaColumns.IS_PENDING, 0);
                values.putNull(MediaColumns.DATE_EXPIRES);
                resolver.update(uri, values, null, null);
            } catch (Exception e) {
                resolver.delete(uri, null);
                throw e;
            }

        } catch (Exception e) {

        }

        return uri;
    }


    @Override
    protected void onCancelled(Uri uri) {
        // If we are cancelled while the task is running in the background, we may get null
        // params. The finisher is expected to always be called back, so just use the baked-in
        // params from the ctor in any case.
        if(mBitmap!=null){
            mBitmap.recycle();
            mBitmap=null;
        }

    }

    @Override
    protected void onPostExecute(Uri uri) {
        super.onPostExecute(uri);
       if(mListener!=null){
           mListener.onComplete(uri);
       }
    }

    public void setListener(TaskListener listener){
        mListener=listener;
    }

}
