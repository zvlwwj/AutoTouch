package com.zou.autotouch;

import java.io.Closeable;
import java.io.IOException;

/**
 * 关闭流的工具类
 * Created by Neo on 2016/9/29.
 */

public class CloseUtils {
    private CloseUtils() {}

    /**
     * 关闭closeble对象
     * @param closeable
     */
    public static void closeQuietly(Closeable closeable) {
        if(null != closeable){
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
