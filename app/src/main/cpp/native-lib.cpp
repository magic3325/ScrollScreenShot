#include <jni.h>
#include <string>

#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/features2d/features2d.hpp>

#include "opencv2/imgcodecs.hpp"
#include "opencv2/highgui.hpp"
#include "opencv2/stitching.hpp"

#include <android/log.h>
#include <android/bitmap.h>


#include "opencv2/stitching/detail/autocalib.hpp"
#include "opencv2/stitching/detail/blenders.hpp"
#include "opencv2/stitching/detail/timelapsers.hpp"
#include "opencv2/stitching/detail/camera.hpp"
#include "opencv2/stitching/detail/exposure_compensate.hpp"
#include "opencv2/stitching/detail/matchers.hpp"
#include "opencv2/stitching/detail/motion_estimators.hpp"
#include "opencv2/stitching/detail/seam_finders.hpp"
#include "opencv2/stitching/detail/warpers.hpp"
#include "opencv2/stitching/warpers.hpp"



#define LOG_TAG    "ImageStitcher"

#define LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG, __VA_ARGS__)
#define LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG, __VA_ARGS__)
#define LOGW(...)  __android_log_print(ANDROID_LOG_WARN,LOG_TAG, __VA_ARGS__)
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG, __VA_ARGS__)
#define LOGF(...)  __android_log_print(ANDROID_LOG_FATAL,LOG_TAG, __VA_ARGS__)

using namespace std;
using namespace cv;
using namespace cv::detail;

string result_name = "/storage/emulated/0/Pictures/result.jpg";
string result_main = "/storage/emulated/0/Pictures/main.jpg";
string result_sub = "/storage/emulated/0/Pictures/sub.jpg";
int TEMPLATE_HEIGHT=150;
int CUT_HEIGHT=300;

static int gCut_height = 300;
static int gCompare_height =150;
static char gOut_path[128];
#ifdef __cplusplus
extern "C" {
#endif

 jstring stringFrom_JNI(JNIEnv* env,jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

static void  init_stitcher(JNIEnv* env,jclass type,jint c_height,jint t_height,jstring outpath) {

    gCut_height=c_height;
    gCompare_height=t_height;
    const char *out_path = (char *) env->GetStringUTFChars(outpath, NULL);
    memset(gOut_path, 0, sizeof(gOut_path));
    strcpy((char *)gOut_path,out_path);
    LOGI("gCut_height = %d" ,gCut_height);
    LOGI("gCompare_height=  %d" ,gCompare_height);
    LOGI("gOut_path=  %s" ,(char *)gOut_path);
 }


static jint  get_seam(JNIEnv* env,jclass type,jstring path1,jstring path2,jstring out) {
    const char *f_path = (char *) env->GetStringUTFChars(path1, NULL);
    const char *s_path = (char *) env->GetStringUTFChars(path2, NULL);
    const char *out_path = (char *) env->GetStringUTFChars(out, NULL);
    Mat image_f = cv::imread(f_path);
    Mat image_s = cv::imread(s_path);
    LOGI("source_path = %s" ,f_path);
    LOGI("template_path=  %s" ,s_path);
    if(image_f.empty()||image_s.empty()){
        return -1;
    }
    int w=image_s.cols;
    int h=image_s.rows;
    Mat image_template(image_s, Rect(0,0,image_s.cols,gCompare_height));

    int const rows =image_f.cols - image_template.cols + 1;
    int const cols =image_f.rows - image_template.rows + 1;
    Mat tmp(rows, cols, CV_32FC1);
    matchTemplate(image_f, image_template, tmp, cv::TM_CCOEFF_NORMED);

    double minVal = -1;
    double maxVal;
    Point minLoc;
    Point maxLoc;
    Point matchLoc;
    minMaxLoc(tmp, &minVal, &maxVal, &minLoc, &maxLoc, Mat());
    LOGI("minLoc y = %d" ,minLoc.x);
    LOGI("maxLoc y  =  %d" ,maxLoc.y);

    matchLoc = maxLoc;
    Mat image_ff(image_f, Rect(0,0,image_f.cols,matchLoc.y));
    Mat result;
    vconcat(image_ff,image_s,result);
    imwrite(out_path, result);
    return matchLoc.y;
}



static jint  merge_Image(JNIEnv* env,jclass type,jstring path1,jstring path2) {
    const char *f_path = (char *) env->GetStringUTFChars(path1, NULL);
    const char *s_path = (char *) env->GetStringUTFChars(path2, NULL);
    Mat image_f = cv::imread(f_path);
    Mat image_s = cv::imread(s_path);
    LOGI("source_path = %s" ,f_path);
    LOGI("template_path=  %s" ,s_path);

    if(image_f.empty()||image_s.empty()){
        return -1;
    }
    Mat image_main=image_f(Rect(0,0,image_f.cols,image_f.rows-gCut_height));
    Mat image_sub(image_s, Rect(0,gCut_height,image_s.cols,image_s.rows-gCut_height));
    Mat image_template(image_main, Rect(0,image_main.rows-gCompare_height,image_main.cols,gCompare_height));


    int const rows =image_sub.cols - image_template.cols + 1;
    int const cols =image_sub.rows - image_template.rows + 1;
    Mat tmp(rows, cols, CV_32FC1);
    matchTemplate(image_sub, image_template, tmp, cv::TM_CCOEFF_NORMED);
    double minVal = -1;
    double maxVal;
    Point minLoc;
    Point maxLoc;
    Point matchLoc;
    minMaxLoc(tmp, &minVal, &maxVal, &minLoc, &maxLoc, Mat());
    matchLoc = maxLoc;

    LOGI("minLoc y = %d" ,minLoc.x);
    LOGI("maxLoc y  =  %d" ,maxLoc.y);

    int cutposition=matchLoc.y+image_template.rows;
    Mat image_tmp(image_sub, Rect(0,cutposition,image_sub.cols,image_sub.rows-cutposition));
    //imwrite("image_f.png", image_tmp);
    Mat result;
    vconcat(image_main,image_tmp,result);
    imwrite(f_path, result);
    return result.rows;
}

static jboolean compare_Image(JNIEnv* env,jclass type,jstring path1,jstring path2) {
    jboolean res = JNI_FALSE;
    const double ISSAME = 1.0000;
    const char *f_path = (char *) env->GetStringUTFChars(path1, NULL);
    const char *s_path = (char *) env->GetStringUTFChars(path2, NULL);
    double similarityValue=0.000000f;
    vector<Mat> img_mains;
    vector<Mat> img_subs;
    Mat image_main = cv::imread(f_path);
    Mat image_sub = cv::imread(s_path);
    if(image_main.empty()||image_sub.empty()){
        LOGI("compare empty image" );
        return JNI_FALSE;
    }
    img_mains.push_back(image_main);
    img_subs.push_back(image_sub);
    Rect rect(0, 0, image_main.cols , image_main.rows/4);
    rect.y = image_main.rows /4;
    img_mains.push_back(image_main(rect).clone());
    img_subs.push_back(image_sub(rect).clone());

    rect.y = image_main.rows /2;
    img_mains.push_back(image_main(rect).clone());
    img_subs.push_back(image_sub(rect).clone());

    for(int i = 0 ; i < 3 ; i ++){
        Mat hsv_base, hsv_test1, hsv_test2;
        cvtColor( img_mains[i], img_mains[i], COLOR_BGR2HSV_FULL );
        cvtColor( img_subs[i], img_subs[i], COLOR_BGR2HSV_FULL );

        int h_bins = 50, s_bins = 60;
        int histSize[] = { h_bins, s_bins };

        // hue varies from 0 to 179, saturation from 0 to 255
        float h_ranges[] = { 0, 180 };
        float s_ranges[] = { 0, 256 };

        const float* ranges[] = { h_ranges, s_ranges };

        // Use the 0-th and 1-st channels
        int channels[] = { 0, 1 };
        //! [Using 50 bins for hue and 60 for saturation]

        //! [Calculate the histograms for the HSV images]
        Mat hist_f,hist_s;


        calcHist( &img_mains[i], 1, channels, Mat(), hist_f, 2, histSize, ranges, true, false );
        normalize( hist_f, hist_f, 0, 1, NORM_MINMAX, -1, Mat() );

        calcHist( &img_subs[i], 1, channels, Mat(), hist_s, 2, histSize, ranges, true, false );
        normalize( hist_s, hist_s, 0, 1, NORM_MINMAX, -1, Mat());
        //! [Calculate the histograms for the HSV images]
        // for( int compare_method = 0; compare_method < 4; compare_method++ )
        // {
        double value = compareHist(hist_f, hist_s,0);
        similarityValue = similarityValue+value;
        LOGI("value  =  %1.8f" ,value);

        //}

    }
    if((similarityValue/(double)3.000000f)==ISSAME){
        return JNI_TRUE;
    }
    return JNI_FALSE;
}

static jint  stitcher_Image(JNIEnv* env,jclass type,jobjectArray paths) {
    jsize size = env->GetArrayLength(paths);
    jstring path = (jstring)env->GetObjectArrayElement(paths,0);
    const char *f_path = (char *) env->GetStringUTFChars(path, NULL);
    Mat image_f;
    Mat image_s;
    Mat result;
    image_f = cv::imread(f_path);
    if(image_f.empty()){
        LOGI("empty image_f = %s" ,f_path);
        return -1;
    }
    image_f.copyTo(result);
    for(int i=1;i<size;i++){
        jstring obj = (jstring)env->GetObjectArrayElement(paths,i);
        const char *s_path = (char *) env->GetStringUTFChars(obj, NULL);
        image_s = cv::imread(s_path);
        if(image_s.empty()){
            LOGI("empty image_s=  %s " ,s_path);
            continue;
        }

        Mat image_main=result(Rect(0,0,result.cols,result.rows-gCut_height));
        Mat image_sub(image_s, Rect(0,gCut_height,image_s.cols,image_s.rows-gCut_height));
        Mat image_template(image_main, Rect(0,image_main.rows-gCompare_height,image_main.cols,gCompare_height));
        int const rows =image_sub.cols - image_template.cols + 1;
        int const cols =image_sub.rows - image_template.rows + 1;
        Mat tmp(rows, cols, CV_32FC1);
        matchTemplate(image_sub, image_template, tmp, cv::TM_CCOEFF_NORMED);
        double minVal = -1;
        double maxVal;
        Point minLoc;
        Point maxLoc;
        Point matchLoc;
        minMaxLoc(tmp, &minVal, &maxVal, &minLoc, &maxLoc, Mat());
        matchLoc = maxLoc;

        LOGI("minLoc y = %d" ,minLoc.x);
        LOGI("maxLoc y  =  %d" ,maxLoc.y);

        int cutposition=matchLoc.y+image_template.rows;
        Mat image_tmp(image_sub, Rect(0,cutposition,image_sub.cols,image_sub.rows-cutposition));
        Mat out;
        vconcat(image_main,image_tmp,result);
    }
    imwrite(gOut_path, result);
    return result.rows;
}


static const char *MainActivityPathName ="com/water/scrollscreenshot/MainActivity";
static const char *ImageStitcherPathName ="com/water/scrollscreenshot/MainActivity";

static JNINativeMethod kMethods[] = {
        { "getseam","(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)I", (void*)get_seam },
        { "compareImage","(Ljava/lang/String;Ljava/lang/String;)Z", (void*)compare_Image },
        {"mergeImage", "(Ljava/lang/String;Ljava/lang/String;)I", (void*) merge_Image },
        {"stitcherImage", "([Ljava/lang/String;)I", (void*) stitcher_Image },
        { "stringFromJNI",  "()Ljava/lang/String;", (void*)stringFrom_JNI },
        { "initStitcher",  "(IILjava/lang/String;)V", (void*)init_stitcher },
        };


static int registerNativeMethods(JNIEnv* env, const char* className,
                                 JNINativeMethod* gMethods, int numMethods) {
    jclass clazz;
    clazz = env->FindClass(className);
    if (clazz == NULL) {
        LOGE("Native registration unable to find class '%s'", className);
        return JNI_FALSE;
    }
    if (env->RegisterNatives(clazz, gMethods, numMethods) < 0) {
        LOGE("RegisterNatives failed for '%s'", className);
        return JNI_FALSE;
    }
    return JNI_TRUE;
}



jint JNI_OnLoad(JavaVM* vm, void* reserved __unused) {
    JNIEnv* env;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        LOGE("Error: GetEnv failed in JNI_OnLoad");
        return -1;
    }
    if (!registerNativeMethods(env, MainActivityPathName, kMethods,
                               sizeof(kMethods) / sizeof(kMethods[0]))) {
        LOGE("Error: could not register native methods for JPEGOutputStream");
        return -1;
    }
    if (!registerNativeMethods(env, ImageStitcherPathName, kMethods,
                               sizeof(kMethods) / sizeof(kMethods[0]))) {
        LOGE("Error: could not register native methods for JPEGOutputStream");
        return -1;
    }
    return JNI_VERSION_1_6;
}
#ifdef __cplusplus
}
#endif