package com.baidu.fps;

import java.text.DecimalFormat;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.widget.TextView;

/**
 * FpsView shows the instant fps to the screen
 * 
 * @author yuankai02@baidu.com
 * @version 1.0
 * @date 2013-12-22
 */
public class FpsView extends TextView {
    /**
     * 200ms calcute period (ns)
     */
    public static final long PERIOD = (long) (1000000000L / 5); 
    
    /**
     * 1 seconds (ns)
     */
    public static long FPS_MAX_INTERVAL = 1000000000L; 
    
    private double mNowFPS = 0.0;
    private double mMaxFps = 0.0;
    private double mMinFps = Double.MAX_VALUE;
    private double mAvgFps = 0.0;
    private FpsUpdateListener mListener = null;
    
    private long mInterval = 0L;
    private long mLastTime = 0L;
    private long mTime = -1;
    private long mFrameCount = 0;
    private long mCaculateTimes = 0;
    private DecimalFormat mDecimalFormat = new DecimalFormat("0.0"); 
    
    /**
     * fps update listener 
     * 
     * @author yuankai02@baidu.com
     * @version 1.0
     * @date 2013-12-22
     */
    public interface FpsUpdateListener
    {
        /**
         * fps update callback
         * @param fps
         */
        public void onUpdateFps(double fps, long frameCount, long duration);
    }
	
	public FpsView(Context context) {
		super(context);
		
		setTextColor(Color.parseColor("#ff0000"));
		setText(" ");
	}

	@Override
	public void draw(Canvas canvas) {
	    if(mLastTime == 0)
	    {
	        mLastTime = System.nanoTime();
	        mTime = System.nanoTime();
	    }
		super.draw(canvas);
		
		long timeNow = System.nanoTime();
		mFrameCount++;
        mInterval += timeNow - mLastTime;
        mLastTime = timeNow;
        if (mInterval >= PERIOD)
        {
            // nanoTime()
            long realTime = timeNow - mTime /*- (interval - PERIOD)*/; // 单位: ns
            // caculate to real fps 
            mNowFPS = ((double) mFrameCount / realTime) * FPS_MAX_INTERVAL;
            
            // show
            updateFps(mFrameCount, realTime);
            
            // reset
            mFrameCount = 0L;
            mInterval = 0;
            mTime = timeNow;
        }
	}
	
	public void setListener(FpsUpdateListener listener)
	{
	    mListener = listener;
	}
	
	private void updateFps(long frameCount, long realTime)
	{
	    if(mListener != null)
	    {
	        mListener.onUpdateFps(mNowFPS, frameCount, realTime);
	    }
	    
	    if(mNowFPS > mMaxFps)
	    {
	        mMaxFps = mNowFPS;
	    }
	    if(mNowFPS < mMinFps)
        {
            mMinFps = mNowFPS;
        }
	    
	    mAvgFps = (double)(mAvgFps * mCaculateTimes + mNowFPS) / (mCaculateTimes + 1);
	    mCaculateTimes ++;
	    
        setText("FPS: " + mDecimalFormat.format(mNowFPS) + "\n" + 
                "AVG: " + mDecimalFormat.format(mAvgFps) + "\n" +
                "MAX: " + mDecimalFormat.format(mMaxFps) + "\n" + 
                "MIN: " + mDecimalFormat.format(mMinFps) + 
                "");
	}
}
