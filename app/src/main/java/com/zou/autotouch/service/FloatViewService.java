package com.zou.autotouch.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;

import com.zou.autotouch.application.ATApplication;
import com.zou.autotouch.utils.UIUtils;
import com.zou.autotouch.view.FloatBall;

/**
 * Created by zou on 2016/10/9.
 */

public class FloatViewService extends Service {
    private static final String TAG = "FloatViewService";
    private WindowManager mWindowManager;
    private WindowManager.LayoutParams params_ball;
    private WindowManager.LayoutParams params_list;
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
        params_ball = ((ATApplication)getApplication()).getwmParams();

        params_ball.type= WindowManager.LayoutParams.TYPE_TOAST;     // 系统提示类型,重要
        params_ball.format= PixelFormat.RGBA_8888;
        params_ball.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;

        params_ball.gravity= Gravity.LEFT| Gravity.TOP;   //调整悬浮窗口至左上角
        //以屏幕左上角为原点，设置x、y初始值
        params_ball.x=UIUtils.dp2px(MARGING_LEFT,getApplicationContext());
        params_ball.y=UIUtils.dp2px(MARGING_TOP,getApplicationContext());

        params_ball.width= UIUtils.dp2px(FloatBall.WIDTH,getApplicationContext());
        params_ball.height=UIUtils.dp2px(FloatBall.HEIGHT,getApplicationContext());
        mWindowManager.addView(floatBall, params_ball);
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
//                session.play();
            }

            @Override
            public void showList() {
                Button btn = new Button(FloatViewService.this);
                btn.setText("test");
                btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        session.play();
                    }
                });
                params_list = new WindowManager.LayoutParams();

                params_list.type= WindowManager.LayoutParams.TYPE_TOAST;     // 系统提示类型,重要
                params_list.format= PixelFormat.RGBA_8888;
                params_list.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;

                params_list.gravity= Gravity.LEFT| Gravity.TOP;   //调整悬浮窗口至左上角
                //以屏幕左上角为原点，设置x、y初始值
                params_list.x=UIUtils.dp2px(80,getApplicationContext());
                params_list.y=UIUtils.dp2px(20,getApplicationContext());

                params_list.width= UIUtils.dp2px(100,getApplicationContext());
                params_list.height=UIUtils.dp2px(50,getApplicationContext());
                mWindowManager.addView(btn, params_list);

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
