package com.zou.autotouch.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v7.widget.CardView;
import android.view.GestureDetector;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.Toast;

import com.zou.autotouch.R;
import com.zou.autotouch.application.ATApplication;
import com.zou.autotouch.utils.UIUtils;


/**
 * Created by zou on 2016/9/28.
 */

public class FloatBall extends CardView implements GestureDetector.OnGestureListener{
    private static final String TAG ="FloatBall" ;
    private float mTouchStartX;
    private float mTouchStartY;
    private WindowManager wm=(WindowManager)getContext().getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
    private WindowManager.LayoutParams wmParams = ((ATApplication)getContext().getApplicationContext()).getwmParams();
    public static final int WIDTH = 56 ;//球的宽度
    public static final int HEIGHT = 56 ;//球的高度
    private static final float ELEVATION = 10;//阴影高度
    private int width,height;
    private Context context;
    private GestureDetector detector;
    private int currentStatus = STATUS_START_RECORD;
    private static final int STATUS_START_RECORD = 1;//表示开始录制
    private static final int STATUS_STOP_RECORD = 2;//表示停止录制
    private static final int STATUS_HIDE_LIST = 3;//表示隐藏列表
    private TouchFunctionEvent touchFunctionEvent;
    public FloatBall(Context context) {
        super(context);
        this.context = context;
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void init(){
        width = UIUtils.dp2px(WIDTH,context);
        height = UIUtils.dp2px(HEIGHT,context);
        detector = new GestureDetector(context,this);
        setRadius(width/2);//设置圆角
        setElevation(ELEVATION);//设置阴影高度
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.save();
        drawFloatView(canvas);
        canvas.restore();
    }
    private void drawFloatView(Canvas canvas){
        switch (currentStatus){
            case STATUS_START_RECORD:
                Drawable start = getResources().getDrawable(R.drawable.ic_videocam_black_24dp);
                Bitmap bitmap_start = UIUtils.drawableToBitmap(start);
                canvas.drawBitmap(bitmap_start, (UIUtils.dp2px(WIDTH, context) - bitmap_start.getWidth())/2, (UIUtils.dp2px(HEIGHT, context) - bitmap_start.getHeight())/2, null);
                break;
            case STATUS_STOP_RECORD:
                Drawable stop = getResources().getDrawable(R.drawable.ic_videocam_off_black_24dp);
                Bitmap bitmap_stop = UIUtils.drawableToBitmap(stop);
                canvas.drawBitmap(bitmap_stop, (UIUtils.dp2px(WIDTH, context) - bitmap_stop.getWidth())/2, (UIUtils.dp2px(HEIGHT, context) - bitmap_stop.getHeight())/2, null);
                break;
            case STATUS_HIDE_LIST:
                Drawable clear = getResources().getDrawable(R.drawable.ic_clear_black_24dp);
                Bitmap bitmap_clear = UIUtils.drawableToBitmap(clear);
                canvas.drawBitmap(bitmap_clear, (UIUtils.dp2px(WIDTH, context) - bitmap_clear.getWidth())/2, (UIUtils.dp2px(HEIGHT, context) - bitmap_clear.getHeight())/2, null);
                break;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        detector.onTouchEvent(event);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mTouchStartX =  event.getX();
                mTouchStartY =  event.getY()+UIUtils.dp2px(25,context);//25是导航栏的高度
                break;
            case MotionEvent.ACTION_MOVE:
                wmParams.x = (int) (event.getRawX()-mTouchStartX);
                wmParams.y = (int) (event.getRawY()-mTouchStartY);
                wm.updateViewLayout(this, wmParams);
                break;
        }
        return true;
    }

    @Override
    public boolean onDown(MotionEvent motionEvent) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent motionEvent) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent motionEvent) {
        //TODO 单击事件
        switch (currentStatus){
            case STATUS_START_RECORD:
                currentStatus = STATUS_STOP_RECORD;
                invalidate();
                if(touchFunctionEvent!=null){
                    touchFunctionEvent.startRecord();
                }
                break;

            case STATUS_STOP_RECORD:
                currentStatus = STATUS_START_RECORD;
                invalidate();
                if(touchFunctionEvent!=null){
                    touchFunctionEvent.stopRecord();
                }
                break;

            case STATUS_HIDE_LIST:
                currentStatus = STATUS_START_RECORD;
                invalidate();
                break;
        }
        Toast.makeText(context,"单击",Toast.LENGTH_SHORT).show();
        return true;
    }

    @Override
    public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent motionEvent) {
        //TODO 长按事件
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);//震动反馈
        currentStatus = STATUS_HIDE_LIST;
        invalidate();
        if(touchFunctionEvent!=null){
            touchFunctionEvent.showList();
        }
    }

    @Override
    public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
        return false;
    }

    public void setTouchFunctionEvent(TouchFunctionEvent touchFunctionEvent){
        this.touchFunctionEvent = touchFunctionEvent;
    }

    public interface TouchFunctionEvent{
        void startRecord();
        void stopRecord();
        void play();
        void showList();
    }
}
