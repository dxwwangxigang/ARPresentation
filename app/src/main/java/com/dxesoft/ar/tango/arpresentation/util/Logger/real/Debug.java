package com.dxesoft.ar.tango.arpresentation.util.Logger.real;

import android.app.Activity;
import android.widget.Toast;

import static android.opengl.GLES20.GL_VALIDATE_STATUS;
import static android.opengl.GLES20.glGetProgramInfoLog;
import static android.opengl.GLES20.glGetProgramiv;
import static android.opengl.GLES20.glValidateProgram;

/**
 * Created by wangxigang on 2017/7/18.
 */

public class Debug {
    private static final String TAG = "ShaderHelper";

    public static void showToast(Activity pActivity, final String text) {
        final Activity activity = pActivity;
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(activity, text, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    public static void ShowDebugLog(String tag, String msg) {
        //Log.d(tag, msg);
    }

    public static boolean validateProgram(int programObjectId) {
        glValidateProgram(programObjectId);

        final int[] validateStatus = new int[1];
        glGetProgramiv(programObjectId, GL_VALIDATE_STATUS, validateStatus, 0);
        Log.v(TAG, "校验结果：" + validateStatus[0] + "\n" + glGetProgramInfoLog(programObjectId));

        return validateStatus[0] != 0;
    }
}
