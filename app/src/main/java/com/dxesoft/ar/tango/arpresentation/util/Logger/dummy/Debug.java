package com.dxesoft.ar.tango.arpresentation.util.Logger.dummy;

import android.app.Activity;

/**
 * Created by wangxigang on 2017/7/18.
 * package com.dxwsoft.arbim.arbim.dummy中的所有类的方法都是dummy的，所以什么也没有
 */

public class Debug {

    public void showToast(Activity pActivity, final String text) {}
    public void ShowDebugLog(String tag, String msg){}
    public static boolean validateProgram(int programObjectId) {
        return true;
    }
}
