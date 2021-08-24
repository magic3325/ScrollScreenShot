package com.water.photoselector;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent =getIntent();
        if(intent!=null&&intent.getClipData()!=null){
            Log.e("water","intent.getClipData()= "+intent.getClipData().getItemCount())  ;
        }else{
            Log.e("water","intent.getClipData()= nullll")  ;
        }


    }
}