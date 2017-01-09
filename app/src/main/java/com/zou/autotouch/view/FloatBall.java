package com.zou.autotouch.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v7.widget.CardView;
import android.view.MotionEvent;
import android.view.WindowManager;

import com.zou.autotouch.R;
import com.zou.autotouch.application.ATApplication;
import com.zou.autotouch.utils.UIUtils;


/**
 * Created by zou on 2016/9/28.
 */

public class FloatBall extends CardView {
    private static final String TAG ="HorizontalRuler" ;
    private float mTouchStartX;
    private float mTouchStartY;
    private WindowManager wm=(WindowManager)getContext().getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
    private WindowManager.LayoutParams wmParams = ((ATApplication)getContext().getApplicationContext()).getwmParams();
//    private Paint paint;
    private static final int WIDTH = 56 ;//球的宽度
    private static final int HEIGHT = 56 ;//球的高度
    private static final float ELEVATION = 10;//阴影高度
    private int width,height;
    private Context context;
    public FloatBall(Context context) {
        super(context);
        this.context = context;
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void init(){
        width = UIUtils.dp2px(WIDTH,context);
        height = UIUtils.dp2px(HEIGHT,context);
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
        Drawable drawable = getResources().getDrawable(R.drawable.ic_videocam_black_24dp);
        Bitmap bitmap = UIUtils.drawableToBitmap(drawable);
        canvas.drawBitmap(bitmap, (UIUtils.dp2px(WIDTH, context) - bitmap.getWidth())/2, (UIUtils.dp2px(HEIGHT, context) - bitmap.getHeight())/2, null);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
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
}
