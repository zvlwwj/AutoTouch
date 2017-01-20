package com.zou.autotouch.fragment;

import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;

import com.zou.autotouch.R;
import com.zou.autotouch.service.RecordSession;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by zou on 2016/12/30.
 */

public class GestureRecordedFragment extends Fragment {
    private static final String TAG = "GestureRecordedFragment";
    private Button btn_test;
    private View mView;
    private int i;
    RecordSession session;
    private Timer timer;
    public GestureRecordedFragment(){

    }
    public static GestureRecordedFragment newInstance(){
        GestureRecordedFragment fragment = new GestureRecordedFragment();
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_gesture_recorded,container,false);
        initView();
        return mView;
    }

    private void initView() {
        timer = new Timer();
        session = new RecordSession();
        btn_test = (Button) mView.findViewById(R.id.btn_test);
        btn_test.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                session.play();
            }
        });
        mView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, final MotionEvent motionEvent) {
//                long time1 = 0;
//                switch (motionEvent.getAction()){
//                    case MotionEvent.ACTION_DOWN:
//                        time1 = SystemClock.currentThreadTimeMillis();
//                        break;
//                    case MotionEvent.ACTION_UP:
//                        long time2 = SystemClock.currentThreadTimeMillis();
//                        Log.i(TAG,"dis time :" + (time2-time1));
//                        break;
//                }
//                i++;
//                Log.i(TAG,"Touch event motionEvent------"+i+"----------- x: "+ motionEvent.getRawX()+" y: "+motionEvent.getRawY());
//                switch (motionEvent.getAction()){
//                    case MotionEvent.ACTION_DOWN:
//                        session.touchdown((int)motionEvent.getRawX(),(int)motionEvent.getRawY());
//                        break;
//                    case MotionEvent.ACTION_UP:
//                        session.touchup();
//                        break;
//                    case MotionEvent.ACTION_MOVE:
//                        session.touchmove((int)motionEvent.getRawX(),(int)motionEvent.getRawY());
//                        break;
//                }
                return true;
            }
        });
    }
}
