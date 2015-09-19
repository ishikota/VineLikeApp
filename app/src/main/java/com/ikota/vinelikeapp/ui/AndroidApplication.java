package com.ikota.vinelikeapp.ui;


import android.app.Application;
import android.content.Context;
import android.graphics.Point;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

public class AndroidApplication extends Application{

    public static int SCREEN_WIDTH;
    public static int SCREEN_HEIGHT;

    @Override
    public void onCreate() {
        super.onCreate();
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        SCREEN_WIDTH = size.x;
        SCREEN_HEIGHT = size.y;
        Log.i("AndroidApplication",
                String.format("Screen size: w = %d, h = %d", SCREEN_WIDTH, SCREEN_HEIGHT));
    }

}
