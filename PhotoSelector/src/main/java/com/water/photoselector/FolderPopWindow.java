package com.water.photoselector;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.water.photoselector.bean.LocalMediaFolder;
import com.water.photoselector.listener.OnAlbumItemClickListener;
import com.water.photoselector.load.ImageLoad;

import java.util.ArrayList;
import java.util.List;

public class FolderPopWindow extends FrameLayout implements View.OnClickListener {
    private static final int FOLDER_MAX_COUNT = 8;
    public final static int TYPE_ALL = 0;
    public final static int TYPE_IMAGE = 1;
    public final static int TYPE_VIDEO = 2;

    private final Context mContext;

    private View mCoverView;
    private ImageView mArrowView;
    private TextView mDirTitle;
    private RecyclerView mRecyclerView;
    private AlbumDirAdapter mAdapter;
    private boolean isShowing;
    private int mSelectPosition =-1;
    public FolderPopWindow(@NonNull Context context) {
        super(context);
        mContext = context;
    }

    public FolderPopWindow(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mRecyclerView = findViewById(R.id.folder_list);
        mCoverView = findViewById(R.id.cover_view);
        mAdapter = new AlbumDirAdapter();
        mRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(mContext, DividerItemDecoration.VERTICAL);
        dividerItemDecoration.setDrawable(mContext.getResources().getDrawable(R.drawable.folder_divider));
        mRecyclerView.addItemDecoration(dividerItemDecoration);
        mRecyclerView.setAdapter(mAdapter);
        mCoverView.setOnClickListener(this);
    }

    public boolean isEmpty() {
        return mAdapter.getFolderData().size() == 0;
    }


    public void setView(ImageView arrow,TextView title) {
        mArrowView = arrow;
        mDirTitle = title;
    }


    public void setFolderDate(List<LocalMediaFolder> folders) {
        mAdapter.bindFolderData(folders);
        if(folders!=null&&folders.size()>0){
            mDirTitle.setText(folders.get(0).getName());
        }
    }

    @Override
    public void onClick(View v) {
        if(v==mCoverView&&isShowing()){
            showDirPopWindow(false);
        }
    }
    public boolean isShowing(){
            return isShowing;
    }
    public void showDirPopWindow(boolean show){

        isShowing =show;
        float start =show?0f:1f;
        float end =show?1f:0f;
        AnimatorSet animatorSet = new AnimatorSet();
        ValueAnimator anima = ValueAnimator.ofFloat(start, end);
        anima.setDuration(250);
        anima.addUpdateListener(animation -> {
            float t = (float)animation.getAnimatedValue();
            int height=  mRecyclerView.getMeasuredHeight();
            mRecyclerView.setTranslationY((t-1f)*height);
            mCoverView.setAlpha(t);
            mArrowView.setRotation(isShowing?180*t:-180*t);

        });
        anima.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation, boolean isReverse) {
                setLayerType(View.LAYER_TYPE_HARDWARE, null);
                setVisibility(View.VISIBLE);
                int w = View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED);
                int h = View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED);
                mRecyclerView.measure(w, h);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                setLayerType(View.LAYER_TYPE_NONE, null);
                if(!show){
                    setVisibility(View.GONE);
                }
            }

            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                setVisibility(View.VISIBLE);
            }
        });

        animatorSet.play(anima);
        animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        animatorSet.start();
    }

    public class AlbumDirAdapter extends RecyclerView.Adapter<AlbumDirAdapter.ViewHolder> {
        private List<LocalMediaFolder> mFolderList = new ArrayList<>();

        public AlbumDirAdapter() {
            super();
        }

        public void bindFolderData(List<LocalMediaFolder> folders) {
            mFolderList.clear();
            mFolderList.addAll(folders);
            notifyItemRangeChanged(0,getItemCount());

        }
        public List<LocalMediaFolder> getFolderData() {
            return mFolderList;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.picture_album_folder_item, parent, false);
            return new ViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            LocalMediaFolder folder = mFolderList.get(position);
            String name = folder.getName();
            int imageNum = folder.getImageNum();
            String imagePath = folder.getFirstImagePath();
            boolean isChecked = folder.isChecked();
            if(mSelectPosition==-1&&position==0){
                mSelectPosition=0;
                isChecked=true;
            }
            holder.mCheckbox.setSelected(isChecked);
            ImageLoad.loadFolderImage(holder.itemView.getContext(),
                    imagePath, holder.mImage);
            Context context = holder.itemView.getContext();
            String firstTitle = name;
            holder.mFolderName.setText(context.getString(R.string.picture_camera_roll_num, firstTitle, imageNum));
            holder.itemView.setOnClickListener(view -> {
                if (onAlbumItemClickListener != null) {
                    if(mSelectPosition!=position){
                        mFolderList.get(mSelectPosition).setChecked(false);
                        notifyItemChanged(mSelectPosition);
                        mSelectPosition =position;
                        mFolderList.get(position).setChecked(true);
                        notifyItemChanged(position);
                    }
                    onAlbumItemClickListener.onItemClick(position, folder.isCameraFolder(), folder.getBucketId(), folder.getName());
                }
            });
        }

        @Override
        public int getItemCount() {
            return mFolderList.size();
        }

         class ViewHolder extends RecyclerView.ViewHolder {
            ImageView mImage;
            TextView mFolderName;
             ImageView mCheckbox;
            public ViewHolder(View itemView) {
                super(itemView);
                mImage = itemView.findViewById(R.id.first_image);
                mFolderName = itemView.findViewById(R.id.tv_folder_name);
                mCheckbox = itemView.findViewById(R.id.checkbox);
            }

        }

    }

    private OnAlbumItemClickListener onAlbumItemClickListener;

    public void setOnAlbumItemClickListener(OnAlbumItemClickListener listener) {
        this.onAlbumItemClickListener = listener;
    }


}
