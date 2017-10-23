package com.dxesoft.ar.tango.arpresentation.util.Logger.real;

/**
 * Created by wangxigang on 2017/7/21.
 */

public class Log {
    public static void w(String tag, String msg){
        android.util.Log.w(tag, msg);
    }

    public static void v(String tag, String msg){
        android.util.Log.v(tag, msg);
    }
}
