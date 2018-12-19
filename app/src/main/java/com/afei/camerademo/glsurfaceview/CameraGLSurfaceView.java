package com.afei.camerademo.glsurfaceview;

import android.app.Activity;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import com.afei.camerademo.camera.CameraProxy;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class CameraGLSurfaceView extends GLSurfaceView implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {

    private static final String TAG = "CameraGLSurfaceView";
    private CameraProxy mCameraProxy;
    private SurfaceTexture mSurfaceTexture;
    private CameraDrawer mDrawer;
    private int mRatioWidth = 0;
    private int mRatioHeight = 0;
    private int mTextureId = -1;

    public CameraGLSurfaceView(Context context) {
        this(context, null);
    }

    public CameraGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        mCameraProxy = new CameraProxy((Activity) context);
        setEGLContextClientVersion(3);
        setRenderer(this);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        mTextureId = OpenGLUtils.getExternalOESTextureID();
        mSurfaceTexture = new SurfaceTexture(mTextureId);
        mSurfaceTexture.setOnFrameAvailableListener(this);
        mCameraProxy.openCamera();
        mDrawer = new CameraDrawer();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.d(TAG, "onSurfaceChanged. thread: " + Thread.currentThread().getName());
        Log.d(TAG, "onSurfaceChanged. width: " + width + ", height: " + height);
        int previewWidth = mCameraProxy.getPreviewWidth();
        int previewHeight = mCameraProxy.getPreviewHeight();
        if (width > height) {
            setAspectRatio(previewWidth, previewHeight);
        } else {
            setAspectRatio(previewHeight, previewWidth);
        }
        GLES20.glViewport(0, 0, width, height);
        mCameraProxy.startPreview(mSurfaceTexture);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClearColor(0, 0, 0, 0);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        mSurfaceTexture.updateTexImage();
        mDrawer.draw(mTextureId);
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        requestRender();
    }

    @Override
    public void onPause() {
        super.onPause();
        mCameraProxy.stopPreview();
    }

    @Override
    public void onResume() {
        super.onResume();
        bringToFront();
    }

    public void setAspectRatio(int width, int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Size cannot be negative.");
        }
        mRatioWidth = width;
        mRatioHeight = height;
        post(new Runnable() {
            @Override
            public void run() {
                requestLayout(); // must run in UI thread
            }
        });
    }

    public CameraProxy getCameraProxy() {
        return mCameraProxy;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (0 == mRatioWidth || 0 == mRatioHeight) {
            setMeasuredDimension(width, height);
        } else {
            if (width < height * mRatioWidth / mRatioHeight) {
                setMeasuredDimension(width, width * mRatioHeight / mRatioWidth);
            } else {
                setMeasuredDimension(height * mRatioWidth / mRatioHeight, height);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // 点击聚焦
                mCameraProxy.focusOnPoint((int) event.getX(), (int) event.getY(), getWidth(), getHeight());
                break;
            default:
                break;
        }
        return super.onTouchEvent(event);
    }

    public SurfaceTexture getSurfaceTexture() {
        return mSurfaceTexture;
    }
}
