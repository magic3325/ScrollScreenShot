package com.water.photoselector.load;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.text.TextUtils;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;

import com.squareup.picasso.Picasso;
import com.water.photoselector.R;
import java.io.File;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
public class ImageLoad {
    public static boolean isContent(String url) {
        if (TextUtils.isEmpty(url)) {
            return false;
        }
        return url.startsWith("content://");
    }
    public static boolean isHasHttp(String path) {
        if (TextUtils.isEmpty(path)) {
            return false;
        }
        return path.startsWith("http") || path.startsWith("https")
                || path.startsWith("/http") || path.startsWith("/https");
    }
//    public static  void loadImage(@NonNull Context context, @NonNull String url, @NonNull ImageView imageView) {
//        if (isContent(url) ||isHasHttp(url)) {
//            Picasso.get().load(Uri.parse(url)).into(imageView);
//        } else {
//              Picasso.get().load(new File(url)).into(imageView);
//         }
//      }

    public static void loadFolderImage(@NonNull Context context, @NonNull String url, @NonNull ImageView imageView) {

        Glide.with(context)
                .asBitmap()
                .load(url)
                //.override(180, 180)
                .centerCrop()
                .sizeMultiplier(1f)
                .placeholder(R.drawable.picture_icon_placeholder)
                .into(new BitmapImageViewTarget(imageView) {
                    @Override
                    protected void setResource(Bitmap resource) {
                        RoundedBitmapDrawable circularBitmapDrawable =
                                RoundedBitmapDrawableFactory.
                                        create(context.getResources(), resource);
                        circularBitmapDrawable.setCornerRadius(4);
                        imageView.setImageDrawable(circularBitmapDrawable);
                    }
                });
    }
    public static void loadImage(@NonNull Context context, @NonNull String url, @NonNull ImageView imageView) {

        Glide.with(context)
                .asBitmap()
                .load(url)
                //.override(180, 180)
                .centerCrop()
                .sizeMultiplier(1f)
                .placeholder(R.drawable.picture_icon_placeholder)
                .into(new BitmapImageViewTarget(imageView) {
                    @Override
                    protected void setResource(Bitmap resource) {
                        RoundedBitmapDrawable circularBitmapDrawable =
                                RoundedBitmapDrawableFactory.
                                        create(context.getResources(), resource);
                        //  circularBitmapDrawable.setCornerRadius(8);
                        imageView.setImageDrawable(circularBitmapDrawable);
                    }
                });
    }







}
