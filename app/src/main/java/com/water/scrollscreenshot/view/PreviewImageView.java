package com.water.scrollscreenshot.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewOutlineProvider;
import android.view.WindowManager;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.math.MathUtils;
import com.water.scrollscreenshot.R;
import com.water.scrollscreenshot.Utils.ScreenUtils;

public class PreviewImageView extends FrameLayout implements View.OnClickListener{



    private static final Interpolator RUBBER_BAND_INTERPOLATOR = new PathInterpolator(1.0f , 1.0f, 1.0f, 1.0f);
    private float mInitialTouchX;
    private float mInitialTouchY;

    private float mMaxTranslation;

    private int mScreenWidth;
    private int mScreenHeight;
    private  float mDensity;
    private Context mContext;
    private int mWidth;
    private int mHeight;
    private int mPreWidth;
    private int mPreHeight;
    private float mScale;
    private float mRound;
    private ImageView mImage;
    private TextView mTopText;
    private TextView mBottomText;
    private int mTextHeight;
    private int mTextWidth;
    private int mPadding;

    private float mTouchTranslation;
    private float mProgress;
    private  int mTouchSlop;
    private boolean IsDragH;
    private boolean IsDragV;
    public  enum DragStatus {
        DRAG_UNKNOWN,
        DRAG_DOWN,
        DRAG_UP,
        DRAG_LEFT,
        DRAG_RIGHT,
    }
    public enum ActionStatus {
        UNKNOWN,
        SHARE,
        SCROLL,
        EDIT,
        SAVE,
        DELETE,
    }
    private static final long SCREENSHOT_TO_CORNER_Y_DURATION_MS = 500;

    private static final long SCREENSHOT_DISMISS_Y_DURATION_MS = 350;
    private static final long SCREENSHOT_DISMISS_DURATION_MS = 250;
    private static final long SCREENSHOT_DISMISS_OFFSET_MS = 50;
    private static final long SCREENSHOT_RESET_DURATION_MS = 250;


    private DragStatus mDragStatus=DragStatus.DRAG_UNKNOWN;

    public interface  onActionListener{
        void dissActionsReady(boolean diss);
        void onAction(ActionStatus action);
    }

    private onActionListener mListener;

    public void setOnActionListener(onActionListener l){
        mListener=l;
    }
    public PreviewImageView(@NonNull Context context) {
        super(context);
    }
    public PreviewImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    private void initView(Context context){
        mContext=context;
        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        Display mDisplay = wm.getDefaultDisplay();
        DisplayMetrics mDisplayMetrics = new DisplayMetrics();
        mDisplay.getRealMetrics(mDisplayMetrics);
        mPadding=getResources().getDimensionPixelSize(R.dimen.preview_padding);
        mWidth = getResources().getDimensionPixelSize(R.dimen.preview_width);
        mPreWidth=mWidth-mPadding*2;
        mScale=(float) (mPreWidth)/(float)mDisplayMetrics.widthPixels;
        mPreHeight=(int) (mDisplayMetrics.heightPixels*mScale);
        mHeight= mPreHeight+mPadding*2;
        mDensity = context.getResources().getDisplayMetrics().density;
        mScreenWidth=mDisplayMetrics.widthPixels;
        mScreenHeight=mDisplayMetrics.heightPixels;
        mMaxTranslation=240;

        setWillNotDraw(false);

        mTextHeight=(int)dp(80f);
        mTextWidth =mPreWidth;
        mRound=dp(ScreenUtils.frame_round);
        setPadding(mPadding,mPadding,mPadding,mPadding);
        mImage=new ImageView(context);
        LayoutParams lp = new LayoutParams(mPreWidth, mPreHeight);
        lp.gravity= Gravity.TOP;
        mImage.setClipToOutline(true);
        mImage.setOutlineProvider(mProvider);
        mImage.setScaleType(ImageView.ScaleType.FIT_START);
        addView(mImage,lp);
        addTopTextView();
        addBottomTextView();
        setOnClickListener(this);
        final ViewConfiguration configuration = ViewConfiguration.get(mContext);
        mTouchSlop = configuration.getScaledTouchSlop();
    }
    private float dp(float dp) {
        return mDensity * dp;
    }
    private final ViewOutlineProvider mProvider = new ViewOutlineProvider() {

        @Override
        public void getOutline(View view, Outline outline) {
            outline.setRoundRect(new Rect(0, 0, view.getWidth(), view.getHeight()),
                    mRound);
        }
    };
    private void addTopTextView() {
        mTopText = new TextView(mContext);
        mTopText.setPadding(0, mTextHeight/4, 0, 0);
        mTopText.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL);
        mTopText.setTextSize(18f);
        mTopText.setSingleLine(true);
        mTopText.setClickable(false);
        mTopText.setClipToOutline(false);
        mTopText.setText("share");
        mTopText.setAlpha(0f);
        mTopText.setClipToOutline(true);
        mTopText.setOutlineProvider(mProvider);
        LayoutParams top = new LayoutParams(LayoutParams.MATCH_PARENT, mTextHeight);
        top.gravity = Gravity.CENTER_HORIZONTAL|Gravity.TOP;
        addView(mTopText,top);

    }
    private void addBottomTextView() {
        mBottomText = new TextView(mContext);
        mBottomText.setPadding(0, 0, 0, mTextHeight/4);
        mBottomText.setGravity(Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);
        mBottomText.setTextSize(18f);
        mBottomText.setSingleLine(true);
        mBottomText.setClickable(false);
        mBottomText.setBackground(null);
        mBottomText.setText("scrolling");
        mBottomText.setAlpha(0f);
        mBottomText.setClipToOutline(true);
        mBottomText.setOutlineProvider(mProvider);
        LayoutParams bottom = new LayoutParams(LayoutParams.MATCH_PARENT, mTextHeight);
        bottom.gravity = Gravity.CENTER_HORIZONTAL|Gravity.BOTTOM;
        addView(mBottomText,bottom);

    }
    private Bitmap getFrameBitmap(){
        float size= dp(15f);
        Bitmap output = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888,true);
        Canvas canvas = new Canvas(output);
        RectF shadowRect = new RectF(
                size,
                size,
                output.getWidth() - size,
                output.getHeight() - size);
        Paint shadowPaint = new Paint();
        shadowPaint.setAntiAlias(true);
        shadowPaint.setColor(Color.WHITE);
        shadowPaint.setStyle(Paint.Style.FILL);
        shadowPaint.setShadowLayer(mRound, 0, 0, Color.GRAY);
        canvas.drawRoundRect(shadowRect, size, size, shadowPaint);

        RectF rect = new RectF(
                mPadding,
                mPadding,
                output.getWidth() - mPadding,
                output.getHeight() - mPadding);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(Color.TRANSPARENT);
        paint.setStyle(Paint.Style.FILL);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));

        canvas.drawRoundRect(rect, mRound, mRound, paint);
        paint.setColor(Color.GRAY);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth((float) 5.0);
        canvas.drawRoundRect(rect, mRound, mRound, paint);

        return output;
    }
    public void setBitmap(Bitmap bitmap){
        if(bitmap==null){
            return;
        }
        mImage.setImageBitmap(bitmap);
        invalidate();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        setBackground(new BitmapDrawable(getFrameBitmap()));
    }

    @Override
    public void onClick(View v) {
        ActionStatus action= ActionStatus.EDIT;
        startDropOutAnima(action);
    }





    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if(mListener!=null){
                    mListener.dissActionsReady(false);
                }
                mInitialTouchX = event.getRawX();
                mInitialTouchY = event.getRawY();
                IsDragH=false;
                IsDragV=false;
                mDragStatus=DragStatus.DRAG_UNKNOWN;
                break;
            case MotionEvent.ACTION_MOVE:

                final float xDiff = Math.abs(event.getRawX() - mInitialTouchX);
                final float yDiff = Math.abs(event.getRawY() - mInitialTouchY);
                if(!IsDragH&&!IsDragV){
                    if(yDiff > mTouchSlop&&yDiff >xDiff){
                        IsDragV=true;
                    }else if( xDiff > mTouchSlop&&xDiff >yDiff){
                        IsDragH=true;
                    }
                }
                if (IsDragV) {

                    float progress =  Math.min(1.0f,yDiff / (mScreenHeight));
                    float pp = RUBBER_BAND_INTERPOLATOR.getInterpolation(progress);
                    if (event.getRawY() - mInitialTouchY > 0) {
                        mDragStatus=DragStatus.DRAG_UP;
                        mTouchTranslation = pp * mMaxTranslation;
                    } else {
                        mDragStatus=DragStatus.DRAG_DOWN;
                        mTouchTranslation = -(pp * mMaxTranslation);
                    }
                    setTranslationY(mTouchTranslation);
                    mProgress = Math.min((yDiff / (getHeight() / 2)), 1.0f);
                    updateTextView(mProgress);
                    return true;
                }else if(IsDragH){
                    IsDragH=true;
                    float progress = Math.min(1.0f,xDiff / (mScreenWidth));
                    float pp = RUBBER_BAND_INTERPOLATOR.getInterpolation(progress);
                    if (event.getRawX() - mInitialTouchX > 0) {
                        mTouchTranslation = pp * mMaxTranslation;
                        mDragStatus=DragStatus.DRAG_RIGHT;
                    } else {
                        mTouchTranslation = -(pp * mMaxTranslation);
                        mDragStatus=DragStatus.DRAG_LEFT;
                    }
                    setTranslationX(mTouchTranslation);
                    return true;
                }

                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if(IsDragH||IsDragV){
                    IsDragH=false;
                    IsDragV=false;
                    ActionStatus action= ActionStatus.UNKNOWN;
                    if(mProgress==1f&&mDragStatus==DragStatus.DRAG_DOWN){
                        action=ActionStatus.SHARE;

                    }else if(mProgress==1f&&mDragStatus==DragStatus.DRAG_UP){
                        action=ActionStatus.SCROLL;
                    }   else if(mDragStatus==DragStatus.DRAG_LEFT){
                        action=ActionStatus.SAVE;
                    }   else if(mDragStatus==DragStatus.DRAG_RIGHT){
                        action=ActionStatus.DELETE;
                    }
                    startDropOutAnima(action);
                    return true;
                }

                break;
        }
        return super.onTouchEvent(event);
    }

    private Drawable gettextBitmap(float progress, boolean isTop){

        float  mDeltaH=(float)mTextHeight/3f;
        Paint mBackPaint = new Paint();
        mBackPaint.setAntiAlias(true);
        mBackPaint.setStyle(Paint.Style.FILL);
        if(progress>=1f){
            mBackPaint.setColor(Color.BLUE);
        }else{
            mBackPaint.setColor(0xffCCCCCC);
        }
        mBackPaint.setAlpha((int) (progress * 255f));
        Path mPath = new Path();
        int top=0;
        int bottom=0;

        if(isTop){
            top=0;
            bottom = (int) (mTextHeight/2+(progress * mDeltaH));
            mPath.moveTo(0, bottom);
            mPath.quadTo(mTextWidth / 2f, bottom+(progress - 0.5f) * mDeltaH, mTextWidth, bottom);
        }else{
            top=mTextHeight-(int) (mTextHeight/2+(progress * mDeltaH));
            bottom = mTextHeight;
            mPath.moveTo(0, top);
            mPath.quadTo(mTextWidth / 2f, top-(progress - 0.5f) * mDeltaH, mTextWidth, top);
        }

        Bitmap output = Bitmap.createBitmap(mTextWidth, mTextHeight, Bitmap.Config.ARGB_8888,true);
        Canvas canvas = new Canvas(output);
        canvas.drawRect(0, top, mTextWidth, bottom, mBackPaint);
        if(progress>=0.5f) {
            canvas.drawPath(mPath, mBackPaint);
        }
        return new BitmapDrawable(output);
    }


    private void updateTextView(float progress){
        if(progress==0){
            mTopText.setAlpha(0f);
            mBottomText.setAlpha(0f);
        }else {
            if (mDragStatus==DragStatus.DRAG_DOWN) {
                mTopText.setAlpha(1f);
                mBottomText.setAlpha(0f);
                mTopText.setBackground(gettextBitmap(progress,true));
            } else if(mDragStatus==DragStatus.DRAG_UP) {
                mBottomText.setAlpha(1f);
                mTopText.setAlpha(0f);
                mBottomText.setBackground(gettextBitmap(progress,false));
            }
            if(progress>=1f){
                mTopText.setTextColor(Color.WHITE);
                mBottomText.setTextColor(Color.WHITE);
            }else{
                mTopText.setTextColor(Color.BLUE);
                mBottomText.setTextColor(Color.BLUE);
            }
            invalidate();
        }
    }

    public void resetView() {
        mTouchTranslation=0;
        mProgress=0f;
        setTranslationY(0);
        setTranslationX(0);
        setScaleX(1f);
        setScaleY(1f);
        mTopText.setAlpha(0f);
        mTopText.setBackground(null);
        mBottomText.setAlpha(0f);
        mBottomText.setBackground(null);
        setLayerType(View.LAYER_TYPE_NONE, null);
    }


    private AnimatorSet createResetAnimation(){
        ValueAnimator yAnim = ValueAnimator.ofFloat(1f, 0);
        yAnim.setDuration(SCREENSHOT_RESET_DURATION_MS);
        yAnim.addUpdateListener(animation -> {
            float yDelta = MathUtils.lerp(mTouchTranslation, 0, animation.getAnimatedFraction());
            float alpha = MathUtils.lerp(mProgress, 0.5f, animation.getAnimatedFraction());
            setTranslationY(yDelta);
            updateTextView(alpha);
            if(mTopText.getAlpha()!=0){
                mTopText.setAlpha(1f-animation.getAnimatedFraction());
            }
            if(mBottomText.getAlpha()!=0){
                mBottomText.setAlpha(1f-animation.getAnimatedFraction());
            }
        });
        yAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                resetView();

            }
        });

        AnimatorSet animSet = new AnimatorSet();
        animSet.play(yAnim);
        return animSet;
    }

    private AnimatorSet createDropInAnimation() {
        Rect previewBounds = new Rect();
        //getBoundsOnScreen(previewBounds);
        getGlobalVisibleRect(previewBounds);
        Rect bounds =new Rect(0,0,mScreenWidth,mScreenHeight);
        float mScale =(float) bounds.width()/(float)mPreWidth;
        AnimatorSet dropInAnimation = new AnimatorSet();
        final PointF startPos = new PointF(bounds.centerX(), bounds.centerY());
        final PointF finalPos = new PointF(previewBounds.centerX(), previewBounds.centerY());
        ValueAnimator toCorner = ValueAnimator.ofFloat(0, 1);
        toCorner.setDuration(SCREENSHOT_TO_CORNER_Y_DURATION_MS);
        toCorner.addUpdateListener(animation -> {
            float t = animation.getAnimatedFraction();
            float scale =MathUtils.lerp(mScale,1.0f ,t);
            float xCenter = MathUtils.lerp((startPos.x-finalPos.x),0 , t);
            float yCenter = MathUtils.lerp((startPos.y- finalPos.y),0, t);
            setScaleX(scale);
            setScaleY(scale);
            setTranslationX(xCenter);
            setTranslationY(yCenter);
        });

        toCorner.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                setAlpha(1f);
            }
            @Override
            public void onAnimationEnd(Animator animation, boolean isReverse) {
                if(mListener!=null){
                    mListener.dissActionsReady(true);
                }
            }
        });

        dropInAnimation.play(toCorner);
        return dropInAnimation;
    }

    private AnimatorSet createDismissAnimation() {
        ValueAnimator alphaAnim = ValueAnimator.ofFloat(0, 1);
        alphaAnim.setDuration(SCREENSHOT_DISMISS_DURATION_MS);
        alphaAnim.addUpdateListener(animation -> {
            float scale = MathUtils.lerp(1f, 0.75f, animation.getAnimatedFraction());
            setAlpha(1 - animation.getAnimatedFraction());
            setScaleX(scale);
            setScaleY(scale);
        });

        AnimatorSet animSet = new AnimatorSet();
        animSet.play(alphaAnim);
        return animSet;
    }

    private AnimatorSet createSaveAnimation() {
        ValueAnimator anim = ValueAnimator.ofFloat(0, 1);
        anim.setDuration(SCREENSHOT_DISMISS_DURATION_MS);
        anim.addUpdateListener(animation -> {
            float alpha =MathUtils.lerp(1.0f,0f ,animation.getAnimatedFraction());
            float xDelta = MathUtils.lerp(mTouchTranslation, -mScreenWidth, animation.getAnimatedFraction());
            setTranslationX(xDelta);
            setAlpha(alpha);
        });

        AnimatorSet animSet = new AnimatorSet();
        animSet.play(anim);
        return animSet;
    }


    private AnimatorSet createDeleteAnimation() {
        ValueAnimator anim = ValueAnimator.ofFloat(0, 1);
        anim.setStartDelay(SCREENSHOT_DISMISS_OFFSET_MS);
        anim.setDuration(SCREENSHOT_DISMISS_DURATION_MS);
        anim.addUpdateListener(animation -> {
            float alpha =MathUtils.lerp(1.0f,0f ,animation.getAnimatedFraction());
            float xDelta = MathUtils.lerp(mTouchTranslation, mScreenWidth, animation.getAnimatedFraction());
            setTranslationX(xDelta);
            setAlpha(alpha);
        });

        AnimatorSet animSet = new AnimatorSet();
        animSet.play(anim);
        return animSet;
    }

    public void startDropInAnima(){
        Animator anima=createDropInAnimation();
        setLayerType(View.LAYER_TYPE_HARDWARE, null);
        buildLayer();
        anima.start();
    }

    public void startDropOutAnima(ActionStatus action){
        Animator anima;
        Log.e(ScreenUtils.TAG, "  atcionatcion = "+action);
        switch (action){
            case SHARE:
            case SCROLL:
                anima =createDismissAnimation();
                break;
            case EDIT:
                anima =createSaveAnimation();
                break;
            case SAVE:
                anima =createSaveAnimation();
                break;
            case DELETE:
                anima =createDeleteAnimation();
                break;
            default:
                anima =createResetAnimation();
                break;
        }
        anima.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                resetView();
                if(action!=ActionStatus.UNKNOWN){
                    if(mListener!=null){
                        mListener.onAction(action);
                    }
                }else{
                    if(mListener!=null){
                        mListener.dissActionsReady(true);
                    }
                }
            }
        });
        setLayerType(View.LAYER_TYPE_HARDWARE, null);
        buildLayer();
        anima.start();
    }











}
