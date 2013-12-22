package com.baidu.fps;

import java.text.DecimalFormat;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.widget.TextView;

public class FpsView extends TextView {
    /**
     * 换算为运行周期
     * 单位: ns(纳秒)
     */
    public static final long PERIOD = (long) (1000000000L / 5); 
    /**
     * FPS最大间隔时间，换算为1s = 10^9ns 
     * 单位: ns
     */
    public static long FPS_MAX_INTERVAL = 1000000000L; 
    
    /**
     * 实际的FPS数值
     */
    private double mNowFPS = 0.0;
    private double mMaxFps = 0.0;
    private double mMinFps = Double.MAX_VALUE;
    private double mAvgFps = 0.0;
    private FpsUpdateListener mListener = null;
    
    /**
     * 此level代表画面静止时的最低FPS，不计入平均数
     */
    private final static int ACTIVE_LIMIT = 60;
    
    /**
     * FPS累计用间距时间
     * in ns
     */
    private long interval = 0L;
    
    private long mLastTime = 0L;
    
    private long time = -1;
    /**
     * 运行桢累计 
     */
    private long frameCount = 0;
    
    private long mCaculateTimes = 0;
    
    public interface FpsUpdateListener
    {
        /**
         * fps更新回调
         * @param fps
         */
        public void onUpdateFps(double fps, long frameCount, long duration);
    }
    
    /**
     * 格式化小数位数 
     */
    private DecimalFormat df = new DecimalFormat("0.0"); 
	
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
	        time = System.nanoTime();
	    }
		super.draw(canvas);
		
		long timeNow = System.nanoTime();
		frameCount++;
        interval += timeNow - mLastTime;
        mLastTime = timeNow;
//		interval += PERIOD;
        //当实际间隔符合时间时。
        if (interval >= /*FPS_MAX_INTERVAL*/PERIOD)
        {
            //nanoTime()返回最准确的可用系统计时器的当前值，以毫微秒为单位
            // 获得到目前为止的时间距离
            long realTime = timeNow - time /*- (interval - PERIOD)*/; // 单位: ns
            //换算为实际的fps数值
            mNowFPS = ((double) frameCount / realTime) * FPS_MAX_INTERVAL;
            
            // 显示
            updateFps(frameCount, realTime);
            
            //变更数值
            frameCount = 0L;
            interval = 0;
            time = timeNow;
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
	    
        setText("FPS: " + df.format(mNowFPS) + "\n" + 
                "AVG: " + df.format(mAvgFps) + "\n" +
                "MAX: " + df.format(mMaxFps) + "\n" + 
                "MIN: " + df.format(mMinFps) + 
                "");
	}
}
