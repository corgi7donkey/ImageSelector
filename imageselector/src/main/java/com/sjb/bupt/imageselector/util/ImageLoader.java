package com.sjb.bupt.imageselector.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.LruCache;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

import static android.graphics.BitmapFactory.decodeFile;

/**
 * Created by sjb on 2017/10/24.
 */

public class ImageLoader {
    private static ImageLoader mInstance;

    private LruCache<String, Bitmap> mLruCache;
    private ExecutorService mThreadPool;
    private static final int DEFALT_THREAD_COUNT = 1;
    private Type mType = Type.LIFO;
    private LinkedList<Runnable> mTaskQueue;
    private Thread mPoolThread;
    private Handler mPoolThreadHandler;
    private Handler mUIHandler;
    private Semaphore mSemaphorePoolThreadHandler=new Semaphore(0);

    private Semaphore mSemaphorePoolThread;
    /**
     * 队列调度方式
     */
    public enum Type {
        FIFO, LIFO;
    }

    private ImageLoader(int threadCount, Type type) {
        init(threadCount, type);
    }

    private void init(int threadCount, Type type) {
        mPoolThread = new Thread() {
            @Override
            public void run() {
                Looper.prepare();
                mPoolThreadHandler = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        //从线程池中取出一个任务执行。
                        mThreadPool.execute(getTask());
                        try {
                            mSemaphorePoolThread.acquire();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                };
                mSemaphorePoolThreadHandler.release();
                Looper.loop();
            }
        };

        mPoolThread.start();
        int maxMemory = (int) Runtime.getRuntime().maxMemory();
        int cacheMemory = maxMemory / 8;
        mLruCache = new LruCache<String, Bitmap>(cacheMemory) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getRowBytes() * value.getHeight();
            }
        };
        mThreadPool = Executors.newFixedThreadPool(threadCount);
        mTaskQueue = new LinkedList<Runnable>();
        mType = type;

        mSemaphorePoolThread = new Semaphore(threadCount);
    }

    private Runnable getTask() {
        if (mType == Type.FIFO) {
            return mTaskQueue.removeFirst();
        } else if (mType == Type.LIFO) {
            return mTaskQueue.removeLast();
        }
        return null;
    }

    public static ImageLoader getInstance() {
        if (mInstance == null) {
            synchronized (ImageLoader.class) {
                if (mInstance == null) {
                    mInstance = new ImageLoader(DEFALT_THREAD_COUNT, Type.LIFO);
                }
            }
        }
        return mInstance;
    }
    public static ImageLoader getInstance(int threadCount,Type type) {
        if (mInstance == null) {
            synchronized (ImageLoader.class) {
                if (mInstance == null) {
                    mInstance = new ImageLoader(threadCount, type);
                }
            }
        }
        return mInstance;
    }

    public void loadImage(final String path, final ImageView imageView) {
        imageView.setTag(path);
        if (mUIHandler == null) {
            mUIHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    //获取通过异步加载的图片，并显示在imageView中
                    ImageBeanHolder holder = (ImageBeanHolder) msg.obj;
                    Bitmap bitmap = holder.bitmap;
                    ImageView imageView = holder.imageView;
                    String path = holder.path;
                    if (imageView.getTag().toString().equals(path)) {
                        imageView.setImageBitmap(bitmap);
                    }
                }
            };
        }
        Bitmap bm = getBitmapFromLruCache(path);
        if (bm != null) {
            refreshBitmap(path, imageView, bm);
        } else {
            addTask(new Runnable() {
                @Override
                public void run() {
                    //加载图片，图片的压缩
                    //1，获取图片需要显示的大小
                    ImageSize imageSize = getImageViewSize(imageView);
                    //压缩图片
                    Bitmap bm = decodeSampledBitmapFromPath(path, imageSize.width, imageSize.height);
                    //把图片加入缓存
                    addBitmapToLruCache(path, bm);
                    refreshBitmap(path, imageView, bm);

                    mSemaphorePoolThread.release();
                }
            });
        }
    }

    private void refreshBitmap(String path, ImageView imageView, Bitmap bm) {
        Message message = Message.obtain();
        ImageBeanHolder holder = new ImageBeanHolder(bm, imageView, path);
        message.obj = holder;
        mUIHandler.sendMessage(message);
    }

    private void addBitmapToLruCache(String path, Bitmap bm) {
        if (getBitmapFromLruCache(path) == null && bm != null) {
            mLruCache.put(path, bm);
        }
    }

    private Bitmap decodeSampledBitmapFromPath(String path, int width, int height) {
        //只获取图片宽高，不将图片加载到内存。
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        decodeFile(path, options);

        //获取采样值压缩图片，获取bitmap
        options.inSampleSize = caculateInSampleSize(options, width, height);
        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeFile(path, options);
        return bitmap;
    }


    /**
     * 根据需求的宽高和图片实际的宽高计算SampleSize
     *
     * @param options
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    private int caculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int width = options.outWidth;
        int height = options.outHeight;
        int inSampleSize = 1;
        if (width > reqWidth || height > reqHeight) {
            int widthSample = Math.round(width * 1.0f / reqWidth);
            int heightSample = Math.round(height * 1.0f / reqHeight);
            inSampleSize = Math.min(widthSample, heightSample);
        }
        return inSampleSize;
    }

    private ImageSize getImageViewSize(ImageView imageView) {

        DisplayMetrics metrics = imageView.getContext().getResources().getDisplayMetrics();

        ImageSize imageSize = new ImageSize();
        ViewGroup.LayoutParams lp = imageView.getLayoutParams();
        int width = imageView.getWidth();
        if (width <= 0) {
            width = lp.width;
        }
        if (width <= 0) {
            width = imageView.getMaxWidth();
        }
        if (width <= 0) {
            width = metrics.widthPixels;
        }

        int height = imageView.getHeight();
        if (height <= 0) {
            height = lp.height;
        }
        if (height <= 0) {
            height = imageView.getMaxHeight();
        }
        if (height <= 0) {
            height = metrics.heightPixels;
        }

        imageSize.width = width;
        imageSize.height = height;
        return imageSize;
    }

    private synchronized  void addTask(Runnable runnable) {
        mTaskQueue.add(runnable);
        try {
            if (mPoolThreadHandler==null)
            mSemaphorePoolThreadHandler.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mPoolThreadHandler.sendEmptyMessage(0);
    }

    private Bitmap getBitmapFromLruCache(String key) {
        return mLruCache.get(key);
    }


    private class ImageSize {
        int width;
        int height;
    }


    private class ImageBeanHolder {
        Bitmap bitmap;
        ImageView imageView;
        String path;

        public ImageBeanHolder(Bitmap bitmap, ImageView imageView, String path) {
            this.bitmap = bitmap;
            this.imageView = imageView;
            this.path = path;
        }
    }

}
