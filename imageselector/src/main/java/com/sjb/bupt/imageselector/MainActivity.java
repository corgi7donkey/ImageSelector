package com.sjb.bupt.imageselector;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.GridView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.sjb.bupt.imageselector.adapter.ImageAdapter;
import com.sjb.bupt.imageselector.bean.FolderBean;
import com.sjb.bupt.imageselector.view.ListImageDirPopupWindow;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private GridView mGridView;
    private List<String> mImags;
    private ImageAdapter mImageAdapter;

    private RelativeLayout mBottomLayout;
    private TextView mDirName, mDirCount;

    private File mCurrentDir;
    private int mMaxCount;

    private List<FolderBean> mFolderBeens = new ArrayList<>();
    private ProgressDialog mProgressDialog;

    private ListImageDirPopupWindow mPopupWindow;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    mProgressDialog.dismiss();
                    bindData2View();
                    initPopupWindow();
                    break;
            }
        }
    };

    //初始化popupwindow
    private void initPopupWindow() {
        mPopupWindow = new ListImageDirPopupWindow(this, mFolderBeens);
        mPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                lightOn();
            }
        });
        //监听popupwindow中listview的点击事件，然后更新grideview的数据。
        mPopupWindow.setOnDirSelectedlistener(new ListImageDirPopupWindow.OnDirSelectedListener() {
            @Override
            public void onSelected(FolderBean folderBean) {
                mCurrentDir = new File(folderBean.getDir());
                mImags= Arrays.asList(mCurrentDir.list(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        if (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png"))
                            return true;
                        return false;
                    }
                }));
                mImageAdapter = new ImageAdapter(MainActivity.this, mImags, mCurrentDir.getAbsolutePath());
                mGridView.setAdapter(mImageAdapter);
                mDirCount.setText(mImags.size() + "");
                mDirName.setText(folderBean.getName());
                mPopupWindow.dismiss();
            }
        });
    }
    //隐藏popupwindow时，将背景变亮。
    private void lightOn() {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.alpha = 1.0f;
        getWindow().setAttributes(lp);
    }

    //显示popupwindow时，将背景变暗
    private void lightOff() {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.alpha = 0.3f;
        getWindow().setAttributes(lp);
    }

    //绑定数据到view上。
    private void bindData2View() {
        if (mCurrentDir == null) {
            Toast.makeText(this, "未扫描到图片。", Toast.LENGTH_SHORT).show();
            return;
        }
        mImags = Arrays.asList(mCurrentDir.list());
        mImageAdapter = new ImageAdapter(this, mImags, mCurrentDir.getAbsolutePath());
        mGridView.setAdapter(mImageAdapter);

        mDirCount.setText(mMaxCount + "");
        mDirName.setText(mCurrentDir.getName());

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        initDatas();
        initEvents();
    }

    private void initViews() {
        mGridView = (GridView) findViewById(R.id.id_gridView);
        mBottomLayout = (RelativeLayout) findViewById(R.id.id_bottom_rl);
        mDirName = (TextView) findViewById(R.id.id_dir_name);
        mDirCount = (TextView) findViewById(R.id.id_dir_count);
    }

    private void initDatas() {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Toast.makeText(this, "当前存储卡不可以！", Toast.LENGTH_SHORT).show();
            return;
        }
        mProgressDialog = ProgressDialog.show(this, null, "努力加载中。。。");
        new Thread() {
            @Override
            public void run() {
                Uri mImgUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                ContentResolver cr = MainActivity.this.getContentResolver();
                Cursor cursor = cr.query(mImgUri, null,
                        MediaStore.Images.Media.MIME_TYPE + "=? or " + MediaStore.Images.Media.MIME_TYPE + "=?",
                        new String[]{"image/jpeg", "image/png"},
                        MediaStore.Images.Media.DATE_MODIFIED);

                Set<String> mDirPaths = new HashSet<String>();
                while (cursor.moveToNext()) {
                    String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                    File parentFile = new File(path).getParentFile();
                    if (parentFile == null)
                        continue;
                    String dirPath = parentFile.getAbsolutePath();

                    if (mDirPaths.contains(dirPath))
                        continue;
                    mDirPaths.add(dirPath);
                    FolderBean folderBean = new FolderBean();
                    folderBean.setDir(dirPath);
                    folderBean.setFirstImgPath(path);
                    if (parentFile.list() == null)
                        continue;
                    int picCount = parentFile.list(new FilenameFilter() {
                        @Override
                        public boolean accept(File dir, String name) {
                            if (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png"))
                                return true;
                            return false;
                        }
                    }).length;
                    folderBean.setCount(picCount);

                    mFolderBeens.add(folderBean);
                    if (picCount > mMaxCount) {
                        mMaxCount = picCount;
                        mCurrentDir = parentFile;
                    }
                }
                cursor.close();
                mHandler.sendEmptyMessage(0);//扫描图片完成，通知handler
            }
        }.start();
    }

    private void initEvents() {
        //显示popupwindow
        mBottomLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPopupWindow.setAnimationStyle(R.style.dir_popupwindow_anim);//设置显示动画
                mPopupWindow.showAsDropDown(mBottomLayout, 0, 0);
                lightOff();
            }
        });
    }


}

