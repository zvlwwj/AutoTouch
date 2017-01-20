package com.zou.autotouch.jni;

/**
 * Created by zou on 2017/1/20.
 */

public class InputEventJni {
    static {
        System.loadLibrary("inputevent");
    }
    public native void touchdown(int x,int y);
    public native void touchmove(int x,int y);
    public native void touchup(int x,int y);
    public native void touch(int arg1,int arg2,int arg3);
}
