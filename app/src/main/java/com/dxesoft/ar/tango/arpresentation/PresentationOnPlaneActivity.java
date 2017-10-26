package com.dxesoft.ar.tango.arpresentation;

import android.Manifest;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.opengl.Matrix;
import android.os.Bundle;
import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.google.atap.tangoservice.Tango;
import com.google.atap.tangoservice.TangoCameraIntrinsics;
import com.google.atap.tangoservice.TangoConfig;
import com.google.atap.tangoservice.TangoCoordinateFramePair;
import com.google.atap.tangoservice.TangoErrorException;
import com.google.atap.tangoservice.TangoEvent;
import com.google.atap.tangoservice.TangoException;
import com.google.atap.tangoservice.TangoInvalidException;
import com.google.atap.tangoservice.TangoOutOfDateException;
import com.google.atap.tangoservice.TangoPointCloudData;
import com.google.atap.tangoservice.TangoPoseData;
import com.google.atap.tangoservice.TangoXyzIjData;
import com.google.tango.support.TangoPointCloudManager;
import com.google.tango.support.TangoSupport;
import com.karumi.expandableselector.ExpandableItem;
import com.karumi.expandableselector.ExpandableSelector;
import com.karumi.expandableselector.ExpandableSelectorListener;
import com.karumi.expandableselector.OnExpandableItemClickListener;

import org.rajawali3d.scene.ASceneFrameCallback;
import org.rajawali3d.view.SurfaceView;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

public class PresentationOnPlaneActivity extends Activity{
    /**********   全局相关属性   **********/
    private static final String TAG = PresentationOnPlaneActivity.class.getSimpleName();
    private static final int MODEL_BLANK = 100;
    private static final int MODEL_PRESENTATION = 101;
    private int mMode;
    private boolean mRemoveMask = false;
    private Handler mHandlerMaskControl = new Handler(){
        public void handleMessage(android.os.Message msg){
            switch (msg.what){
                case EVENT_STARTUP_TANGO_DISPLAY:
                    mTextViewStartTango.setVisibility(View.VISIBLE);
                    break;
                case EVENT_STARTUP_TANGO_OK_DISPLAY:
                    mTextViewStartTangoOK.setVisibility(View.VISIBLE);
                    break;
                case EVENT_LOAD_MODEL_DISPLAY:
                    mTextViewLoadModel.setVisibility(View.VISIBLE);
                    break;
                case EVENT_LOAD_MODEL_OK_DISPLAY:
                    mTextViewLoadModelOK.setVisibility(View.VISIBLE);
                    break;
                case EVENT_HIDE_ALL:
                    mLayoutMask.setVisibility(View.INVISIBLE);
                    break;
                case EVENT_SHOW_PUTMODEL:
                    mButtonPutModel.setVisibility(View.VISIBLE);
                default:
                    break;
            }
        }
    };
    private static final int EVENT_STARTUP_TANGO_DISPLAY = 0x201;
    private static final int EVENT_STARTUP_TANGO_OK_DISPLAY = 0x202;
    private static final int EVENT_LOAD_MODEL_DISPLAY = 0x203;
    private static final int EVENT_LOAD_MODEL_OK_DISPLAY = 0x204;
    private static final int EVENT_SHOW_PUTMODEL = 0x205;
    private static final int EVENT_HIDE_PUTMODEL = 0x206;
    private static final int EVENT_HIDE_ALL = 0x301;

    private Handler mHandlerFindFocus = new Handler(){
        public void handleMessage(android.os.Message msg){
            if (msg.what == EVENT_GOT_FOCUS){
                mRenderer.loadFocus();
            }else if (msg.what == EVENT_LOSE_FOCUS){
                //mGLSurfaceView.setVisibility(View.INVISIBLE);
                mRenderer.eraseFocus();
            }else if (msg.what == EVENT_LOAD_MODEL){
                //mGLSurfaceView.setVisibility(View.INVISIBLE);
                //mRenderer.loadModel();
            }else if (msg.what == EVENT_ERASE_MODEL){
                //mGLSurfaceView.setVisibility(View.INVISIBLE);
                mRenderer.eraseModel();
            }
        }
    };
    private static final int EVENT_GOT_FOCUS = 0x401;
    private static final int EVENT_LOSE_FOCUS = 0x402;
    private static final int EVENT_LOAD_MODEL = 0x403;
    private static final int EVENT_ERASE_MODEL = 0x404;

    private WorkThread mWorkThread;
    private Timer mTimer = new Timer();
    private float[] lMatrix = {
            1f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f,
            0f, 0f, 1f, 0f,
            0f, 0f, 0f, 1f
    };
    private MyTimerTask mTimerTask = new MyTimerTask();

    /**********   控件   **********/
    private TextView mTextViewStartTango;
    private TextView mTextViewStartTangoOK;
    private TextView mTextViewLoadModel;
    private TextView mTextViewLoadModelOK;
    private ImageView mImageViewMask;
    private ExpandableSelector mExpandableSelector;
    private ImageView mImageViewFocusing;
    private ImageButton mButtonPutModel;
    private ImageButton mButtonZoomIn;
    private ImageButton mButtonZoomOut;
    private ImageButton mButtonZoomCancel;

    private View mLayoutView;
    private RelativeLayout mLayoutZoom;
    private RelativeLayout mLayoutMask;

    /**********   Camera权限申请相关   **********/
    private static final String CAMERA_PERMISSION = Manifest.permission.CAMERA;
    private static final int CAMERA_PERMISSION_CODE = 0;
    /**********   Camera显示相关   **********/
    private SurfaceView mSurfaceView;
    private int mDisplayRotation;

    /**********   Tango相关   **********/
    private TangoPointCloudManager mPointCloudManager;
    private Tango mTango;
    private TangoConfig mConfig;
    private boolean mIsConnected = false;
    private double mCameraPoseTimestamp = 0;
    // Texture rendering related fields
    // NOTE: Naming indicates which thread is in charge of updating this variable.
    private static final int INVALID_TEXTURE_ID = 0;
    private int mConnectedTextureIdGlThread = INVALID_TEXTURE_ID;
    private AtomicBoolean mIsFrameAvailableTangoThread = new AtomicBoolean(false);
    private double mRgbTimestampGlThread;
    private float[] mDepthTPlane;
    private double mPlanePlacedTimestamp;
    /**********   模型Renderder相关   **********/
    private PlaneFittingRenderer mRenderer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_presentation_on_plane);
        mLayoutView = (RelativeLayout)getLayoutInflater().inflate(R.layout.activity_presentation_on_plane, null);

        setContentView(mLayoutView);
        mLayoutView = getLayoutInflater().inflate(R.layout.zoom_adjust, (ViewGroup)mLayoutView, true);
        mLayoutView = getLayoutInflater().inflate(R.layout.layout_mask, (ViewGroup)mLayoutView, true);
        mLayoutZoom = (RelativeLayout)findViewById(R.id.zoom_toast);
        mButtonZoomIn = (ImageButton)findViewById(R.id.imagebutton_zoom_in);
        mButtonZoomOut = (ImageButton)findViewById(R.id.imagebutton_zoom_out);
        mButtonZoomCancel = (ImageButton)findViewById(R.id.imagebutton_cancel);
        mButtonZoomIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mRenderer.setModelScale(2);
            }
        });
        mButtonZoomOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mRenderer.setModelScale(0.5f);
            }
        });
        mButtonZoomCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                mButtonZoomOut.setVisibility(View.INVISIBLE);
//                mButtonZoomIn.setVisibility(View.INVISIBLE);
//                mButtonZoomCancel.setVisibility(View.INVISIBLE);
                mLayoutZoom.setVisibility(View.INVISIBLE);
            }
        });
        mMode = getIntent().getIntExtra("mode", 0);

        /**
         * mask相关的控件定义&初始化
         */
        mTextViewStartTango = (TextView)findViewById(R.id.textView_strating_tango);
        mTextViewStartTangoOK = (TextView)findViewById(R.id.textView_strating_tango_ok);
        mTextViewLoadModel = (TextView)findViewById(R.id.textView_loading_model);
        mTextViewLoadModelOK = (TextView)findViewById(R.id.textView_loading_model_ok);
        mImageViewMask = (ImageView)findViewById(R.id.imageview_mask);
        mButtonPutModel = (ImageButton)findViewById(R.id.imageButton_putmodel);

        mTextViewStartTango.setVisibility(View.INVISIBLE);
        mTextViewStartTangoOK.setVisibility(View.INVISIBLE);
        mTextViewLoadModel.setVisibility(View.INVISIBLE);
        mTextViewLoadModelOK.setVisibility(View.INVISIBLE);
        mImageViewMask.setVisibility(View.VISIBLE);

        mButtonPutModel.setVisibility(View.INVISIBLE);
        mLayoutZoom.setVisibility(View.INVISIBLE);

        /**
         * 一般控件的定义&初始化
         */

        mLayoutMask = (RelativeLayout)findViewById(R.id.layout_mask);
        mButtonPutModel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mRenderer.loadModel();
                mImageViewFocusing.setVisibility(View.INVISIBLE);
                mTimerTask.cancel();
            }
        });


        // 初始化SurfaceView
        mSurfaceView = (SurfaceView) findViewById(R.id.ar_view_on_plane);
        mRenderer = new PlaneFittingRenderer(this);
        mSurfaceView.setSurfaceRenderer(mRenderer);
        // 不要让SurfaceView显示在最上层
        mSurfaceView.setZOrderOnTop(false);
        mPointCloudManager = new TangoPointCloudManager();
        // 管理显示属性
        DisplayManager displayManager = (DisplayManager) getSystemService(DISPLAY_SERVICE);
        if (displayManager != null) {
            // 监听显示变化
            displayManager.registerDisplayListener(new DisplayManager.DisplayListener() {
                // 当一个逻辑显示设备被加载到系统时
                @Override
                public void onDisplayAdded(int displayId) {
                }
                // 当逻辑显示设备的属性发生变化时
                @Override
                public void onDisplayChanged(int displayId) {
                    synchronized (this) {
                        setDisplayRotation();
                    }
                }
                // 当逻辑显示设备被移除系统时
                @Override
                public void onDisplayRemoved(int displayId) {
                }
            }, null);
        }
    }

    private void updateIconsFirstButtonResource(int resourceId) {
        ExpandableItem arrowUpExpandableItem = new ExpandableItem();
        arrowUpExpandableItem.setResourceId(resourceId);
        mExpandableSelector.updateExpandableItem(0, arrowUpExpandableItem);
    }

    @Override
    protected void onStart() {
        super.onStart();
        setHideVirtualKey(getWindow());

        if (mMode == 0){
            Toast.makeText(this, "传递参数失败", Toast.LENGTH_SHORT).show();
        } else {
            mRemoveMask = true;
            if (checkAndRequestPermissions()) {

                mHandlerMaskControl.sendEmptyMessage(EVENT_STARTUP_TANGO_DISPLAY);

                bindTangoService();

                mHandlerMaskControl.sendEmptyMessage(EVENT_STARTUP_TANGO_OK_DISPLAY);
            }

            if (mMode == MODEL_BLANK){

                mImageViewFocusing = (ImageView)findViewById(R.id.imageview_focusing);
                mImageViewFocusing.setVisibility(View.INVISIBLE);

                // 扩展Button的初始化
                mExpandableSelector = (ExpandableSelector)findViewById(R.id.es_icon);
                List<ExpandableItem> expandableItems = new ArrayList<ExpandableItem>();
                ExpandableItem item = new ExpandableItem();
                item.setResourceId(R.mipmap.ic_keyboard_arrow_up_black);
                expandableItems.add(item);
                item = new ExpandableItem();
                item.setResourceId(R.mipmap.location);
                expandableItems.add(item);
                item = new ExpandableItem();
                item.setResourceId(R.mipmap.rotation);
                expandableItems.add(item);
                item = new ExpandableItem();
                item.setResourceId(R.mipmap.zoom);
                expandableItems.add(item);
//                item = new ExpandableItem();
//                item.setResourceId(R.mipmap.move);
//                expandableItems.add(item);
                mExpandableSelector.showExpandableItems(expandableItems);
                mExpandableSelector.setOnExpandableItemClickListener(new OnExpandableItemClickListener() {
                    @Override
                    public void onExpandableItemClickListener(int index, View view) {
                        if (index == 0 && mExpandableSelector.isExpanded()) {
                            mExpandableSelector.collapse();
                            updateIconsFirstButtonResource(R.mipmap.ic_keyboard_arrow_up_black);
                        }
                        switch (index) {
                            case 1:// 定位button
//                                mWorkThread = new WorkThread();
//                                mWorkThread.executeTask();
                                if (mTimerTask!= null){
                                    mTimerTask.cancel();
                                    mTimerTask = new MyTimerTask();
                                }else {
                                    mTimerTask = new MyTimerTask();
                                }
                                mTimer.schedule(mTimerTask, 0, 100);

                                // Toast.makeText(PresentationOnPlaneActivity.this, "定位",Toast.LENGTH_SHORT).show();
                                mImageViewFocusing.setVisibility(View.VISIBLE);
                                mExpandableSelector.collapse();
                                updateIconsFirstButtonResource(R.mipmap.ic_keyboard_arrow_up_black);

                                break;
                            case 2:// 旋转button
                                Toast.makeText(PresentationOnPlaneActivity.this, "旋转",Toast.LENGTH_SHORT).show();
                                mExpandableSelector.collapse();
                                updateIconsFirstButtonResource(R.mipmap.ic_keyboard_arrow_up_black);
                                break;
                            case 3:// 缩放button
                                mLayoutZoom.setVisibility(View.VISIBLE);
                                //Toast.makeText(PresentationOnPlaneActivity.this, "缩放",Toast.LENGTH_SHORT).show();
                                mExpandableSelector.collapse();
                                updateIconsFirstButtonResource(R.mipmap.ic_keyboard_arrow_up_black);
                                break;
                            /*
                            case 4:// 移动button
                                Toast.makeText(PresentationOnPlaneActivity.this, "移动",Toast.LENGTH_SHORT).show();
                                mExpandableSelector.collapse();
                                updateIconsFirstButtonResource(R.mipmap.ic_keyboard_arrow_up_black);
                                break;
                            */
                            default:
                        }
                    }
                });
                mExpandableSelector.setExpandableSelectorListener(new ExpandableSelectorListener() {
                    @Override public void onCollapse() {

                    }

                    @Override public void onExpand() {
                        updateIconsFirstButtonResource(R.mipmap.ic_keyboard_arrow_down_black);
                    }

                    @Override public void onCollapsed() {

                    }

                    @Override public void onExpanded() {
//                        mButtonZoomIn.setVisibility(View.INVISIBLE);
//                        mButtonZoomOut.setVisibility(View.INVISIBLE);
                    }
                });

            } else if (mMode == MODEL_PRESENTATION){

            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Synchronize against disconnecting while the service is being used in the OpenGL thread or
        // in the UI thread.
        synchronized (this) {
            try {
                mRenderer.getCurrentScene().clearFrameCallbacks();
                if (mTango != null) {
                    mTango.disconnectCamera(TangoCameraIntrinsics.TANGO_CAMERA_COLOR);
                    mTango.disconnect();
                    mTango = null;
                }
                // We need to invalidate the connected texture ID so that we cause a
                // re-connection in the OpenGL thread after resume.
                mConnectedTextureIdGlThread = INVALID_TEXTURE_ID;
                mIsConnected = false;
            } catch (TangoErrorException e) {
                Log.e(TAG, getString(R.string.exception_tango_error), e);
            }
        }
    }

    /**
     * 初始化Tango Service，作为一个普通的Android Service。
     * 我们在OnPause()或OnStop()时调用mTango.disconnect()，因此，
     * 我们需要在每次OnResume()或OnStart()时调用本方法，重建Tango Object
     */
    private void bindTangoService() {
        mTango = new Tango(PresentationOnPlaneActivity.this, new Runnable() {
            // Pass in a Runnable to be called from UI thread when Tango is ready; this Runnable
            // will be running on a new thread.
            // When Tango is ready, we can call Tango functions safely here only when there are no
            // UI thread changes involved.
            @Override
            public void run() {
                // Synchronize against disconnecting while the service is being used in the OpenGL
                // thread or in the UI thread.
                synchronized (PresentationOnPlaneActivity.this) {
                    try {
                        mConfig = setupTangoConfig(mTango);
                        mTango.connect(mConfig);
                        startupTango();
                        TangoSupport.initialize(mTango);
                        connectRenderer();
                        mIsConnected = true;
                        setDisplayRotation();
                    } catch (TangoOutOfDateException e) {
                        Log.e(TAG, getString(R.string.exception_out_of_date), e);
                        showsToastAndFinishOnUiThread(R.string.exception_out_of_date);
                    } catch (TangoErrorException e) {
                        Log.e(TAG, getString(R.string.exception_tango_error), e);
                        showsToastAndFinishOnUiThread(R.string.exception_tango_error);
                    } catch (TangoInvalidException e) {
                        Log.e(TAG, getString(R.string.exception_tango_invalid), e);
                        showsToastAndFinishOnUiThread(R.string.exception_tango_invalid);
                    }
                }
            }
        });
    }

    /**
     * 此处检查并申请app必要的系统权限
     * 本app目前仅用到Camera
     *
     * @return 如拥有权限，返回true，否则返回false。
     */
    private boolean checkAndRequestPermissions() {
        if (!hasCameraPermission()) {
            requestCameraPermission();
            return false;
        }
        return true;
    }

    /**
     * Check to see that we have the necessary permissions for this app.
     */
    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(this, CAMERA_PERMISSION) ==
                PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Request the necessary permissions for this app.
     */
    private void requestCameraPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, CAMERA_PERMISSION)) {
            showRequestPermissionRationale();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{CAMERA_PERMISSION},
                    CAMERA_PERMISSION_CODE);
        }
    }

    /**
     * If the user has declined the permission before, we have to explain that the app needs this
     * permission.
     */
    private void showRequestPermissionRationale() {
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setMessage("Java Plane Fitting Example requires camera permission")
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        ActivityCompat.requestPermissions(PresentationOnPlaneActivity.this,
                                new String[]{CAMERA_PERMISSION}, CAMERA_PERMISSION_CODE);
                    }
                })
                .create();
        dialog.show();
    }

    /**
     * 设置Tango Config对象，必须要保证，Tango对象在此之前已经建立
     */
    private TangoConfig setupTangoConfig(Tango tango) {
        // 使用默认的Config(motion tracking)，
        // 其他配置使用config.putBoolean()追加
        TangoConfig config = tango.getConfig(TangoConfig.CONFIG_TYPE_DEFAULT);
        // Low latency integration对于AR应用来说非常必要，
        // 因为它可以使虚拟物体与camera保持精确的位置关系
        config.putBoolean(TangoConfig.KEY_BOOLEAN_LOWLATENCYIMUINTEGRATION, true);
        config.putBoolean(TangoConfig.KEY_BOOLEAN_COLORCAMERA, true);
        config.putBoolean(TangoConfig.KEY_BOOLEAN_DEPTH, true);
        config.putInt(TangoConfig.KEY_INT_DEPTH_MODE, TangoConfig.TANGO_DEPTH_MODE_POINT_CLOUD);
        // Drift correction allows motion tracking to recover after it loses tracking.
        config.putBoolean(TangoConfig.KEY_BOOLEAN_DRIFT_CORRECTION, true);

        return config;
    }

    /**
     * 为Tango Service设置回调Listener，设置Tango Connection所需的参数。
     * 监听RGB camera 以及 Point Cloud的更新
     */
    private void startupTango() {
        // 不需要添加坐标FramePair，因为我们不使用Pose Data，只是初始化
        ArrayList<TangoCoordinateFramePair> framePairs = new ArrayList<TangoCoordinateFramePair>();
        mTango.connectListener(framePairs, new Tango.TangoUpdateCallback() {
            @Override
            public void onPoseAvailable(TangoPoseData pose) {
                // We are not using OnPoseAvailable for this app.
            }

            @Override
            public void onFrameAvailable(int cameraId) {
                // Check if the frame available is for the camera we want and update its frame
                // on the view.
                if (cameraId == TangoCameraIntrinsics.TANGO_CAMERA_COLOR) {
                    // Mark a camera frame as available for rendering in the OpenGL thread.
                    mIsFrameAvailableTangoThread.set(true);
                    mSurfaceView.requestRender();
                }
            }

            @Override
            public void onXyzIjAvailable(TangoXyzIjData xyzIj) {
                // We are not using onXyzIjAvailable for this app.
            }

            @Override
            public void onPointCloudAvailable(TangoPointCloudData pointCloud) {
                // Save the cloud and point data for later use.
                mPointCloudManager.updatePointCloud(pointCloud);
            }

            @Override
            public void onTangoEvent(TangoEvent event) {
                // We are not using OnPoseAvailable for this app.
            }
        });
    }


    /**
     * 将view和renderer连接到camera和callback
     */
    private void connectRenderer() {
        // 注册一个Rajawali frame回调，每得到一帧frame，更新一次Camera姿态
        // (@see https://github.com/Rajawali/Rajawali/wiki/Scene-Frame-Callbacks)
        mRenderer.getCurrentScene().registerFrameCallback(new ASceneFrameCallback() {
            @Override
            public void onPreFrame(long sceneTime, double deltaTime) {
                // 此方法在OpenGL renderer线程中被调用，
                // NOTE: This is called from the OpenGL render thread, after all the renderer
                // onRender callbacks have a chance to run and before scene objects are rendered
                // into the scene.

                try {
                    synchronized (PresentationOnPlaneActivity.this) {
                        // 如果没连接到Tango Service，以下代码不执行
                        if (!mIsConnected) {
                            return;
                        }

                        // 根据相机的内参计算投影矩阵
                        if (!mRenderer.isSceneCameraConfigured()) {
                            TangoCameraIntrinsics intrinsics =
                                    TangoSupport.getCameraIntrinsicsBasedOnDisplayRotation(
                                            TangoCameraIntrinsics.TANGO_CAMERA_COLOR,
                                            mDisplayRotation);
                            mRenderer.setProjectionMatrix(
                                    projectionMatrixFromCameraIntrinsics(intrinsics));
                        }

                        // 把相机的图像连接到OpenGL纹理上
                        // OpenGL纹理每次刷新后，Rajawali会重新生成一个不一样的纹理ID
                        if (mConnectedTextureIdGlThread != mRenderer.getTextureId()) {
                            mTango.connectTextureId(TangoCameraIntrinsics.TANGO_CAMERA_COLOR,
                                    mRenderer.getTextureId());
                            mConnectedTextureIdGlThread = mRenderer.getTextureId();
                            Log.d(TAG, "connected to texture id: " + mRenderer.getTextureId());
                        }

                        // 如果有新的一帧图像，用它来更新纹理
                        if (mIsFrameAvailableTangoThread.compareAndSet(true, false)) {
                            mRgbTimestampGlThread =
                                    mTango.updateTexture(TangoCameraIntrinsics.TANGO_CAMERA_COLOR);
                            if (mRemoveMask){
                                // 由于Camera已经准备好纹理，此时可以撤掉Mask，
                                // 并且只执行一次就可以
                                mHandlerMaskControl.sendEmptyMessage(EVENT_HIDE_ALL);
                                mRemoveMask = false;
                            }
                        }

                        // mRgbTimestampGlThread : 更新纹理的时间戳
                        // mCameraPoseTimestamp  : 最后一次获取位姿的时间戳
                        if (mRgbTimestampGlThread > mCameraPoseTimestamp) {
                            // 相机的frame更新之后，计算相机位姿。
                            TangoPoseData lastFramePose = TangoSupport.getPoseAtTime(
                                    mRgbTimestampGlThread,
                                    TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE,
                                    TangoPoseData.COORDINATE_FRAME_CAMERA_COLOR,
                                    TangoSupport.ENGINE_OPENGL,
                                    TangoSupport.ENGINE_OPENGL,
                                    mDisplayRotation);
                            if (lastFramePose.statusCode == TangoPoseData.POSE_VALID) {
                                // 更新相机的位姿
                                mRenderer.updateRenderCameraPose(lastFramePose);
                                mCameraPoseTimestamp = lastFramePose.timestamp;
                            } else {
                                // When the pose status is not valid, it indicates the tracking has
                                // been lost. In this case, we simply stop rendering.
                                //
                                // This is also the place to display UI to suggest that the user
                                // walk to recover tracking.
                                Log.w(TAG, "Can't get device pose at time: " +
                                        mRgbTimestampGlThread);
                            }
                            // mDepthTPlane是点击屏幕后根据点云和位姿计算出来的transform矩阵
                            if (mDepthTPlane != null) {
                                // 将Model的位置更新到计算出来的平面上
                                // 为了确定精确的位置，我们需要重新查询Area Description
                                // To make sure drift corrected pose is applied to the virtual
                                // object we need to re-query the Area Description to Depth camera
                                // at the time when the corresponding plane fitting
                                // measurement was acquired.
                                TangoSupport.MatrixTransformData openglTDepthArr =
                                        TangoSupport.getMatrixTransformAtTime(
                                                mPlanePlacedTimestamp,
                                                TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE,
                                                TangoPoseData.COORDINATE_FRAME_CAMERA_DEPTH,
                                                TangoSupport.ENGINE_OPENGL,
                                                TangoSupport.ENGINE_TANGO,
                                                TangoSupport.ROTATION_IGNORED);

                                if (openglTDepthArr.statusCode == TangoPoseData.POSE_VALID) {
                                    mRenderer.updateObjectPose(openglTDepthArr.matrix,
                                            mDepthTPlane);
                                }
                            }
                        }
                    }
                    // Avoid crashing the application due to unhandled exceptions.
                } catch (TangoErrorException e) {
                    Log.e(TAG, "Tango API call error within the OpenGL render thread", e);
                } catch (Throwable t) {
                    Log.e(TAG, "Exception on the OpenGL thread", t);
                }
            }

            @Override
            public void onPreDraw(long sceneTime, double deltaTime) {

            }

            @Override
            public void onPostFrame(long sceneTime, double deltaTime) {

            }

            @Override
            public boolean callPreFrame() {
                return true;
            }
        });
    }

    /**
     * 利用相机内参计算投影矩阵 for the Rajawali scene.
     */
    private static float[] projectionMatrixFromCameraIntrinsics(TangoCameraIntrinsics intrinsics) {
        // Uses frustumM to create a projection matrix taking into account calibrated camera
        // intrinsic parameter.
        // Reference: http://ksimek.github.io/2013/06/03/calibrated_cameras_in_opengl/
        float near = 0.1f;
        float far = 100;

        double cx = intrinsics.cx;
        double cy = intrinsics.cy;
        double width = intrinsics.width;
        double height = intrinsics.height;
        double fx = intrinsics.fx;
        double fy = intrinsics.fy;

        double xscale = near / fx;
        double yscale = near / fy;

        double xoffset = (cx - (width / 2.0)) * xscale;
        // Color camera's coordinates has y pointing downwards so we negate this term.
        double yoffset = -(cy - (height / 2.0)) * yscale;

        float m[] = new float[16];
        Matrix.frustumM(m, 0,
                (float) (xscale * -width / 2.0 - xoffset),
                (float) (xscale * width / 2.0 - xoffset),
                (float) (yscale * -height / 2.0 - yoffset),
                (float) (yscale * height / 2.0 - yoffset), near, far);
        return m;
    }

    /**
     * Display toast on UI thread.
     *
     * @param resId The resource id of the string resource to use. Can be formatted text.
     */
    private void showsToastAndFinishOnUiThread(final int resId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(PresentationOnPlaneActivity.this,
                        getString(resId), Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    /**
     * 根据设备的rotation设置camera的旋转
     */
    private void setDisplayRotation() {
        Display display = getWindowManager().getDefaultDisplay();
        mDisplayRotation = display.getRotation();

        // 根据rotation更新Camera纹理的坐标。
        // 必须在OpenGL线程中调用
        mSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                if (mIsConnected) {
                    mRenderer.updateColorCameraTextureUvGlThread(mDisplayRotation);
                }
            }
        });
    }

    /**
     * 利用Tango Support Library + 点云数据来计算平面。
     * 返回值：平面的transform矩阵
     */
    private float[] doFitPlane(float u, float v, double rgbTimestamp) {
        TangoPointCloudData pointCloud = mPointCloudManager.getLatestPointCloud();

        if (pointCloud == null) {
            return null;
        }

        TangoPoseData depthToColorPose = TangoSupport.getPoseAtTime(
                rgbTimestamp,
                TangoPoseData.COORDINATE_FRAME_CAMERA_DEPTH,
                TangoPoseData.COORDINATE_FRAME_CAMERA_COLOR,
                TangoSupport.ENGINE_TANGO,
                TangoSupport.ENGINE_TANGO,
                TangoSupport.ROTATION_IGNORED);
        if (depthToColorPose.statusCode != TangoPoseData.POSE_VALID) {
            Log.d(TAG, "Could not get a valid pose from depth camera"
                    + "to color camera at time " + rgbTimestamp);
            return null;
        }

        // 根据相机位姿计算平面模型，数据来源于深度相机
        TangoSupport.IntersectionPointPlaneModelPair intersectionPointPlaneModelPair =
                TangoSupport.fitPlaneModelNearPoint(pointCloud,
                        new double[] {0.0, 0.0, 0.0},
                        new double[] {0.0, 0.0, 0.0, 1.0},
                        u, v,
                        mDisplayRotation,
                        depthToColorPose.translation,
                        depthToColorPose.rotation);

        mPlanePlacedTimestamp = mRgbTimestampGlThread;
        return convertPlaneModelToMatrix(intersectionPointPlaneModelPair);
    }

    private float[] convertPlaneModelToMatrix(TangoSupport.IntersectionPointPlaneModelPair planeModel) {
        // 注意，深度相机的坐标系:
        // X - right
        // Y - down
        // Z - forward
        float[] up = new float[]{0, 1, 0, 0};
        float[] depthTPlane = matrixFromPointNormalUp(
                planeModel.intersectionPoint,
                planeModel.planeModel,
                up);
        return depthTPlane;
    }

    /**
     * Calculates a transformation matrix based on a point, a normal and the up gravity vector.
     * The coordinate frame of the target transformation will be a right handed system with Z+ in
     * the direction of the normal and Y+ up.
     */
    private float[] matrixFromPointNormalUp(double[] point, double[] normal, float[] up) {
        float[] zAxis = new float[]{(float) normal[0], (float) normal[1], (float) normal[2]};
        normalize(zAxis);
        float[] xAxis = crossProduct(up, zAxis);
        normalize(xAxis);
        float[] yAxis = crossProduct(zAxis, xAxis);
        normalize(yAxis);
        float[] m = new float[16];
        Matrix.setIdentityM(m, 0);
        m[0] = xAxis[0];
        m[1] = xAxis[1];
        m[2] = xAxis[2];
        m[4] = yAxis[0];
        m[5] = yAxis[1];
        m[6] = yAxis[2];
        m[8] = zAxis[0];
        m[9] = zAxis[1];
        m[10] = zAxis[2];
        m[12] = (float) point[0];
        m[13] = (float) point[1];
        m[14] = (float) point[2];
        return m;
    }

    /**
     * Normalize a vector.
     */
    private void normalize(float[] v) {
        double norm = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        v[0] /= norm;
        v[1] /= norm;
        v[2] /= norm;
    }

    /**
     * Cross product between two vectors following the right-hand rule.
     */
    private float[] crossProduct(float[] v1, float[] v2) {
        float[] result = new float[3];
        result[0] = v1[1] * v2[2] - v2[1] * v1[2];
        result[1] = v1[2] * v2[0] - v2[2] * v1[0];
        result[2] = v1[0] * v2[1] - v2[0] * v1[1];
        return result;
    }

    /**
     * 开启轮训服务
     */
    private void startLoopService(Context context, int seconds, Class<?> cls, String action){
        // 获取AlarmManager系统服务
        AlarmManager manager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        //包装需要执行Service的Intent
        Intent intent = new Intent(context, cls);
        intent.setAction(action);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        //触发服务的起始时间
        long triggerAtTime = SystemClock.elapsedRealtime();
        //使用AlarmManger的setRepeating方法设置定期执行的时间间隔（seconds秒）和需要执行的Service
        manager.setRepeating(AlarmManager.ELAPSED_REALTIME, triggerAtTime, seconds, pendingIntent);
    }

    /**
     * 停止轮询服务
     */
    private void stopLoopSevice(Context context, Class<?> cls, String action){
        AlarmManager manager = (AlarmManager) context
                .getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, cls);
        intent.setAction(action);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);
        //取消正在执行的服务
        manager.cancel(pendingIntent);
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

    private class WorkThread extends Thread{
        private Handler mHandler;
        private Looper mLooper;

        public WorkThread(){
            start();

        }

        public void run() {

            Looper.prepare();
            mLooper = Looper.myLooper();
            mHandler = new Handler(mLooper){
                @Override
                public void handleMessage(Message msg) {
                    mHandlerFindFocus.sendEmptyMessage(EVENT_GOT_FOCUS);
                    SystemClock.sleep(100);
                }
            };
            Looper.loop();
        }

        public void exit() {
            if (mLooper != null) {
                mLooper.quit();
                mLooper = null;
            }
        }

        public void executeTask() {
            if (mLooper == null || mHandler == null)
                return;
            Message msg = Message.obtain();
            mHandler.sendMessage(msg);
        }
    }

    private class MyTimerTask extends TimerTask{
        @Override
        public void run() {
            try {
                // 在手机上Touch的点，利用点云数据找到一个适配的平面
                // Synchronize against concurrent access to the RGB timestamp in the OpenGL thread
                // and a possible service disconnection due to an onPause event.
                synchronized (this) {
                    mDepthTPlane = doFitPlane(0.5f, 0.5f, mRgbTimestampGlThread);
                    mHandlerFindFocus.sendEmptyMessage(EVENT_GOT_FOCUS);
                    //mFocusRenderer.setvMatrix(mDepthTPlane);
                    mHandlerMaskControl.sendEmptyMessage(EVENT_SHOW_PUTMODEL);
                }

            } catch (TangoException t) {
                mHandlerFindFocus.sendEmptyMessage(EVENT_LOSE_FOCUS);
                //mFocusRenderer.setvMatrix(lMatrix);

            } catch (SecurityException t) {
                Log.e(TAG, getString(R.string.failed_permissions), t);
            }
        }
    }
}
