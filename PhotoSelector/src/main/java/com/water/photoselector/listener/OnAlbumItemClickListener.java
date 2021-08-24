package com.water.photoselector.listener;

public interface OnAlbumItemClickListener {
    void onItemClick(int position, boolean isCameraFolder,
                     long bucketId, String folderName);
}
