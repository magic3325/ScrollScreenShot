package com.water.photoselector;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.water.photoselector.adapter.GridMarginDecoration;
import com.water.photoselector.adapter.ImageGridAdapter;
import com.water.photoselector.bean.LocalMedia;
import com.water.photoselector.bean.LocalMediaFolder;
import com.water.photoselector.listener.OnAlbumItemClickListener;
import com.water.photoselector.listener.OnPhotoSelectListener;
import com.water.photoselector.listener.OnQueryDataResultListener;
import com.water.photoselector.load.FileLoadTask;
import com.water.photoselector.load.FolderLoadTask;
import com.water.photoselector.view.RecyclerPreloadView;
import com.water.photoselector.R;
import java.util.ArrayList;
import java.util.List;

public class PictureSelectorActivity extends AppCompatActivity implements View.OnClickListener, OnAlbumItemClickListener {

    protected boolean isEnterSetting;
    protected FolderPopWindow mFolderWindow;
    private TextView mDirTitle;
    private TextView mSelectDone;
    private View mDirView;
    private View mBack;
    protected View mTitleBar;
    protected RecyclerView mRecyclerView;
    protected ImageGridAdapter mAdapter;
    public int maxSelectNum = 9;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.picture_selector);

        mTitleBar = findViewById(R.id.titleBar);
        mDirTitle= findViewById(R.id.dir_title);
        mDirView= findViewById(R.id.album_layout);
        mDirView.setOnClickListener(this);
        mSelectDone=findViewById(R.id.select_done);
        mSelectDone.setOnClickListener(this);
        mBack =findViewById(R.id.back);
        mBack.setOnClickListener(this);
        mFolderWindow= findViewById(R.id.folder_view);
        mFolderWindow.setView(findViewById(R.id.arrow_view),mDirTitle);
        mFolderWindow.setOnAlbumItemClickListener(this);

        mRecyclerView = findViewById(R.id.picture_recycler);
        mRecyclerView.addItemDecoration(new GridMarginDecoration(4, 4));
        mRecyclerView.setLayoutManager(new GridLayoutManager(this, 4));
        mRecyclerView.setHasFixedSize(true);
        RecyclerView.ItemAnimator itemAnimator = mRecyclerView.getItemAnimator();
        if (itemAnimator != null) {
            ((SimpleItemAnimator) itemAnimator).setSupportsChangeAnimations(false);
            mRecyclerView.setItemAnimator(null);
        }
        mAdapter = new ImageGridAdapter(this);
        mRecyclerView.setAdapter(mAdapter);
        mAdapter.setOnSelectListener(OnSelectListener);
        loadAllMediaData();
    }
    public  boolean checkSelfPermission(Context ctx, String permission) {
        return ContextCompat.checkSelfPermission(ctx.getApplicationContext(), permission)
                == PackageManager.PERMISSION_GRANTED;
    }
    public  void requestPermissions(Activity activity, @NonNull String[] permissions, int code) {
        ActivityCompat.requestPermissions(activity, permissions, code);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void loadAllMediaData() {
        if (checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            readLocalMedia();
        } else {
            requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }
    }

    protected void readLocalMedia() {
        //showPleaseDialog();
        FolderLoadTask task=new FolderLoadTask(this);
            task.setListener(new OnQueryDataResultListener<LocalMediaFolder>() {
                @Override
                public void onComplete(List<LocalMediaFolder> data) {
                    mFolderWindow.setFolderDate(data);
                    FileLoadTask task =new FileLoadTask(PictureSelectorActivity.this,data.get(0).getBucketId());
                    task.setListener(onImageLoad);
                    task.execute();
                }
            });
         task.execute();
    }




    @Override
    public void onClick(View v) {
        if(v.getId()==R.id.album_layout){
            if (mFolderWindow.isShowing()) {
                mFolderWindow.showDirPopWindow(false);
            }else if(!mFolderWindow.isEmpty()){
                mFolderWindow.showDirPopWindow(true);
            }
        }else  if(v.getId()==R.id.select_done){
            onComplete();
        }else if(v.getId()==R.id.back){
            finish();
        }
    }

    @Override
    public void onItemClick(int position, boolean isCameraFolder, long bucketId, String folderName) {
        mDirTitle.setText(folderName);
        if (mFolderWindow.isShowing()) {
            mFolderWindow.showDirPopWindow(false);
        }else if(!mFolderWindow.isEmpty()){
            mFolderWindow.showDirPopWindow(true);
        }
        FileLoadTask task =new FileLoadTask(this,bucketId);
        task.setListener(onImageLoad);
        task.execute();
    }
    OnQueryDataResultListener<LocalMedia> onImageLoad =new OnQueryDataResultListener<LocalMedia>(){

        @Override
        public void onComplete(List<LocalMedia> data) {
            Log.e("water","onComplete= "+data.size());
            mAdapter.bindData(data);
        }
    };

    OnPhotoSelectListener<LocalMedia> OnSelectListener =new OnPhotoSelectListener<LocalMedia>(){

        @Override
        public void onTakePhoto() {

        }

        @Override
        public void onChange(List<LocalMedia> data) {
            changeImageNumber(data);
        }

        @Override
        public void onPictureClick(LocalMedia data, int position) {

        }

    };
    protected void changeImageNumber(List<LocalMedia> selectData) {
        boolean enable = selectData.size() != 0;
        if(enable){
            mSelectDone.setVisibility(View.VISIBLE);
            mSelectDone.setClickable(true);
            mSelectDone.setText(getString(R.string.picture_select_num, selectData.size(), maxSelectNum));
        }else{
            mSelectDone.setVisibility(View.GONE);
        }

    }
    private void onComplete() {

        List<LocalMedia> items = mAdapter.getSelectedList();
        int size = items.size();
        final ContentResolver resolver = getContentResolver();
         ArrayList<ClipData.Item> clipItems = new ArrayList<>();
        final ClipDescription clipDescription = new ClipDescription("", new String[] {ClipDescription.MIMETYPE_TEXT_URILIST });
        for (LocalMedia item : items) {
          if(!TextUtils.isEmpty(item.getPath())) {
              clipItems.add(new ClipData.Item(Uri.parse(item.getRealPath())));
          }
        }
        ClipData clip = new ClipData(clipDescription, clipItems.get(0));
        for (int i = 1; i < clipItems.size(); i++) {
            clip.addItem(clipItems.get(i));
          }
        Intent intent=new Intent();
        intent.setClipData(clip);
        setResult(RESULT_OK,intent);
        finish();

    }

}
