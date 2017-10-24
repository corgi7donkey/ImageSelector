package com.sjb.bupt.imageselector.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.sjb.bupt.imageselector.R;
import com.sjb.bupt.imageselector.util.ImageLoader;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by sjb on 2017/10/24.
 */

public class ImageAdapter extends BaseAdapter {
    private Context mContext;
    private List<String> mDatas;
    private LayoutInflater mInflater;
    private String mDirPath;
    private static Set<String> mSelectedImg = new HashSet<>();

    public ImageAdapter(Context context, List<String> mDatas, String dirPath) {
        this.mContext = context;
        this.mDatas = mDatas;
        this.mDirPath = dirPath;
        mInflater = LayoutInflater.from(mContext);
    }

    @Override
    public int getCount() {
        return mDatas.size();
    }

    @Override
    public Object getItem(int position) {
        return mDatas.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
      final  ViewHolder holder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.item_gridview, parent, false);
            holder = new ViewHolder();
            holder.mImgView = (ImageView) convertView.findViewById(R.id.id_item_img);
            holder.mImageButton = (ImageButton) convertView.findViewById(R.id.id_item_select);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        //重置状态
        holder.mImgView.setImageResource(R.mipmap.ic_launcher);
        holder.mImageButton.setImageResource(R.mipmap.pic_unselected);
        holder.mImgView.setColorFilter(null);
        ImageLoader.getInstance(3, ImageLoader.Type.LIFO)
                .loadImage(mDirPath + "/" + mDatas.get(position), holder.mImgView);

        final String filePath=mDirPath + "/" + mDatas.get(position);
        holder.mImgView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (mSelectedImg.contains(filePath)) {
                    //已选中
                    mSelectedImg.remove(filePath);
                    holder.mImgView.setColorFilter(null);
                    holder.mImageButton.setImageResource(R.mipmap.pic_unselected);
                }else{
                    //未选中
                    mSelectedImg.add(filePath);
                    holder.mImgView.setColorFilter(Color.parseColor("#77000000"));
                    holder.mImageButton.setImageResource(R.mipmap.pic_selected);
                }
            }
        });
        if (mSelectedImg.contains(filePath)) {
            holder.mImgView.setColorFilter(Color.parseColor("#77000000"));
            holder.mImageButton.setImageResource(R.mipmap.pic_selected);
        }

        return convertView;
    }


    private class ViewHolder {
        ImageView mImgView;
        ImageButton mImageButton;
    }
}
