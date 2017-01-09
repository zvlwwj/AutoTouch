package com.zou.autotouch.application;

import android.app.Application;
import android.graphics.PointF;
import android.view.WindowManager;

/**
 * Created by zou on 2016/10/9.
 */

public class ATApplication extends Application {
    private WindowManager.LayoutParams wmParams =new WindowManager.LayoutParams();

    private PointF point = new PointF(0,0);

    public WindowManager.LayoutParams getwmParams(){
        return wmParams;
    }

    public void setPoint(float x,float y){
        if(x!=0) {
            point.x = x;
        }
        if(y!=0){
            point.y = y;
        }
    }

    public PointF getPoint(){
        return point;
    }
}
