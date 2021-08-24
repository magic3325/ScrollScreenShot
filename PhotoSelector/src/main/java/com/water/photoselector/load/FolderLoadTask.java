package com.water.photoselector.load;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.util.Log;

import com.water.photoselector.bean.LocalMediaFolder;
import com.water.photoselector.listener.OnQueryDataResultListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FolderLoadTask extends AsyncTask<Void, Void, Void> {

    private static final long FILE_SIZE_UNIT = 1024 * 1024L;
    private static final Uri QUERY_URI = MediaStore.Files.getContentUri("external");
    private static final String ORDER_BY = MediaStore.Files.FileColumns._ID + " DESC";
    private static final String NOT_GIF_UNKNOWN = "!='image/*'";
    private static final String NOT_GIF = " AND (" + MediaStore.MediaColumns.MIME_TYPE + "!='image/gif' AND " + MediaStore.MediaColumns.MIME_TYPE + NOT_GIF_UNKNOWN + ")";
    private static final String GROUP_BY_BUCKET_Id = " GROUP BY (bucket_id";
    private static final String COLUMN_COUNT = "count";
    private static final String COLUMN_BUCKET_ID = "bucket_id";
    private static final String COLUMN_BUCKET_DISPLAY_NAME = "bucket_display_name";
    private final Context mContext;
    private static final String[] PROJECTION = {
            MediaStore.Files.FileColumns._ID,
            COLUMN_BUCKET_ID,
            COLUMN_BUCKET_DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE};
  public FolderLoadTask(Context context){
        mContext=context;
    }

    private OnQueryDataResultListener<LocalMediaFolder> mListener;
    @Override
    protected Void doInBackground(Void... voids) {
        //String select ="media_type=? AND (mime_type!='image/gif' AND mime_type!='image/*') AND 1024 <= _size and _size <= 9223372036854775807";
        String select ="media_type=?";
        Cursor data = mContext.getContentResolver().query(QUERY_URI,
                PROJECTION,
                select, new String[]{String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE)}, ORDER_BY);
        try {
            if (data != null) {
                int count = data.getCount();
                int totalCount = 0;
                List<LocalMediaFolder> mediaFolders = new ArrayList<>();
                if (count > 0) {
                        Map<Long, Long> countMap = new HashMap<>();
                        while (data.moveToNext()) {
                            long bucketId = data.getLong(data.getColumnIndex(COLUMN_BUCKET_ID));
                            Long newCount = countMap.get(bucketId);
                            if (newCount == null) {
                                newCount = 1L;
                            } else {
                                newCount++;
                            }
                            countMap.put(bucketId, newCount);
                        }

                        if (data.moveToFirst()) {
                            Set<Long> hashSet = new HashSet<>();
                            do {
                                long bucketId = data.getLong(data.getColumnIndex(COLUMN_BUCKET_ID));
                                if (hashSet.contains(bucketId)) {
                                    continue;
                                }
                                LocalMediaFolder mediaFolder = new LocalMediaFolder();
                                mediaFolder.setBucketId(bucketId);
                                String bucketDisplayName = data.getString(
                                        data.getColumnIndex(COLUMN_BUCKET_DISPLAY_NAME));
                                String mimeType = data.getString(data.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE));
                                long size = countMap.get(bucketId);
                                long id = data.getLong(data.getColumnIndex(MediaStore.Files.FileColumns._ID));
                                mediaFolder.setName(bucketDisplayName);
                                mediaFolder.setImageNum(Integer.valueOf((int) size));
                                mediaFolder.setFirstImagePath(ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id).toString());
                                mediaFolder.setFirstMimeType(mimeType);
                                mediaFolders.add(mediaFolder);
                                hashSet.add(bucketId);
                                totalCount += size;
                            } while (data.moveToNext());
                        }
                        if(mListener!=null){
                            mListener.onComplete(mediaFolders);
                        }

            Log.e("FolderLoadTask","mediaFolders ="+mediaFolders.size());
                    return null;
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

    public void setListener(OnQueryDataResultListener<LocalMediaFolder> listener){
        mListener =listener;
    }
    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);


    }
}
