package com.water.photoselector.listener;

import java.util.List;


public interface OnQueryDataResultListener<T> {

    void onComplete(List<T> data);
}
