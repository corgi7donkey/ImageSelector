package com.sjb.bupt.imageselector.view;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.support.annotation.NonNull;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.sjb.bupt.imageselector.R;
import com.sjb.bupt.imageselector.bean.FolderBean;
import com.sjb.bupt.imageselector.util.ImageLoader;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sjb on 2017/10/24.
 */

public class ListImageDirPopupWindow extends PopupWindow {
    private int mWidth;
    private int mHeight;
    private ListView mListView;
    private View mConvertView;
    private List<FolderBean> mDatas;

    private OnDirSelectedListener mListener;

    public ListImageDirPopupWindow(Context context, List<FolderBean> datas) {
        calWidthAndHeight(context);
        this.mDatas = datas;
        mConvertView = LayoutInflater.from(context).inflate(R.layout.popup_main, null);

        setContentView(mConvertView);
        setWidth(mWidth);
        setHeight(mHeight);

        setFocusable(true);
        setTouchable(true);
        setOutsideTouchable(true);
        setBackgroundDrawable(new BitmapDrawable());

        setTouchInterceptor(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
                    dismiss();
                    return true;
                }
                return false;
            }
        });

        initViews(context);
        initEvents();
    }

    private void initViews(Context context) {
        mListView = (ListView) mConvertView.findViewById(R.id.id_list_dir);
        mListView.setAdapter(new ListDirAdapter(context,mDatas));
    }

    public void setOnDirSelectedlistener(OnDirSelectedListener mListener) {
        this.mListener = mListener;
    }

    private void initEvents() {
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (mListener != null) {
                    mListener.onSelected(mDatas.get(position));
                }
            }
        });
    }

    private void calWidthAndHeight(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics=new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(outMetrics);

        mWidth=outMetrics.widthPixels;
        mHeight = (int) (outMetrics.heightPixels * 0.6);
    }
    private class ListDirAdapter extends BaseAdapter{
        private LayoutInflater mInflater;
        private List<FolderBean> mDatas=new ArrayList<>();
        public ListDirAdapter(@NonNull Context context, @NonNull List<FolderBean> datas) {
            mInflater = LayoutInflater.from(context);
            this.mDatas=datas;
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
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                holder=new ViewHolder();
                convertView = mInflater.inflate(R.layout.item_popup, parent, false);
                holder.mImageView = (ImageView) convertView.findViewById(R.id.id_dir_img);
                holder.mDirName = (TextView) convertView.findViewById(R.id.id_dir_item_name);
                holder.mDirCount = (TextView) convertView.findViewById(R.id.id_dir_item_count);
                convertView.setTag(holder);
            }else {
                holder = (ViewHolder) convertView.getTag();
            }
            FolderBean folderBean = (FolderBean) getItem(position);
            holder.mImageView.setImageResource(R.mipmap.ic_launcher);
            ImageLoader.getInstance().loadImage(folderBean.getFirstImgPath(),holder.mImageView);
            holder.mDirCount.setText(folderBean.getCount()+"");
            holder.mDirName.setText(folderBean.getName());
            return convertView;
        }

        private class ViewHolder{
            TextView mDirName,mDirCount;
            ImageView mImageView;
        }
    }

public  interface OnDirSelectedListener{
    void onSelected(FolderBean folderBean);
}

}
