package com.dxesoft.ar.tango.arpresentation;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;

public class WelcomeActivity extends Activity {
    private static final String TAG = WelcomeActivity.class.getSimpleName();
    private Button mButtonPresen;
    private Button mButtonBlank;

    private static final int MODEL_BLANK = 100;
    private static final int MODEL_PRESENTATION = 101;

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        // Example of a call to a native method
        // TextView tv = (TextView) findViewById(R.id.sample_text);
        // tv.setText(stringFromJNI());

        mButtonBlank = (Button)findViewById(R.id.button_blank);
        mButtonPresen = (Button)findViewById(R.id.button_presen);

        mButtonPresen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(WelcomeActivity.this, MainActivity.class);
                intent.putExtra("mode", MODEL_PRESENTATION);
                startActivity(intent);
                WelcomeActivity.this.finish();
            }
        });

        mButtonBlank.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(WelcomeActivity.this, MainActivity.class);
                intent.putExtra("mode", MODEL_BLANK);
                startActivity(intent);
                WelcomeActivity.this.finish();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        setHideVirtualKey(getWindow());
    }

    /**
     * 隐藏虚拟按键
     * @param window
     */
    private void setHideVirtualKey(Window window) {
        int uiOptions =
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        window.getDecorView().setSystemUiVisibility(uiOptions);
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}
