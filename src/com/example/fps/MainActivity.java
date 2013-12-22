package com.example.fps;

import com.baidu.fps.FpsService;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;

public class MainActivity extends Activity
{
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        showFps(true);
    }
    
    @Override
    protected void onDestroy()
    {
        // TODO Auto-generated method stub
        super.onDestroy();
        showFps(false);
    }
    
    private void showFps(boolean show)
    {
        if(show)
        {
            Intent intent = new Intent(this, FpsService.class);
            startService(intent);
        }
        else
        {
            Intent intent = new Intent(this, FpsService.class);
            stopService(intent);
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
}
