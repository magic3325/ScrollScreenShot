package com.water.photoselector.listener;

import java.util.List;

public interface OnPhotoSelectListener<T> {

    void onTakePhoto();

    void onChange(List<T> data);

    void onPictureClick(T data, int position);
}
