package com.water.scrollscreenshot.Utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import static org.opencv.core.CvType.CV_32FC1;
import java.util.LinkedList;

public class ImageStitcher {

    private int mTop;
    private int mBottom;
    private int mLeft;
    private int mRight;
    private int mCom_Width;
    private int mCom_Height;
    private int mLimitHeight;
    private int mBitmapHeight=0;
    private  Context mContext;
    private Mat mMainMat;
    private Mat mCutMainMat;
    private Mat mCutSubMat;
    private  float mDensity;
    private int mOrientation;
    private static final int SPLICE_H = 1;
    private static final int SPLICE_V = 2;

    String mDirpath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString()+"/";
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(mContext) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.e(ScreenUtils.TAG, "OpenCV loaded successfully");
                    Mat imageMat=new Mat();
                } break;
                default: {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public ImageStitcher(Context context){
        mContext=context;
        mDensity = context.getResources().getDisplayMetrics().density;
        mTop =dp(ScreenUtils.cut_height);
        mBottom =dp(ScreenUtils.cut_height);
        mCom_Width =dp(ScreenUtils.compare_width);
        mCom_Height =dp(ScreenUtils.compare_height);
        mLimitHeight = ScreenUtils.stitch_max_height;
        mOrientation=SPLICE_V;
        if (!OpenCVLoader.initDebug()) {
            Log.d(ScreenUtils.TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, mContext, mLoaderCallback);
        } else {
            Log.d(ScreenUtils.TAG, "OpenCV library found inside package. Using it!");
        }
    }

    public void setconfig(int left_top,int right_bottom,int com_size,int orientation){
       if(orientation==SPLICE_H){
           mLeft =left_top;
           mRight =right_bottom;
           mCom_Width =com_size;
       }else if(orientation==SPLICE_V){
           mTop =left_top;
           mBottom =right_bottom;
           mCom_Height =com_size;
       }
        mOrientation=orientation;
        mBitmapHeight=0;


    }


    private int dp(int dp) {
        return (int)(mDensity * dp);
    }

    private  int mergeImage(Bitmap bitmap){
        if(mMainMat==null||mMainMat.empty()){
            mMainMat=new Mat();
            Utils.bitmapToMat(bitmap,mMainMat);
            return mMainMat.rows();
        }
        Mat image_s =new Mat();
        Utils.bitmapToMat(bitmap,image_s);
        if(image_s.empty()){
            Log.e(ScreenUtils.TAG, "image_s.empty()");
            return mMainMat.rows();
        }
        Rect frect =new Rect(0,0,mMainMat.cols(),mMainMat.rows()-mBottom);
        Rect srect =new Rect(0,mTop,image_s.cols(),image_s.rows()-mTop);

        Mat image_main =new Mat(mMainMat,frect);
        Mat image_sub= new Mat(image_s,srect);
        Rect trect =new Rect(0,image_main.rows()-mCom_Height,image_main.cols(),mCom_Height);

        Mat image_template= new Mat(image_main,trect);
        int  rows =image_sub.cols() - image_template.cols() + 1;
        int  cols =image_sub.rows() - image_template.rows() + 1;
        Mat tmp =new Mat(rows, cols, CV_32FC1);

        Imgproc.matchTemplate(image_sub, image_template, tmp, Imgproc.TM_CCOEFF_NORMED);

        Point matchLoc;
        Core.MinMaxLocResult res = Core.minMaxLoc(tmp,new Mat());
        matchLoc=res.maxLoc;
        Log.e(ScreenUtils.TAG, "  maxVal ="+res.maxVal+"  maxLoc ="+res.maxLoc);
        if(res.maxVal>0.9) {
            int cutposition = (int) matchLoc.y + image_template.rows();
            trect = new Rect(0, cutposition, image_sub.cols(), image_sub.rows() - cutposition);
            Mat image_tmp = new Mat(image_sub, trect);

            LinkedList<Mat> mats = new LinkedList<>();
            mats.add(image_main);
            mats.add(image_tmp);
            Core.vconcat(mats, mMainMat);
            image_tmp.release();
        }else{
            LinkedList<Mat> mats=new LinkedList<>();
            mats.add(image_s);
            mats.add(mMainMat);
            Core.vconcat(mats,mMainMat);
        }
        Mat mat =new Mat();
        Imgproc.cvtColor(mMainMat,mat,Imgproc.COLOR_RGBA2BGRA);
       //Imgcodecs.imwrite(mDirpath+mMainMat.rows()+"result.png",mat);
        image_s.release();
        image_main.release();
        image_sub.release();
        tmp.release();
        mat.release();
        return mMainMat.rows();
    }

    private  int mergeImage(String path){
        if(mMainMat==null||mMainMat.empty()){
            mMainMat=Imgcodecs.imread(path,Imgcodecs.IMREAD_COLOR);
            if(mOrientation==SPLICE_H){
                mTop =0;
                mBottom =0;
                mCom_Height=mMainMat.rows();
            }else if(mOrientation==SPLICE_V){
                mLeft =0;
                mRight =0;
                mCom_Width =mMainMat.cols();
            }
            return mMainMat.rows();
        }
        Mat image_s=Imgcodecs.imread(path,Imgcodecs.IMREAD_COLOR);
        if(image_s.empty()){
            Log.e(ScreenUtils.TAG, "image_s.empty()");
            return mMainMat.rows();
        }

        getCutImage(mMainMat,image_s);
        Point matchLoc = new Point();

        double matchVal =findSeam(matchLoc);
        LinkedList<Mat> mats=new LinkedList<>();
        Mat image_tmp = null;
        if(matchVal>0.9){
            int x=(int)matchLoc.x+mCom_Width;
            int y=(int)matchLoc.y+mCom_Height;
            Rect rect;
            if(mOrientation==SPLICE_H){
                rect =new Rect(x,0,mCutSubMat.cols()-x,mCutSubMat.rows());
            }else{
                rect =new Rect(0,y,mCutSubMat.cols(),mCutSubMat.rows()-y);
            }
            image_tmp =new Mat(mCutSubMat,rect);
            mats.add(mCutMainMat);
            mats.add(image_tmp);
        }else{
            mats.add(mMainMat);
            mats.add(image_s);
        }

        if(mOrientation==SPLICE_H){
            Core.hconcat(mats,mMainMat);
        }else{
            Core.vconcat(mats,mMainMat);
        }
        image_s.release();
        mCutMainMat.release();
        mCutSubMat.release();
        if(image_tmp!=null){
            image_tmp.release();
        }

        return mOrientation==SPLICE_H ?mMainMat.cols():mMainMat.rows();
    }



    private void getCutImage(Mat mainMat,Mat subMat){
        if(mCutMainMat!=null){
            mCutMainMat.release();
        }
        if(mCutSubMat!=null){
            mCutSubMat.release();
        }
        Rect mrect =new Rect(0,0,mainMat.cols()-mRight,mainMat.rows()-mBottom);
        Rect srect =new Rect(mLeft,mTop,subMat.cols()-mLeft,subMat.rows()-mTop);
        mCutMainMat =new Mat(mainMat,mrect);
        mCutSubMat= new Mat(subMat,srect);
    }


    private double findSeam(Point point){
        Rect trect =new Rect(mCutMainMat.cols()-mCom_Width,mCutMainMat.rows()-mCom_Height,mCom_Width,mCom_Height);
        Mat mComMat= new Mat(mCutMainMat,trect);
        int  rows =mCutSubMat.cols() - mComMat.cols() + 1;
        int  cols =mCutSubMat.rows() - mComMat.rows() + 1;
        Mat tmp =new Mat(rows, cols, CV_32FC1);
        Imgproc.matchTemplate(mCutSubMat, mComMat, tmp, Imgproc.TM_CCOEFF_NORMED);
        Core.MinMaxLocResult res = Core.minMaxLoc(tmp,new Mat());
        point.x=res.maxLoc.x;
        point.y=res.maxLoc.y;
        Log.e(ScreenUtils.TAG, "  maxVal ="+res.maxVal+"  maxLoc ="+res.maxLoc);
        return res.maxVal;
    }




    public boolean stitcherImage(Bitmap bitmap){
        long s = System.currentTimeMillis();
        int height =  mergeImage(bitmap);
        Log.e(ScreenUtils.TAG,"mergeImage time = "+ (System.currentTimeMillis()-s));
        Log.e(ScreenUtils.TAG,"height = "+mBitmapHeight);
        Log.e(ScreenUtils.TAG,"height = "+height);
        if((mBitmapHeight<height)&&(height<mLimitHeight)){
            mBitmapHeight=height;
        }else{
            return false;
        }
        return true;
    }
    public Bitmap getMergeImage(){
        Bitmap bmp=null;
        if(mMainMat!=null&&!mMainMat.empty()){
            bmp = Bitmap.createBitmap(mMainMat.cols(), mMainMat.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(mMainMat, bmp);
            mMainMat.release();
        }
        return bmp;

    }

    public boolean stitcherImage(String[] fileList){
        long s = System.currentTimeMillis();
        for(int i=0;i<fileList.length;i++){
            int height =  mergeImage(fileList[i]);
            Log.e(ScreenUtils.TAG,"height = "+mBitmapHeight);
            Log.e(ScreenUtils.TAG,"height = "+height);
            if((mBitmapHeight<height)&&(height<mLimitHeight)){
                mBitmapHeight=height;
            }else{
              return false;
            }
        }
        Log.e(ScreenUtils.TAG,"mergeImage time = "+ (System.currentTimeMillis()-s));

        return true;
    }
    public Bitmap getImage(){
        Bitmap bmp=null;
        if(mMainMat!=null&&!mMainMat.empty()){
            Mat mat =new Mat();
            Imgproc.cvtColor(mMainMat,mat,Imgproc.COLOR_RGBA2BGRA);
            bmp = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(mat, bmp);
            mMainMat.release();
        }
        return bmp;

    }



}
