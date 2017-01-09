package com.zou.autotouch.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.WindowManager;
import android.widget.EditText;

import java.util.HashMap;
import java.util.List;

/**
 * Created by zou on 2016/6/2.
 */
public class UIUtils {
    /**
     * 获取屏幕高度
     * @param context
     * @return
     */
    public static int getScreenHeight(Context context){
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        return wm.getDefaultDisplay().getHeight();
    }
    /**
     * 获取屏幕宽度
     * @param context
     * @return
     */
    public static int getScreenWidth(Context context){
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        return wm.getDefaultDisplay().getWidth();
    }

    /**
     * 获取版本名称
     * @param context
     * @return
     * @throws Exception
     */
    public static String getVersionName(Context context) throws Exception
    {
        // 获取packagemanager的实例
        PackageManager packageManager = context.getPackageManager();
        // getPackageName()是你当前类的包名，0代表是获取版本信息
        PackageInfo packInfo = packageManager.getPackageInfo(context.getPackageName(),0);
        String version = packInfo.versionName;
        return version;
    }
    /**
     * dp转px
     * @param dp
     * @param context
     * @return
     */
    public static int dp2px(int dp,Context context) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                context.getResources().getDisplayMetrics());
    }

    public static int px2dp(int px,Context context) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int dp = Math.round(px / (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
        return dp;
    }

    /**
     * 将px值转换为sp值，保证文字大小不变
     *
     *    （DisplayMetrics类中属性scaledDensity）
     * @return
     */
    public static int px2sp(Context context, float pxValue) {
        final float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        return (int) (pxValue / fontScale + 0.5f);
    }

    /**
     * drawable转换Bitmap
     * @param drawable
     * @return
     */
    public static Bitmap drawableToBitmap (Drawable drawable) {

        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable)drawable).getBitmap();
        }

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    /**
     * 判断EditText是否为空
     * @param editText
     * @return
     */
    public static boolean isEmptyEdit(EditText editText){
        if(editText.getText().toString()==null||"".equals(editText.getText().toString())){
            return true;
        }
        return false;
    }

    //获得coolie 的鍵值隊
    public HashMap<String, String> cookieParse(String cookies){
        String[] cookieArray = cookies.split("; ");     //得到分割的cookie名值对
        HashMap<String, String> cookieMap = new HashMap<String, String>();
        for (int i=0;i<cookieArray.length;i++){
            String[] arr = cookieArray[i].split("=");   //得到鍵值對，key/value
            if (arr.length > 1)
                cookieMap.put(arr[0], arr[1]);
        }
        return cookieMap;
    }

    public static boolean isWebActivityRunning(Context context){
        boolean isAppRunning = false;
        ActivityManager am = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> list = am.getRunningTasks(100);

        String MY_PKG_NAME = "com.oray.peanuthull";
        for (ActivityManager.RunningTaskInfo info : list) {
            if (info.topActivity.getPackageName().equals(MY_PKG_NAME) || info.baseActivity.getPackageName().equals(MY_PKG_NAME)) {
                isAppRunning = true;
//                Log.i(TAG,info.topActivity.getPackageName() + " info.baseActivity.getPackageName()="+info.baseActivity.getPackageName());
                break;
            }
        }
        return isAppRunning;
    }
}
