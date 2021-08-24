package com.water.scrollscreenshot;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.water.scrollscreenshot.Utils.ImageStitcher;
import com.water.scrollscreenshot.listener.TaskListener;
import com.water.scrollscreenshot.task.SaveImageTask;
import com.water.scrollscreenshot.task.SpliceImageTask;
import com.water.scrollscreenshot.view.ProgressBarDialog;

public class StitcherActivity extends AppCompatActivity{
    private static final String TAG = "ImageStitcher";
    private static final int REQUEST_CODE = 1;
    private static final int SPLICE_H = 1;
    private static final int SPLICE_V = 2;
    private RadioGroup mSpliceGroup;
    private RadioButton mSplice_H;
    private RadioButton mSplice_V;

    private int mSpliceOrientation ;
    private ProgressBarDialog mDialog;

    EditText cut_width;
    EditText cut_height;
    EditText com_size;
    private ImageStitcher mImageStitcher;

    private String[]  mPaths;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stitcher);
        mImageStitcher= new ImageStitcher(this);

        mSpliceGroup = findViewById(R.id.splice_orientation);
        mSplice_H = findViewById(R.id.splice_h);
        mSplice_V = findViewById(R.id.splice_v);
        mSpliceGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if(mSplice_H.isChecked()) {
                    mSpliceOrientation=SPLICE_H;
                }else if(mSplice_V.isChecked()) {
                    mSpliceOrientation=SPLICE_V;
                }
                setConfig();
            }
        });
        mSpliceOrientation=SPLICE_V;
        mSplice_V.setChecked(true);
        setConfig();
    }



    private void setConfig(){
        TextView top_tip =findViewById(R.id.l_t_hint);
        TextView bottom_tip=findViewById(R.id.r_b_hint);
        TextView com_tip=findViewById(R.id.com_size_hint);
         cut_width =findViewById(R.id.cut_l_t);
         cut_height=findViewById(R.id.cut_r_b);
         com_size=findViewById(R.id.com_size);

        if(mSpliceOrientation==SPLICE_H){
            top_tip.setText(getResources().getString(R.string.cut_left));
            bottom_tip.setText(getResources().getString(R.string.cut_right));
            com_tip.setText(getResources().getString(R.string.com_width));
            cut_width.setText("250");
            cut_height.setText("150");
            com_size.setText("100");
        }else{
            top_tip.setText(getResources().getString(R.string.cut_top));
            bottom_tip.setText(getResources().getString(R.string.cut_bottom));
            com_tip.setText(getResources().getString(R.string.com_height));
            cut_width.setText("250");
            cut_height.setText("150");
            com_size.setText("100");
        }

    }
    @Override
    protected void onResume() {
        super.onResume();
    }

    public void selectImage(View view){
        Intent intent=new Intent("select_images_action");
        intent.setClass(StitcherActivity.this,com.water.photoselector.PictureSelectorActivity.class);
        startActivityIfNeeded(intent,REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

         if(resultCode==RESULT_OK&&requestCode==REQUEST_CODE){
             if(intent!=null&&intent.getClipData()!=null){
                 Log.e("water","intent.getClipData()= "+intent.getClipData().getItemCount())  ;
                 mPaths =getPathDate(intent);
                 //startSiticher(intent);
             }else{
                 Log.e("water","intent.getClipData()= nullll")  ;
             }
         }
    }
    private String [] getPathDate(Intent intent){
        int count =  intent.getClipData().getItemCount();
        String paths[]=new String[count];
        for (int i = 0; i < count; i++) {
            Uri uri = intent.getClipData().getItemAt(i).getUri();
            String path= uri.toString();
            paths[i]=path;
            Log.e(TAG,"getPath =  "+path);
        }
        return paths;
    }
    private void configStitcher(){
        int cut_w=Integer.parseInt(cut_width.getText().toString());
        int cut_h=Integer.parseInt(cut_width.getText().toString());
        int com_size=Integer.parseInt(cut_width.getText().toString());
        mImageStitcher.setconfig(cut_w,cut_h,com_size,mSpliceOrientation);

    }
    public void startStitcher(View view){
        mDialog = ProgressBarDialog.Create(this, getString(R.string.stitching), "");
        mDialog.show();
        configStitcher();
        SpliceImageTask task =new SpliceImageTask(this,mImageStitcher,mPaths);
        task.setListener(new TaskListener<Bitmap>() {
            @Override
            public void onComplete(Bitmap bitmap) {
                if(bitmap!=null){
                    mDialog.setTitle(getString(R.string.save));
                    startSaveTask(bitmap);
                }else{
                    mDialog.cancel();
                    Toast.makeText(StitcherActivity.this,"Sticher photo failed",Toast.LENGTH_LONG);
                    Log.e("water","onComplete= nullll")  ;
                }

            }
        });
        task.execute();
    }

    private void startSaveTask(Bitmap bitmap){
        SaveImageTask mSaveInBgTask = new SaveImageTask(this, bitmap);
        mSaveInBgTask.setListener(new TaskListener<Uri>() {
            @Override
            public void onComplete(Uri uri) {
                Log.e("water", "  mSaveUri= "+uri);
                if(mDialog!=null){
                    mDialog.cancel();
                }
                createPriviewAction(uri);
            }
        });
        mSaveInBgTask.execute();
    }
    private void createPriviewAction( Uri uri){
        Intent intent = new Intent(this, PreviewAcitvity.class);
        intent.setData(uri);
        if(intent!=null) {
            startActivity(intent);
        }
    }


}