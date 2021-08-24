package com.water.scrollscreenshot.view;

import android.animation.AnimatorSet;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import androidx.annotation.Nullable;
import com.water.scrollscreenshot.listener.ImageLoadListener;
import com.water.scrollscreenshot.listener.onSingleTapListener;
import com.water.scrollscreenshot.task.BitmapLoadTask;

public class LargeImageView extends FrameLayout implements ImageLoadListener {
    private static final String TAG = "ImageStitcher";
    private Bitmap mBitmap;
    private int mSWidth;
    private int mSHeight;
    private int mVWidth;
    private int mVHeight;
    private PointF sTranslate;
    private PointF sTouchCenter;
    private float sScale;


    private Paint mBitmapPaint;

    private GestureDetector mGestureDetector;
    private ScaleGestureDetector mScaleDetector;

    private static final float FLING_COASTING_DURATION_S = 0.05f;
    private static final float MAX_SCALE = 2.5f;
    private ProgressBar mProgressBar;
    private onSingleTapListener tapListener;
    public LargeImageView(Context context) {
        super(context);
    }

    public LargeImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        if (mBitmapPaint == null) {
            mBitmapPaint = new Paint();
            mBitmapPaint.setAntiAlias(true);
            mBitmapPaint.setFilterBitmap(true);
            mBitmapPaint.setDither(true);
        }
        setWillNotDraw(false);
        mGestureDetector=new GestureDetector(onGestureListener);
        mScaleDetector = new ScaleGestureDetector(context, mScaleListener);
        sTranslate=  new PointF(0, 0);
        sTouchCenter=  new PointF(0, 0);
        sScale=1.0f;

        mProgressBar =new ProgressBar(context);
        LayoutParams lp =new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
        lp.height=100;
        lp.width = 100;
        lp.gravity= Gravity.CENTER;
        addView(mProgressBar,lp);

    }

    public void setonSingleTapListener(onSingleTapListener l){
        tapListener=l;
    }
    public void setImage(Bitmap bp){
        mBitmap =bp;
    }


    public void setImage(Uri uri) {
        mProgressBar.setVisibility(View.VISIBLE);
        BitmapLoadTask task=new BitmapLoadTask(getContext(),uri);
        Log.e(TAG, "  mSaveUri= "+uri);
        task.setListener(this);
        task.execute();

    }

    @Override
    public void onImageLoaded(int w, int h, Bitmap b) {
        mProgressBar.setVisibility(View.GONE);
        mBitmap=b.copy(Bitmap.Config.ARGB_8888, true);
        mSWidth=w;
        mSHeight=h;
        if(mSWidth<mVWidth){
            sTranslate.x=(mVWidth-mSWidth)/2;
        }
        if(mSHeight<mVHeight){
            sTranslate.y=(mVHeight-mSHeight)/2;
        }
        invalidate();
        postInvalidate();
    }

    public void relaseView(){
        if(mBitmap!=null){
            mBitmap.recycle();
        }
    }

    private float getXPosition(){
        if(mVWidth>mSWidth){
            return (mVWidth-mSWidth*sScale)/2;
        }
        return mVWidth-mSWidth*sScale;
    }
    private float getYPosition(){
        if(mVHeight>mSHeight){
            return (mVHeight-mSHeight*sScale)/2;
        }
        return mVHeight-mSHeight*sScale;
    }
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mVWidth = MeasureSpec.getSize(widthMeasureSpec);
        mVHeight = MeasureSpec.getSize(heightMeasureSpec);

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // If image or view dimensions are not known yet, abort.
        if (mSWidth == 0 || mSHeight == 0 || getWidth() == 0 || getHeight() == 0||mBitmap == null) {
            return;
        }
        Matrix matrix = new Matrix();
        matrix.preTranslate(0,0);
        matrix.postScale(sScale,sScale);
        matrix.postTranslate(sTranslate.x,sTranslate.y);
        canvas.drawBitmap(mBitmap,matrix,mBitmapPaint);
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mGestureDetector.onTouchEvent(event);
        mScaleDetector.onTouchEvent(event);
        return true;
    }


    GestureDetector.SimpleOnGestureListener onGestureListener = new GestureDetector.SimpleOnGestureListener() {

        private AnimatorSet mAnimatorSet;

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            Log.e(TAG, "onSingleTapUp");
            if(tapListener!=null){
                tapListener.onSingleTap();
            }
            return super.onSingleTapUp(e);
        }

        @Override
        public void onLongPress(MotionEvent e) {
            Log.e(TAG, "onLongPress");
            super.onLongPress(e);
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
           sTranslate.x=Math.max(Math.min(0.0f,sTranslate.x-distanceX),getXPosition());
           sTranslate.y=Math.max(Math.min(0.0f,sTranslate.y-distanceY),getYPosition());

            invalidate();
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            final float translationY =sTranslate.y;
            final int factor = 4;
            final float velocity = Math.max(Math.abs(velocityX), Math.abs(velocityY));
            final float duration = (float) (FLING_COASTING_DURATION_S
                                * Math.pow(velocity, (1f / (factor - 1f))));
             float dy =translationY + duration / factor * velocityY;
            float endtranslationY = Math.max(dy,getYPosition());
            final ValueAnimator decelerationY = ValueAnimator.ofFloat(translationY,endtranslationY);
            decelerationY.addUpdateListener(
                    new ValueAnimator.AnimatorUpdateListener() {
                         @Override
                         public void onAnimationUpdate(ValueAnimator animation) {
                             float transY = (Float) decelerationY.getAnimatedValue();
                             sTranslate.y=Math.max(Math.min(0.0f,transY),getYPosition());;
                             invalidate();
                         }
                    });
            mAnimatorSet = new AnimatorSet();
            mAnimatorSet.play(decelerationY);
            mAnimatorSet.setDuration((int) (duration * 1000));
            mAnimatorSet.setInterpolator(new TimeInterpolator() {
                  @Override
                public float getInterpolation(float input) {
                     return (float) (1.0f - Math.pow((1.0f - input), factor));
                    }
         });
            mAnimatorSet.start();

            return super.onFling(e1, e2, velocityX, velocityY);
        }

        @Override
        public void onShowPress(MotionEvent e) {
            super.onShowPress(e);
        }

        @Override
        public boolean onDown(MotionEvent e) {
            Log.e(TAG, "onDown");

            return super.onDown(e);
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            Log.e(TAG, "onDoubleTap");
            float touchx=e.getX();
            float touchy=e.getY();
            float scale =1.0f;
            if(sScale<MAX_SCALE){
                scale=MAX_SCALE;
            }else{
                scale=1.0f;
            }

            sTouchCenter.x =Math.abs(sTranslate.x-e.getX())/sScale;
            sTouchCenter.y =Math.abs(sTranslate.y-e.getY())/sScale;
            final ValueAnimator scaleAnima = ValueAnimator.ofFloat(sScale,scale);
            scaleAnima.addUpdateListener(
                    new ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(ValueAnimator animation) {
                            sScale = (Float) scaleAnima.getAnimatedValue();
                            Log.e(TAG, "sScale = "+sScale);
                            float tx=  Math.max(Math.min(0.0f,touchx-sTouchCenter.x*sScale),getXPosition());
                            float ty= Math.max(Math.min(0.0f,touchy-sTouchCenter.y*sScale),getYPosition());
                            sTranslate.x=tx;
                            sTranslate.y=ty;
                            invalidate();
                        }
                    });
            mAnimatorSet = new AnimatorSet();
            mAnimatorSet.play(scaleAnima);
            mAnimatorSet.setDuration(200);
            mAnimatorSet.setInterpolator(new TimeInterpolator() {
                @Override
                public float getInterpolation(float input) {
                    return (float) (1.0f - Math.pow((1.0f - input), 4));
                }
            });
            mAnimatorSet.start();
            invalidate();
            return super.onDoubleTap(e);
        }

        @Override
        public boolean onDoubleTapEvent(MotionEvent e) {

            return super.onDoubleTapEvent(e);
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            return super.onSingleTapConfirmed(e);
        }
    };

    ScaleGestureDetector.OnScaleGestureListener mScaleListener = new ScaleGestureDetector.OnScaleGestureListener() {

        @Override
        public boolean onScale(ScaleGestureDetector detector) {

            float scaleFactor = detector.getScaleFactor();

            if (Float.isNaN(scaleFactor) || Float.isInfinite(scaleFactor)) {
                return false;
            }
            PointF touchCenter=new PointF();

            touchCenter.x =Math.abs(sTranslate.x-detector.getFocusX())/sScale;
            touchCenter.y =Math.abs(sTranslate.y-detector.getFocusY())/sScale;
            sScale *= detector.getScaleFactor();
            if(sScale<1.0f){
                sScale=1.0f;
            }else if(sScale>MAX_SCALE){
                sScale=MAX_SCALE;
            }
            float tx=  Math.max(Math.min(0.0f,detector.getFocusX()-touchCenter.x*sScale),getXPosition());
            float ty= Math.max(Math.min(0.0f,detector.getFocusY()-touchCenter.y*sScale),getYPosition());
            sTranslate.x=tx;
            sTranslate.y=ty;
            Log.e(TAG, "sScale ="+sScale+"  getFocusX ="+ detector.getFocusX()+"  sTouchCenter.x ="+ touchCenter.x+"  tx ="+ tx);
            invalidate();

            return true;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            Log.e(TAG, "sScale ="+detector.getFocusX());

            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {

        }
    };























}
