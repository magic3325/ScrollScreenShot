package com.water.scrollscreenshot;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.water.scrollscreenshot.listener.onSingleTapListener;
import com.water.scrollscreenshot.view.LargeImageView;

public class PreviewAcitvity extends AppCompatActivity implements onSingleTapListener {
    private static final String TAG = "ImageStitcher";
    private static final int MESSAGE_SHOW_BAR = 2;
    private static final int SHOW_BAR_TIMEOUT_MILLIS = 6000;
    private LargeImageView mPreview;
    private View mRootView;
    private View mHeadView;
    private View mFooterView;
    private boolean mShowBars = true;
    private Uri mUri;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Window window = getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        window.setAttributes(lp);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.preview_layout);

        mPreview =findViewById(R.id.preview_view);
        mRootView = findViewById(R.id.view_root);
        mHeadView = findViewById(R.id.header);
        mFooterView = findViewById(R.id.footer);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION
                        |WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                        |WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(Color.TRANSPARENT);
//
       // showSystemUi(true);
        Intent intent =getIntent();
        if(intent==null||intent.getData()==null){
           finish();
        }
        mUri=intent.getData();
        mPreview.setImage(mUri);
        mPreview.setonSingleTapListener(this);
    }
    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_SHOW_BAR:
                    mShowBars =!mShowBars;
                    showBar(mShowBars);
                    break;
            }
        }
    };

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
              onBackPressed();
             return true;
            }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    private void showSystemUi(boolean visible) {
        int flags = 0;
        if (visible) {
            flags = View.STATUS_BAR_HIDDEN
                    |View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    |View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    |View.SYSTEM_UI_FLAG_IMMERSIVE;
        }
        getWindow().getDecorView().setSystemUiVisibility(flags);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mHandler.removeMessages(MESSAGE_SHOW_BAR);
        mHandler.sendEmptyMessageDelayed(MESSAGE_SHOW_BAR,SHOW_BAR_TIMEOUT_MILLIS);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPreview.relaseView();
    }


    @Override
    public void onSingleTap() {
        mShowBars =!mShowBars;
        showBar(mShowBars);
    }

    private void showBar(boolean show){
        mShowBars =show;
        float start =show?0f:1f;
        float end =show?1f:0f;
        AnimatorSet animatorSet = new AnimatorSet();

        ValueAnimator anima = ValueAnimator.ofFloat(start, end);
        anima.setDuration(150);
        anima.addUpdateListener(animation -> {
            float t = (float)animation.getAnimatedValue();
            mHeadView.setAlpha(t);
            mFooterView.setAlpha(t);
            mFooterView.setTranslationY(mFooterView.getMeasuredHeight()*(1-t));
        });
        anima.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation, boolean isReverse) {
                if(show){
                    mHeadView.setVisibility(View.VISIBLE);
                    mFooterView.setVisibility(View.VISIBLE);
                    mHeadView.setAlpha(0f);
                    mFooterView.setAlpha(0f);
                    showSystemUi(false);
                }else{
                    showSystemUi(true);
                }
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if(show){
                    mHeadView.setVisibility(View.VISIBLE);
                    mFooterView.setVisibility(View.VISIBLE);
                    mHandler.removeMessages(MESSAGE_SHOW_BAR);
                    mHandler.sendEmptyMessageDelayed(MESSAGE_SHOW_BAR,SHOW_BAR_TIMEOUT_MILLIS);
                }else{
                    mHeadView.setVisibility(View.GONE);
                    mFooterView.setVisibility(View.GONE);
                }
            }
        });
        animatorSet.play(anima);
        animatorSet.start();
    }
    public void shareAction(View view){
        if(mUri!=null){
            Intent sharingIntent = new Intent(Intent.ACTION_SEND);
            sharingIntent.setType("image/png");
            sharingIntent.putExtra(Intent.EXTRA_STREAM, mUri);
            ClipData clipdata = new ClipData(new ClipDescription("content",
                    new String[]{ClipDescription.MIMETYPE_TEXT_PLAIN}),
                    new ClipData.Item(mUri));
            sharingIntent.setClipData(clipdata);
            sharingIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            sharingIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(sharingIntent);
            finish();
        }

    }
    public void editAction( View view){
        if(mUri!=null) {
            Intent editIntent = new Intent(Intent.ACTION_EDIT);
            editIntent.setType("image/png");
            editIntent.setData(mUri);
            editIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            editIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            editIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(editIntent);
            finish();
        }
    }

    public void deleteAction( View view){
        if(mUri!=null) {
            ContentResolver resolver = getContentResolver();
            resolver.delete(mUri, null, null);
            finish();
        }
    }
}
