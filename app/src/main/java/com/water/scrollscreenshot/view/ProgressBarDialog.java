package com.water.scrollscreenshot.view;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.water.scrollscreenshot.R;

public class ProgressBarDialog extends AlertDialog {

    private ProgressBar mProgress;
    private TextView mTitle;
    private View mContextView;

    protected ProgressBarDialog(Context context) {
        super(context);
        LayoutInflater inflater = LayoutInflater.from(getContext());
        mContextView =inflater.inflate(R.layout.progressbar_dialog,null);
        mTitle= mContextView.findViewById(R.id.title);
        mProgress= mContextView.findViewById(R.id.bar);
    }

    protected ProgressBarDialog(Context context, boolean cancelable, OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(mContextView);


    }
    public static ProgressBarDialog Create(Context context, CharSequence title, CharSequence message) {
        ProgressBarDialog dialog = new ProgressBarDialog(context);
        //dialog.setTitle(title);
        dialog.setMessage(message);
        dialog.setCancelable(false);
        return dialog;
    }


    public void setTitle(String title) {
        if(mTitle!=null){
            mTitle.setText(title);
        }

    }
}
