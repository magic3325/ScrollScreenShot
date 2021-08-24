package com.water.scrollscreenshot;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.water.scrollscreenshot.view.GlobalScreenshot;


public class TakeScreenshotService extends Service {
    private static final String TAG = "ImageStitcher";
    public static final int SCROLL_SCREENSHORT = 0;
    public static final int SCREENSHORT = 1;
    GlobalScreenshot mGlobalScreenshot;
    private Handler mHandler;
    @Override
    public void onCreate() {
        super.onCreate();
        mGlobalScreenshot =new GlobalScreenshot(this);
        Log.e(TAG,"onCreate");
        mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case SCROLL_SCREENSHORT:
                        mGlobalScreenshot.takeScreenshot(true);
                        break;
                }
            }
        };
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(TAG,"onDestroy");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.e(TAG,"onBind");
        return null;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG,"onStartCommand");
        if(intent!=null){
            boolean isScroll=   intent.getBooleanExtra("scroll_screenshort",false);
            if(isScroll){
                mHandler.sendEmptyMessageDelayed(SCROLL_SCREENSHORT,800);
            }else{
                mGlobalScreenshot.takeScreenshot(false);
            }
        }else{
            mGlobalScreenshot.takeScreenshot(false);
        }

        return super.onStartCommand(intent, flags, startId);
    }

}
