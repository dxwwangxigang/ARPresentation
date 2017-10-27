package com.dxesoft.ar.tango.arpresentation;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

import com.dxesoft.ar.tango.arpresentation.util.RoomViewPager;
import com.dxesoft.ar.tango.arpresentation.util.RoomViewPagerAdapter;

import java.util.ArrayList;
import java.util.List;

public class WelcomeActivity extends Activity {
    private static final String TAG = WelcomeActivity.class.getSimpleName();
    private View mLayoutView;
    private ImageButton mButtonPresen;
    private ImageButton mButtonBlank;
    private ImageButton mButtonAddRoom;

    // 临时view
    private View view1, view2, view3, view4;
    // ViewPager
    private ViewPager mViewPager;
    private List<View> mViewList;

    private static final int MODEL_BLANK = 100;
    private static final int MODEL_PRESENTATION = 101;

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLayoutView = (RelativeLayout)getLayoutInflater().inflate(R.layout.activity_welcome, null);
        setContentView(mLayoutView);
        mLayoutView = getLayoutInflater().inflate(R.layout.layout_viewpager_at_welcome_page, (ViewGroup)mLayoutView, true);
        // Example of a call to a native method
        // TextView tv = (TextView) findViewById(R.id.sample_text);
        // tv.setText(stringFromJNI());

        mButtonAddRoom = (ImageButton)findViewById(R.id.button_add_room);
        mButtonAddRoom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(WelcomeActivity.this, AddRoomActivity.class);
                startActivity(intent);
            }
        });

        mViewPager = (RoomViewPager)findViewById(R.id.viewpager);
        LayoutInflater layoutInflater = getLayoutInflater();
        view1 = layoutInflater.inflate(R.layout.layout_room_selector, null);
        view2 = layoutInflater.inflate(R.layout.layout_room_selector2, null);
        view3 = layoutInflater.inflate(R.layout.layout_room_selector3, null);
        view4 = layoutInflater.inflate(R.layout.layout_blank_room, null);

        mViewList = new ArrayList<View>();
        mViewList.add(view1);
        mViewList.add(view2);
        mViewList.add(view3);
        mViewList.add(view4);

        RoomViewPagerAdapter viewPagerAdapter = new RoomViewPagerAdapter(mViewList);
        mViewPager.setAdapter(viewPagerAdapter);



        mButtonBlank = (ImageButton)view4.findViewById(R.id.button_into_blank);
        mButtonPresen = (ImageButton)view1.findViewById(R.id.button_enter_room);

        mButtonPresen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(WelcomeActivity.this, PresentInRoomActivity.class);
                startActivity(intent);
            }
        });

        mButtonBlank.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(WelcomeActivity.this, PresentOnPlaneActivity.class);
                startActivity(intent);
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
