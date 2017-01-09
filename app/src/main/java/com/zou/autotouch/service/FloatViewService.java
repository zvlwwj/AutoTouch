package com.zou.autotouch.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.view.Gravity;
import android.view.WindowManager;

import com.zou.autotouch.application.ATApplication;
import com.zou.autotouch.utils.UIUtils;
import com.zou.autotouch.view.FloatBall;

/**
 * Created by zou on 2016/10/9.
 */

public class FloatViewService extends Service {
    private static final String TAG = "FloatViewService";
    private WindowManager mWindowManager;
    private WindowManager.LayoutParams params;
    private FloatBall floatBall;
    private static final int MARGING_LEFT=15;
    private static final int MARGING_TOP=15;
    private RecordSession session;
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        session = new RecordSession();
        showView();
        setListener();
    }

    private void showView(){
        //获取WindowManager
        mWindowManager=(WindowManager)getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        floatBall = new FloatBall(this);
        params = ((ATApplication)getApplication()).getwmParams();

        params.type= WindowManager.LayoutParams.TYPE_TOAST;     // 系统提示类型,重要
        params.format= PixelFormat.RGBA_8888;
        params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;

        params.gravity= Gravity.LEFT| Gravity.TOP;   //调整悬浮窗口至左上角
        //以屏幕左上角为原点，设置x、y初始值
        params.x=UIUtils.dp2px(MARGING_LEFT,getApplicationContext());
        params.y=UIUtils.dp2px(MARGING_TOP,getApplicationContext());

        params.width= UIUtils.dp2px(FloatBall.WIDTH,getApplicationContext());
        params.height=UIUtils.dp2px(FloatBall.HEIGHT,getApplicationContext());
        mWindowManager.addView(floatBall, params);
    }

    private void setListener() {
        floatBall.setTouchFunctionEvent(new FloatBall.TouchFunctionEvent() {
            @Override
            public void startRecord() {
                session.startRecord();
            }

            @Override
            public void stopRecord() {
                session.stopRecord();
            }

            @Override
            public void play() {
                session.play();
            }
        });
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        //在程序退出(Activity销毁）时销毁悬浮窗口
        mWindowManager.removeView(floatBall);
    }


}
