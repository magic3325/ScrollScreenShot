package com.water.scrollscreenshot;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    private native static int getseam(String path,String tmp,String out);
    private native static int mergeImage(String path_f,String path_s);
    private native static boolean compareImage(String path_f,String path_s);
    private native static int stitcherImage(String[] paths);
    public native String stringFromJNI();
    private native static void initStitcher(int cut_height,int compare_height,String out_path);
    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Example of a call to a native method
        TextView tv = findViewById(R.id.sample_text);
        tv.setText(stringFromJNI());
    }
    public void start(View view){
        Intent screenshotIntent = new Intent(this,TakeScreenshotService.class);
        // screenshotIntent.setAction(SCROLL_SCREENSHORT_ACTION);
        startService(screenshotIntent);
        finish();
    }


    public void stop(View view){
        Intent screenshotIntent = new Intent(this,TakeScreenshotService.class);
        // screenshotIntent.setAction(SCROLL_SCREENSHORT_ACTION);
        stopService(screenshotIntent);
    }


}