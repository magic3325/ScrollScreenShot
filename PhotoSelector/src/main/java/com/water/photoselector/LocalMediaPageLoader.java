package com.water.photoselector;

import android.content.Context;
import android.net.Uri;
import android.provider.MediaStore;

public class LocalMediaPageLoader {


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

    public LocalMediaPageLoader(Context context) {
        mContext = context;

    }
















}
