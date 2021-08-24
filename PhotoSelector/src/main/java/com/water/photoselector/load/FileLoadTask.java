package com.water.photoselector.load;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.text.TextUtils;

import com.water.photoselector.bean.LocalMedia;
import com.water.photoselector.bean.LocalMediaFolder;
import com.water.photoselector.listener.OnQueryDataResultListener;

import java.util.ArrayList;
import java.util.List;

public class FileLoadTask extends AsyncTask<Void, Void, Void> {
    private static final Uri QUERY_URI = MediaStore.Files.getContentUri("external");
    private static final String[] PROJECTION_PAGE = {
            MediaStore.Files.FileColumns._ID,
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.WIDTH,
            MediaStore.MediaColumns.HEIGHT,
            MediaStore.MediaColumns.DURATION,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.BUCKET_ID,
            MediaStore.MediaColumns.DATE_ADDED};

    long mBucketId;
    Context mContext;

    public final static String MIME_TYPE_JPEG = "image/jpeg";
    private OnQueryDataResultListener<LocalMedia> mListener;

    public FileLoadTask(Context context , long id){
        mContext=context;
        mBucketId=id;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        String select ="media_type=? AND bucket_id=?";
        String[] selectArgs=new String[]{String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE), String.valueOf(mBucketId)};
        Cursor data =  mContext.getContentResolver().query(QUERY_URI,PROJECTION_PAGE,select,selectArgs,null);
        try {
            if (data != null) {
                List<LocalMedia> result = new ArrayList<>();
                if (data.getCount() > 0) {
                    int idColumn = data.getColumnIndexOrThrow(PROJECTION_PAGE[0]);
                    int dataColumn = data.getColumnIndexOrThrow(PROJECTION_PAGE[1]);
                    int mimeTypeColumn = data.getColumnIndexOrThrow(PROJECTION_PAGE[2]);
                    int widthColumn = data.getColumnIndexOrThrow(PROJECTION_PAGE[3]);
                    int heightColumn = data.getColumnIndexOrThrow(PROJECTION_PAGE[4]);
                    int durationColumn = data.getColumnIndexOrThrow(PROJECTION_PAGE[5]);
                    int sizeColumn = data.getColumnIndexOrThrow(PROJECTION_PAGE[6]);
                    int folderNameColumn = data.getColumnIndexOrThrow(PROJECTION_PAGE[7]);
                    int fileNameColumn = data.getColumnIndexOrThrow(PROJECTION_PAGE[8]);
                    int bucketIdColumn = data.getColumnIndexOrThrow(PROJECTION_PAGE[9]);
                    int dateAddedColumn = data.getColumnIndexOrThrow(PROJECTION_PAGE[10]);
                    data.moveToFirst();
                    do {
                        long id = data.getLong(idColumn);
                        String mimeType = data.getString(mimeTypeColumn);
                        mimeType = TextUtils.isEmpty(mimeType) ?MIME_TYPE_JPEG : mimeType;
                        String absolutePath = data.getString(dataColumn);
                        String url = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id).toString();

                        int width = data.getInt(widthColumn);
                        int height = data.getInt(heightColumn);
                        long duration = data.getLong(durationColumn);
                        long size = data.getLong(sizeColumn);
                        String folderName = data.getString(folderNameColumn);
                        String fileName = data.getString(fileNameColumn);
                        long bucket_id = data.getLong(bucketIdColumn);

                        LocalMedia image = LocalMedia.parseLocalMedia(id, url, absolutePath, fileName, folderName, duration,  mimeType, width, height, size, bucket_id, data.getLong(dateAddedColumn));
                        result.add(image);

                    } while (data.moveToNext());

                    if(mListener!=null){
                        mListener.onComplete(result);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
        if (data != null && !data.isClosed()) {
            data.close();
        }
    }
        return null;
    }

    public void setListener(OnQueryDataResultListener<LocalMedia> listener){
        mListener =listener;
    }

}
