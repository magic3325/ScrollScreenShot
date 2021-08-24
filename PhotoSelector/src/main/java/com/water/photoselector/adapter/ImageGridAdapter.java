package com.water.photoselector.adapter;

import android.content.Context;
import android.graphics.ColorFilter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.BlendModeColorFilterCompat;
import androidx.core.graphics.BlendModeCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.water.photoselector.R;
import com.water.photoselector.bean.LocalMedia;
import com.water.photoselector.listener.OnPhotoSelectListener;
import com.water.photoselector.load.ImageLoad;

import java.util.ArrayList;
import java.util.List;

public class ImageGridAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{


    Context mContext;
    private List<LocalMedia> mMediaList = new ArrayList<>();
    private List<LocalMedia> mSelectList = new ArrayList<>();

    private OnPhotoSelectListener<LocalMedia> mSelectListener;
    public void setOnSelectListener(OnPhotoSelectListener<LocalMedia> l) {
        mSelectListener = l;
    }
    public ImageGridAdapter(Context context) {
        super();
        mContext = context;

    }

    public void bindData(List<LocalMedia> data) {
        mMediaList.clear();
        mMediaList.addAll(data);
        //this.notifyDataSetChanged();
        notifyItemRangeChanged(0,getItemCount());
    }

    public List<LocalMedia> getMediaList() {
        return mMediaList == null ? new ArrayList<>() : mMediaList;
    }

    public boolean isMediaListEmpty() {
        return mMediaList == null || mMediaList.size() == 0;
    }
    public List<LocalMedia> getSelectedList() {
        return mSelectList == null ? new ArrayList<>() : mSelectList;
    }

    public int getSelectedSize() {
        return mSelectList == null ? 0 : mSelectList.size();
    }


    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.picture_image_grid_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ViewHolder contentHolder = (ViewHolder) holder;

        LocalMedia image = mMediaList.get( position);
        image.setPosition(position);
        String imagePath = image.getPath();
        ImageLoad.loadImage(holder.itemView.getContext(), imagePath, contentHolder.mImageView);
        contentHolder.mCheckbox.setVisibility(View.VISIBLE);
        selectImage(contentHolder, isSelected(image));
        if(image.getNum()>0&&isSelected(image)){
            contentHolder.mCheckbox.setText(String.valueOf(image.getNum()));
        }
        contentHolder.mCheckbox.setOnClickListener(v -> {
            changeCheckboxState(contentHolder, image);
        });
    }

    public boolean isSelected(LocalMedia image) {
        int size = mSelectList.size();
        for (int i = 0; i < size; i++) {
            LocalMedia media = mSelectList.get(i);
            if (media == null || TextUtils.isEmpty(media.getPath())) {
                continue;
            }
            if (TextUtils.equals(media.getPath(), image.getPath())
                    || media.getId() == image.getId()) {
                return true;
            }
        }
        return false;
    }
    private void changeCheckboxState(ViewHolder contentHolder, LocalMedia image) {
        boolean isChecked = contentHolder.mCheckbox.isSelected();
        int count = mSelectList.size();

        if (isChecked) {
            for (int i = 0; i < count; i++) {
                LocalMedia media = mSelectList.get(i);
                if (media == null || TextUtils.isEmpty(media.getPath())) {
                    continue;
                }
                if (media.getPath().equals(image.getPath())
                        || media.getId() == image.getId()) {
                    image.setChecked(false);
                    image.setNum(0);
                    mSelectList.remove(media);
                    subSelectPosition();
                    break;
                }
            }
        } else {
            mSelectList.add(image);
            image.setNum(mSelectList.size());
            image.setChecked(false);
        }
        selectImage(contentHolder, !isChecked);
        notifyItemChanged(contentHolder.getAdapterPosition());
        if (mSelectListener != null) {
            mSelectListener.onChange(mSelectList);
        }
    }
    /**
     * Update the selection order
     */
    private void subSelectPosition() {
            int size = mSelectList.size();
            for (int index = 0; index < size; index++) {
                LocalMedia media = mSelectList.get(index);
                media.setNum(index + 1);
                notifyItemChanged(media.position);
            }

    }

    @Override
    public int getItemCount() {
        return mMediaList.size();
    }

    public void selectImage(ViewHolder holder, boolean isChecked) {
        holder.mCheckbox.setSelected(isChecked);
        if(!isChecked){
            holder.mCheckbox.setText("");
        }
        ColorFilter colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(isChecked ?
                        ContextCompat.getColor(mContext, R.color.picture_color_80) :
                        ContextCompat.getColor(mContext, R.color.picture_color_20),
                BlendModeCompat.SRC_ATOP);
        holder.mImageView.setColorFilter(colorFilter);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView mImageView;
        TextView mCheckbox;
        View mContentView;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mContentView =itemView;
            mImageView = itemView.findViewById(R.id.image_view);
            mCheckbox = itemView.findViewById(R.id.image_checkbox);
        }
    }

}
