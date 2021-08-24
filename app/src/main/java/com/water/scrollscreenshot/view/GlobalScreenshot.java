package com.water.scrollscreenshot.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.app.Instrumentation;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Region;
import android.hardware.input.InputManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.InputChannel;
import android.view.InputDevice;
import android.view.InputEvent;
import android.view.InputEventReceiver;
import android.view.InputMonitor;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceControl;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityManager;
import android.widget.TextView;

import com.google.android.material.math.MathUtils;
import com.water.scrollscreenshot.PreviewAcitvity;
import com.water.scrollscreenshot.R;
import com.water.scrollscreenshot.Utils.ImageStitcher;
import com.water.scrollscreenshot.listener.TaskListener;
import com.water.scrollscreenshot.view.PreviewImageView.ActionStatus;
import com.water.scrollscreenshot.task.SaveImageTask;

public class GlobalScreenshot implements ViewTreeObserver.OnComputeInternalInsetsListener , PreviewImageView.onActionListener {

    private static final String TAG = "ImageStitcher";

    private static final int SCREENSHOT_CORNER_DEFAULT_TIMEOUT_MILLIS = 6000;
    private static final int MESSAGE_CORNER_TIMEOUT = 2;

    public static final int META_SCROLL_LOCK_ON = 0x800000;
    public static final int STATUS_SCROLLING = 0;
    public static final int STATUS_SCREENSHORT = 1;
    public static final int STATUS_STITCHING = 2;
    public static final int STATUS_STOP = 3;
    public static final int STATUS_TIME_OUT = 4;
    public static final int STATUS_CHANGE_TITLE = 5;

    private Uri mSaveUri;

    private final Context mContext;
    private final WindowManager mWindowManager;
    private final WindowManager.LayoutParams mWindowLayoutParams;
    private final Display mDisplay;
    private final DisplayMetrics mDisplayMetrics;
    private Bitmap mScreenBitmap;
    private View mScreenshotLayout;
    private PreviewImageView mScreenshotPreview;
    private View mScrollViewLayout;
    private TextView mScrollTitle;
    private  int mScrollTitleHeight;
    private Instrumentation mInst;
    private  int mTouchstep;
    private  int mTouchX;
    private  int mTouchY;
    private boolean isScrollShot=false;
    private boolean isScrolling=false;
    private boolean isEventSync=true;
    private boolean isInterceptTouch=false;
    private static int mDuration = 1500;
    ImageStitcher mImageStitcher;
    private  int mDisplayId;
    private InputMonitor mInputMonitor;
    private InputEventReceiver mInputEventReceiver;


    public GlobalScreenshot(Context context) {
        Resources r = context.getResources();
        mContext = context;
        initView();
        mWindowLayoutParams = new WindowManager.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 0, 0,
                WindowManager.LayoutParams.TYPE_SCREENSHOT,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                        | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
                PixelFormat.TRANSLUCENT);
        mWindowLayoutParams.setTitle("Screenshot");
        mWindowLayoutParams.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
        // mWindowLayoutParams.setFitInsetsTypes(0 /* types */);


        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        mDisplay = mWindowManager.getDefaultDisplay();
        mDisplayMetrics = new DisplayMetrics();
        mDisplay.getRealMetrics(mDisplayMetrics);
    }

    @Override
    public void onComputeInternalInsets(ViewTreeObserver.InternalInsetsInfo inoutInfo) {
        inoutInfo.setTouchableInsets(ViewTreeObserver.InternalInsetsInfo.TOUCHABLE_INSETS_REGION);
        Region touchRegion = new Region();

        if(isScrollShot){
            Rect left = new Rect(0,0,mDisplayMetrics.widthPixels/2-1,mDisplayMetrics.heightPixels);
            Rect right = new Rect(mDisplayMetrics.widthPixels/2+1,0,mDisplayMetrics.widthPixels,mDisplayMetrics.heightPixels);
            touchRegion.op(left, Region.Op.UNION);
            touchRegion.op(right, Region.Op.UNION);
            inoutInfo.touchableRegion.set(touchRegion);
        }else{
            Rect screenshotRect = new Rect();
            mScreenshotPreview.getBoundsOnScreen(screenshotRect);
            touchRegion.op(screenshotRect, Region.Op.UNION);
            inoutInfo.touchableRegion.set(touchRegion);
        }

    }

    private final Handler mScreenshotHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_CORNER_TIMEOUT:
                    dismissScreenshot("timeout", false);
                    break;
                case STATUS_SCREENSHORT:
                    takeScreenshot(true);
                    break;
                case STATUS_SCROLLING:

                    onScrolling();
                    break;
                case STATUS_CHANGE_TITLE:
                    mScreenshotHandler.post(() -> {
                        if (mScrollTitle != null) {
                            mScrollTitle.setText(R.string.stitching);
                            mWindowManager.updateViewLayout(mScreenshotLayout,mWindowLayoutParams);

                        }
                    });
                    break;
                case STATUS_STOP:
                    mScreenshotHandler.post(() -> {
                        if(mScrollTitle!=null){
                            mScrollTitle.setText(R.string.stitching);
                            mWindowManager.updateViewLayout(mScreenshotLayout,mWindowLayoutParams);
                        }
                        saveScreenshotThread();
                    });
                    break;
                default:
                    break;
            }
        }
    };


    private void initView() {
        if(mScreenshotLayout==null){
            mScreenshotLayout = LayoutInflater.from(mContext).inflate(R.layout.global_screenshot, null);
        }
        mScreenshotLayout.setFocusableInTouchMode(true);
        mScreenshotLayout.requestFocus();
        mScreenshotLayout.setFocusable(true);

        mScreenshotLayout.setOnTouchListener((v, event) -> {
            // Log.i(TAG,"onTouch  ="+event.getAction());
            //Log.e(TAG, " event.getAction()=  " + event.getAction() + "  getMetaState= " + event.getMetaState());
            if (event.getActionMasked() == MotionEvent.ACTION_OUTSIDE) {
                // Once the user touches outside, stop listening for input
                setWindowFocusable(false);
            }
            return false;
        });
        mScreenshotLayout.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    // dismissScreenshot("back pressed", true);
                    return true;
                }
                return false;
            }
        });
        // Get focus so that the key events go to the layout.
        mScreenshotLayout.setFocusableInTouchMode(true);
        mScreenshotLayout.requestFocus();

        mScreenshotPreview = mScreenshotLayout.findViewById(R.id.screenshot_preview);
        mScreenshotPreview.setVisibility(View.VISIBLE);
        mScreenshotPreview.setAlpha(0f);

        mScrollViewLayout = mScreenshotLayout.findViewById(R.id.scroll_title_layout);
        mScrollTitle =(TextView)mScreenshotLayout.findViewById(R.id.title);
        mScrollTitle.setText(R.string.scrolling);
        mScrollViewLayout.setVisibility(View.GONE);
        mScreenshotPreview.setOnActionListener(this);

    }
    public void initScrollConfig(){
        isScrolling=true;
        isEventSync=true;
        isInterceptTouch=false;
        mScrollTitleHeight =mContext.getResources().getDimensionPixelSize(R.dimen.title_height);
        int setp=(mDisplayMetrics.heightPixels-mScrollTitleHeight)/5;
        mTouchstep=setp*3;
        mTouchX=mDisplayMetrics.widthPixels/2;
        mTouchY=setp*4+mScrollTitleHeight;
        mInst = new Instrumentation();
        mImageStitcher= new ImageStitcher(mContext);
        mDisplayId = mContext.getDisplayId();
        mInputMonitor = InputManager.getInstance().monitorGestureInput("Screenshot", mDisplayId);
        mInputEventReceiver = new ScrollInputEventReceiver(mInputMonitor.getInputChannel(), Looper.getMainLooper());
        mScreenshotPreview.setVisibility(View.GONE);

    }
    public void takeScreenshot(boolean isScroll){
        if(!isScroll){
            dismissScreenshot("new screenshot requested", true);
        }
        Rect screenRect =new Rect(0,0,mDisplayMetrics.widthPixels,mDisplayMetrics.heightPixels);
        int rot = mDisplay.getRotation();
        Bitmap source =SurfaceControl.screenshot(screenRect, screenRect.width(), screenRect.height(), rot);
        mScreenBitmap = source.copy(Bitmap.Config.ARGB_8888, true);
        if (mScreenBitmap == null) {
            return;
        }
        mScreenBitmap.setHasAlpha(false);
        mScreenBitmap.prepareToDraw();
        setWindowFocusable(true);
        isScrollShot=isScroll;
        if(isScrollShot) {
            mScreenshotHandler.post(() -> {
                if (mScreenBitmap!=null && mScrollViewLayout.getVisibility() == View.GONE) {
                    initScrollConfig();
                    startTitleDropInAnima();
                } else {
                    boolean succes = mImageStitcher.stitcherImage(mScreenBitmap);
                    Log.i(TAG, "succes  =" + succes);
                    mScreenshotHandler.sendEmptyMessage(isScrolling && succes ? STATUS_SCROLLING : STATUS_STOP);
                }
            });
        }else{
            initView();
            startAnimation();
        }


    }
    private void startAnimation() {
        mScreenshotHandler.post(() -> {
            if (!mScreenshotLayout.isAttachedToWindow()) {
                mWindowManager.addView(mScreenshotLayout, mWindowLayoutParams);
            }
            mScreenshotPreview.setBitmap(mScreenBitmap);
            saveScreenshotInWorkerThread();
            mScreenshotHandler.post(() -> {
                mScreenshotLayout.getViewTreeObserver().addOnComputeInternalInsetsListener(this);
                mScreenshotPreview.startDropInAnima();
            });
        });
    }

    private void startTitleDropInAnima(){
        mScreenshotHandler.post(() -> {
            if (!mScreenshotLayout.isAttachedToWindow()) {
                mWindowManager.addView(mScreenshotLayout, mWindowLayoutParams);
            }
            boolean succes= mImageStitcher.stitcherImage(mScreenBitmap);
            Log.i(TAG,"succes  ="+succes);
            mScreenshotHandler.post(() -> {
                mScreenshotLayout.getViewTreeObserver().addOnComputeInternalInsetsListener(this);
                Animator anima=createDropInAnima();
                mScrollViewLayout.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                mScrollViewLayout.buildLayer();
                anima.start();
            });

        });
    }
    private AnimatorSet createDropInAnima(){
        ValueAnimator yAnim = ValueAnimator.ofFloat(1f, 0);
        yAnim.setDuration(350);
        yAnim.addUpdateListener(animation -> {
            float yDelta = MathUtils.lerp(-mScrollTitleHeight, 0, animation.getAnimatedFraction());
            mScrollViewLayout.setTranslationY(yDelta);

        });
        yAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation, boolean isReverse) {
                mScrollViewLayout.setVisibility(View.VISIBLE);

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                mScrollViewLayout.setLayerType(View.LAYER_TYPE_NONE, null);
                mScrollViewLayout.setTranslationY(0);
                mScreenshotHandler.sendEmptyMessage( STATUS_SCROLLING);
            }
        });

        AnimatorSet animSet = new AnimatorSet();
        animSet.play(yAnim);
        return animSet;
    }




    private void stopScrollScreenshot(){
        Log.e(TAG,"stopScroll= ");
        if(mScreenshotLayout!=null&&mScreenshotLayout.isAttachedToWindow()) {
            mWindowManager.removeView(mScreenshotLayout);
        }
        isEventSync=true;
        isScrolling=false;
        isInterceptTouch=true;
        mScrollViewLayout.setVisibility(View.GONE);
        mScreenshotHandler.removeMessages(STATUS_SCREENSHORT);
        mScreenshotHandler.removeMessages(STATUS_SCROLLING);
        mScreenshotHandler.removeMessages(STATUS_STOP);
        disposeInputChannel();
        createPriviewAction(mSaveUri);
    }
    private void onScrolling(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    long down = SystemClock.uptimeMillis();
                    final long endTime = down + mDuration;
                    long now = SystemClock.uptimeMillis();
                    Instrumentation iso = new Instrumentation();
                    iso.sendPointerSync(MotionEvent.obtain(down, down, MotionEvent.ACTION_DOWN, mTouchX, mTouchY, META_SCROLL_LOCK_ON));
                    while ((now < endTime)&&!isInterceptTouch) {
                        final long elapsedTime = now - down;
                        final float alpha = (float) elapsedTime / mDuration;

                        float dy = MathUtils.lerp(mTouchY, mTouchY - mTouchstep, alpha);
                        if(isEventSync) {
                            isEventSync = false;
                            now = SystemClock.uptimeMillis();
                            MotionEvent ev = MotionEvent.obtain(now, now, MotionEvent.ACTION_MOVE, mTouchX, dy, META_SCROLL_LOCK_ON);
                            iso.sendPointerSync(ev);
                        }
                    }
                    if(!isInterceptTouch) {
                        now = SystemClock.uptimeMillis();
                        iso.sendPointerSync(MotionEvent.obtain(now, now, MotionEvent.ACTION_UP, mTouchX, mTouchY - mTouchstep, META_SCROLL_LOCK_ON));
                    }

                    Thread.sleep(1000);

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }catch (SecurityException e){
                    Log.e(TAG,"SecurityException= "+ e);
                }
                mScreenshotHandler.sendEmptyMessage(STATUS_SCREENSHORT);
            }
        }).start();
    }

    private void saveScreenshotInWorkerThread() {
        SaveImageTask mSaveInBgTask = new SaveImageTask(mContext, mScreenBitmap);
        mSaveInBgTask.setListener(new TaskListener<Uri>() {
            @Override
            public void onComplete(Uri uri) {
                mSaveUri =uri;
                Log.e(TAG, "  mSaveUri= "+mSaveUri);
            }
        });
        mSaveInBgTask.execute();
    }

    private void saveScreenshotThread() {
        mScreenBitmap=mImageStitcher.getMergeImage();
        if(mScreenBitmap==null){
            stopScrollScreenshot();
        }else{
            SaveImageTask mSaveInBgTask = new SaveImageTask(mContext, mScreenBitmap);
            mSaveInBgTask.setListener(new TaskListener<Uri>() {
                @Override
                public void onComplete(Uri uri) {
                    mSaveUri =uri;
                    Log.e(TAG, "  mSaveUri= "+mSaveUri);
                    stopScrollScreenshot();
                }
            });
            mSaveInBgTask.execute();
        }
    }
    private void setWindowFocusable(boolean focusable) {
        if (focusable) {
            mWindowLayoutParams.flags &= ~WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        } else {
            mWindowLayoutParams.flags |= WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        }
        if (mScreenshotLayout.isAttachedToWindow()) {
            mWindowManager.updateViewLayout(mScreenshotLayout, mWindowLayoutParams);
        }
    }
    void dismissScreenshot(String reason, boolean immediate) {
        Log.v(TAG, "clearing screenshot: " + reason);
        mScreenshotHandler.removeMessages(MESSAGE_CORNER_TIMEOUT);
        mScreenshotLayout.getViewTreeObserver().removeOnComputeInternalInsetsListener(this);
        if (!immediate) {
            mScreenshotPreview.startDropOutAnima(PreviewImageView.ActionStatus.SAVE);
        } else {
            clearScreenshot();
        }
    }

    public void clearScreenshot() {
        if (mScreenshotLayout.isAttachedToWindow()) {
            mWindowManager.removeView(mScreenshotLayout);
        }
        mScreenshotLayout.setTranslationX(0);
        mScreenshotLayout.setTranslationY(0);
        mScreenshotPreview.setTranslationX(0);
        mScreenshotPreview.setTranslationY(0);
        mScreenshotPreview.setAlpha(0f);
        mScreenshotPreview.setLayerType(View.LAYER_TYPE_NONE, null);
        mScreenshotPreview.setBitmap(null);
        if( mScreenBitmap!=null){
            mScreenBitmap.recycle();
            mScreenBitmap=null;
        }
    }


    @Override
    public void dissActionsReady(boolean diss) {
        if(diss) {
            AccessibilityManager accessibilityManager = (AccessibilityManager)
                mContext.getSystemService(Context.ACCESSIBILITY_SERVICE);
            long timeoutMs = accessibilityManager.getRecommendedTimeoutMillis(
                    SCREENSHOT_CORNER_DEFAULT_TIMEOUT_MILLIS,
                    AccessibilityManager.FLAG_CONTENT_CONTROLS);
            mScreenshotHandler.removeMessages(MESSAGE_CORNER_TIMEOUT);
            mScreenshotHandler.sendMessageDelayed(
                    mScreenshotHandler.obtainMessage(MESSAGE_CORNER_TIMEOUT),
                    timeoutMs);
        }else{
            mScreenshotHandler.removeMessages(MESSAGE_CORNER_TIMEOUT);
        }
    }

    @Override
    public void onAction(ActionStatus action){
        Log.e(TAG, "  atcion= "+action);
        Intent intent=null;
        switch (action){
            case EDIT:
                intent=createEditAction(mSaveUri);
                break;
            case SHARE:
                intent=createShareAction(mSaveUri);
                break;
            case SCROLL:
                scrollAction();
                break;
            case DELETE:
                ContentResolver resolver = mContext.getContentResolver();
                resolver.delete(mSaveUri, null, null);
                break;
        }
        if(intent!=null) {
            mContext.startActivity(intent);
        }
        clearScreenshot();
    }

    private Intent createShareAction( Uri uri){
        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        sharingIntent.setType("image/png");
        sharingIntent.putExtra(Intent.EXTRA_STREAM, uri);
        ClipData clipdata = new ClipData(new ClipDescription("content",
                new String[]{ClipDescription.MIMETYPE_TEXT_PLAIN}),
                new ClipData.Item(uri));
        sharingIntent.setClipData(clipdata);
        sharingIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        sharingIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        return sharingIntent;
    }
    private Intent createEditAction( Uri uri){
        Intent editIntent = new Intent(Intent.ACTION_EDIT);
        editIntent.setType("image/png");
        editIntent.setData(uri);
        editIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        editIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        editIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        return editIntent;
    }
    private void createPriviewAction( Uri uri){
        Intent intent = new Intent(mContext, PreviewAcitvity.class);
       // intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.setData(uri);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        if(intent!=null) {
            mContext.startActivity(intent);
        }
    }


    private void scrollAction(){
        mScreenshotHandler.postDelayed(() -> {
            takeScreenshot(true);
        },500);

    }
    private void disposeInputChannel() {
        if (mInputEventReceiver != null) {
            mInputEventReceiver.dispose();
            mInputEventReceiver = null;
        }
        if (mInputMonitor != null) {
            mInputMonitor.dispose();
            mInputMonitor = null;
        }
    }
    class ScrollInputEventReceiver extends InputEventReceiver {
        ScrollInputEventReceiver(InputChannel channel, Looper looper) {
            super(channel, looper);
        }

        public void onInputEvent(InputEvent event) {
            try {
                if (event instanceof MotionEvent && (event.getSource() & InputDevice.SOURCE_CLASS_POINTER) != 0) {
                    MotionEvent e = MotionEvent.obtainNoHistory((MotionEvent) event);

                     //Log.i(TAG, " event.getAction()=  " + e.getAction() + "  getMetaState= " + e.getMetaState() + "  event.y= " + e.getY());

                    if (e.getAction() == MotionEvent.ACTION_MOVE && e.getMetaState() == META_SCROLL_LOCK_ON) {
                        isEventSync = true;
                    }
                    if ((e.getAction() == MotionEvent.ACTION_DOWN/*||e.getAction() == MotionEvent.ACTION_CANCEL*/) && e.getMetaState() == 0) {
                        if(!isInterceptTouch) {
                            isEventSync = true;
                            isScrolling = false;
                            isInterceptTouch = true;
                            mScreenshotHandler.sendEmptyMessage(STATUS_CHANGE_TITLE);
                        }
                    }
                }
            }finally {
                finishInputEvent(event, false);
            }

        }
    }

}
