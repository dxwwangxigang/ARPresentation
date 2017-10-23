package com.dxesoft.ar.tango.arpresentation.util;

import com.dxesoft.ar.tango.arpresentation.util.Logger.dummy.Log;

import static android.opengl.GLES20.GL_COMPILE_STATUS;
import static android.opengl.GLES20.GL_FRAGMENT_SHADER;
import static android.opengl.GLES20.GL_LINK_STATUS;
import static android.opengl.GLES20.GL_VALIDATE_STATUS;
import static android.opengl.GLES20.GL_VERTEX_SHADER;
import static android.opengl.GLES20.glAttachShader;
import static android.opengl.GLES20.glCompileShader;
import static android.opengl.GLES20.glCreateProgram;
import static android.opengl.GLES20.glCreateShader;
import static android.opengl.GLES20.glDeleteProgram;
import static android.opengl.GLES20.glDeleteShader;
import static android.opengl.GLES20.glGetProgramInfoLog;
import static android.opengl.GLES20.glGetProgramiv;
import static android.opengl.GLES20.glGetShaderInfoLog;
import static android.opengl.GLES20.glGetShaderiv;
import static android.opengl.GLES20.glLinkProgram;
import static android.opengl.GLES20.glShaderSource;
import static android.opengl.GLES20.glValidateProgram;

/**
 * Created by wangxigang on 2017/7/21.
 */

public class ShaderHelper {
    private static final String TAG = "ShaderHelper";

    /**
     *
     * @param shadercode
     * @return
     */
    public static int compileVertexShader(String shadercode) {
        return compileShader(GL_VERTEX_SHADER, shadercode);
    }

    /**
     *
     * @param shadercode
     * @return
     */
    public static int compileFragmentShader(String shadercode) {
        return compileShader(GL_FRAGMENT_SHADER, shadercode);
    }

    /**
     *
     * @param vertexShaderId
     * @param fragmentShaderId
     * @return
     */
    public static int linkProgram(int vertexShaderId, int fragmentShaderId) {
        final int programObjectId = glCreateProgram();
        final int[] linkStatus = new int[1];

        if (programObjectId == 0) {
            Log.w(TAG, "无法创建新的Program， programObjectId == 0。");
            return 0;
        }
        glAttachShader(programObjectId, vertexShaderId);
        glAttachShader(programObjectId, fragmentShaderId);
        glLinkProgram(programObjectId);

        glGetProgramiv(programObjectId, GL_LINK_STATUS, linkStatus, 0);
        Log.v(TAG, "链接结果：" + glGetProgramInfoLog(programObjectId));
        if (linkStatus[0] == 0) {
            glDeleteProgram(programObjectId);
            Log.w(TAG, "链接失败");
            return 0;
        }

        return programObjectId;
    }

    /**
     *
     * @param type
     * @param shadercode
     * @return
     */
    private static int compileShader(int type, String shadercode) {
        final int shaderObjectId = glCreateShader(type);
        final int[] compileStatus = new int[1];
        if (shaderObjectId == 0) {
            Log.w(TAG, "无法创建shader, shaderObjectId == 0.");
            return 0;
        }

        glShaderSource(shaderObjectId, shadercode);
        glCompileShader(shaderObjectId);

        glGetShaderiv(shaderObjectId, GL_COMPILE_STATUS, compileStatus, 0);
        Log.v(TAG, "编译结果：" + "\n" + shadercode + "\n" + glGetShaderInfoLog(shaderObjectId));
        if (compileStatus[0] == 0) {
            glDeleteShader(shaderObjectId);
        }
        return shaderObjectId;
    }

    private static boolean validateProgram(int programObjectId) {
        glValidateProgram(programObjectId);

        final int[] validateStatus = new int[1];
        glGetProgramiv(programObjectId, GL_VALIDATE_STATUS, validateStatus, 0);
        return validateStatus[0]!=0;
    }
}
