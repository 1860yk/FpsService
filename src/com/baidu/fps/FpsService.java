package com.baidu.fps;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.Toast;

/**
 * FpsService
 * show and save fps details for app.
 * 
 * @author yuankai02@baidu.com
 * @version 1.0
 * @date 2013-11-14
 */
public class FpsService extends Service implements OnClickListener, FpsView.FpsUpdateListener
{
    private final static String SD_CARD_FILE_DIR = "fps";
    
    private final static String TAG = FpsService.class.getSimpleName();
    
    private final DateFormat mFileNameFormat = new SimpleDateFormat("yyyyMMdd_HH_mm_ss"); 
    private DecimalFormat mFpsFormat = new DecimalFormat("0.0"); 
    private final static int TIMES = 1000000;           // ns => ms
    
    private ViewGroup mLayout = null;
    private FpsView mFpsView = null;
    private Button mStartBtn = null;
    private Button mStopBtn = null;
    
    private final static int FPS_VIEW_ID = 1;
    private final static int START_BTN_ID = 2;
    private final static int STOP_BTN_ID = 3;
    
    private BlockingQueue<ArrayList<FpsRecord>> mRecordQueue = new LinkedBlockingQueue<ArrayList<FpsRecord>>();
    private ArrayList<FpsRecord> mCurrentRecords = null;
    private Worker mWorker = null;
    private Handler mHandler = new Handler();
    
    private Runnable mInvalidateRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            if(mLayout != null)
            {
                mLayout.invalidate();
                mLayout.post(this);
            }
        }
    };
    
    @Override
    public void onCreate()
    {
        
        super.onCreate();
        
        showFpsOnScreen();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        
        return START_STICKY;
    }
    
    private void showFpsOnScreen()
    {
        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE); 
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();    
        layoutParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;    
        /*layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE 
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;*/
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL 
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;                        
        layoutParams.format = PixelFormat.RGBA_8888;    
        layoutParams.gravity = Gravity.TOP | Gravity.LEFT;    
        layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;    
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;    
        layoutParams.x = 0;    
        layoutParams.y = 0;    
        // myLayout is the customized layout which contains textview  
        mLayout = new RelativeLayout(this);
        RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        mFpsView = new FpsView(this);
        mFpsView.setId(FPS_VIEW_ID);
        mFpsView.setListener(this);
        mLayout.addView(mFpsView, rlp);
        
        rlp = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        rlp.addRule(RelativeLayout.BELOW, FPS_VIEW_ID);
        mStartBtn = new Button(this);
        mStartBtn.setId(START_BTN_ID);
        mStartBtn.setText("start");
        mStartBtn.setOnClickListener(this);
        mLayout.addView(mStartBtn, rlp);
        
        rlp = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        rlp.addRule(RelativeLayout.BELOW, START_BTN_ID);
        mStopBtn = new Button(this);
        mStopBtn.setId(STOP_BTN_ID);
        mStopBtn.setText("stop");
        mStopBtn.setOnClickListener(this);
        mLayout.addView(mStopBtn, rlp);
        
        mLayout.post(mInvalidateRunnable);
        
        windowManager.addView(mLayout, layoutParams); 
        
        mStartBtn.setEnabled(true);
        mStopBtn.setEnabled(false);
        
        mWorker = new Worker();
        mWorker.start();
    }
    
    @Override
    public IBinder onBind(Intent intent)
    {
        
        return null;
    }
    
    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if(mLayout != null && mLayout.getParent() != null)
        {
            WindowManager windowManager = (WindowManager)getSystemService(Context.WINDOW_SERVICE);
            windowManager.removeView(mLayout);
            mLayout.removeAllViews();
        }
        mFpsView = null;
        mStartBtn = null;
        mStopBtn = null;
        if(mWorker != null)
        {
            mWorker.quit();
            mWorker = null;
        }
    }
    
    /**
     * every record
     * 
     * @author yuankai02
     * @version 1.0
     * @date 2013-12-14
     */
    private static class FpsRecord
    {
        public FpsRecord(double fps, long frameCount, long realTime)
        {
            mFps = fps;
            mFrameCount = frameCount;
            mRealTime = realTime;
        }
        public double mFps;
        public long mFrameCount;
        public long mRealTime;
    }
    
    /**
     * record worker thread
     * 
     * @author yuankai02
     * @version 1.0
     * @date 2013-12-13
     */
    private class Worker extends Thread
    {
        private boolean mQuit = false;
        
        public void quit()
        {
            mQuit = true;
        }
        
        @Override
        public void run()
        {
            super.run();
            while(!mQuit)
            {
                ArrayList<FpsRecord> records = null;
                try
                {
                    records = mRecordQueue.take();
                }
                catch (InterruptedException e)
                {
                    // TODO Auto-generated catch block
                    continue;
                }
                
                // 进行写文件
                StringBuilder sb = new StringBuilder();
                long totalFps = 0l;
                long totalFrameCount = 0l;
                long totalRealTime = 0l;
                final int size = records.size();
                for(int i = 0;i < size; ++i)
                {
                    FpsRecord record = records.get(i);
                    totalFps += record.mFps;
                    totalFrameCount += record.mFrameCount;
                    totalRealTime += record.mRealTime;
                    sb.append(mFpsFormat.format(record.mFps));
                    sb.append("\t");
                    sb.append(record.mFrameCount);
                    sb.append("\t");
                    sb.append(record.mRealTime / TIMES);
                    sb.append("\n");
                }
                double avgFps = totalFps / size;
                sb.append("\n");
                sb.append(mFpsFormat.format(avgFps));
                sb.append("\t");
                sb.append(totalFrameCount);
                sb.append("\t");
                sb.append(totalRealTime / TIMES);       // to ms
                final File file = createWritableFile();
                if(file == null)
                {
                    continue;
                }
                FileWriter fw = null;
                try
                {
                    fw = new FileWriter(file);
                    fw.write(sb.toString());
                    fw.flush();
                    
                    mHandler.post(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            Toast.makeText(getApplicationContext(), "has write " + size + " FPS records into  :" + file.getAbsolutePath(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                }
                finally
                {
                    if(fw != null)
                    {
                        try
                        {
                            fw.close();
                        }
                        catch (IOException e)
                        {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }
                records.clear();
            }
        }
    }
    
    /**
     * create writeable file in sd card
     * @return null if create fails
     */
    private File createWritableFile()
    {
        File fpsDir = new File(Environment.getExternalStorageDirectory(), SD_CARD_FILE_DIR);
        boolean exists = fpsDir.exists();
        if (!exists) {
            try {
                new File(fpsDir, ".nomedia").createNewFile();
            } catch (IOException e) {
                Log.w(TAG, "Can't create \".nomedia\" file in application external cache directory", e);
            }
            if (!fpsDir.mkdirs()) {
                Log.w(TAG, "Unable to create external cache directory");
                return null;
            }
        }
        
        File file = new File(fpsDir, mFileNameFormat.format(new Date(System.currentTimeMillis())) + ".fps");
        return file;
    }

    @Override
    public void onClick(View v)
    {
        final int id = v.getId();
        if(id == START_BTN_ID)
        {
            // start record
            if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
            {
                mStartBtn.setEnabled(false);
                mStopBtn.setEnabled(true);
                mCurrentRecords = new ArrayList<FpsService.FpsRecord>(3000);        // record for 10min
            }
            else
            {
                Toast.makeText(getApplicationContext(), "sd card is unavailable！", Toast.LENGTH_LONG).show();
            }
        }
        else if(id == STOP_BTN_ID)
        {
            // stop record and save to file
            mStartBtn.setEnabled(true);
            mStopBtn.setEnabled(false);
            
            mRecordQueue.add(mCurrentRecords);
            mCurrentRecords = null;
        }
    }

    @Override
    public void onUpdateFps(double fps, long frameCount, long realTime)
    {
        if(mStartBtn.isEnabled())return;
        mCurrentRecords.add(new FpsRecord(fps, frameCount, realTime));
    }
    
}
