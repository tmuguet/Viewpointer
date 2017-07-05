package com.tmuguet.viewpointer;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Size;
import android.os.Build;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.List;

/**
 * Created by tmuguet on 16/08/2014.
 */
public class CameraView extends SurfaceView implements
        SurfaceHolder.Callback {

    private static final String TAG = "CameraView";
    Camera mCamera;
    SurfaceHolder mHolder;
    Activity mActivity;

    public CameraView(Context context, Activity activity) {
        super(context);

        mActivity = activity;
        mHolder = getHolder();

        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        mHolder.addCallback(this);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        mCamera = Camera.open();

        // Set Display orientation
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            CameraInfo info = new CameraInfo();
            Camera.getCameraInfo(CameraInfo.CAMERA_FACING_BACK, info);

            int rotation = mActivity.getWindowManager().getDefaultDisplay()
                    .getRotation();
            int degrees = 0;
            switch (rotation) {
                case Surface.ROTATION_0:
                    degrees = 0;
                    break;
                case Surface.ROTATION_90:
                    degrees = 90;
                    break;
                case Surface.ROTATION_180:
                    degrees = 180;
                    break;
                case Surface.ROTATION_270:
                    degrees = 270;
                    break;
            }

            mCamera.setDisplayOrientation((info.orientation - degrees + 360) % 360);
        }

        try {
            mCamera.setPreviewDisplay(mHolder);
        } catch (IOException e) {
            Log.e(TAG, "IOException", e);
        }
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
        Camera.Parameters params = mCamera.getParameters();

        List<Size> supportedSizes = params.getSupportedPreviewSizes();
        for (Size s : supportedSizes) {
            if (s.height <= height && s.width <= width) {
                params.setPreviewSize(s.width, s.height);
                break;
            }
        }
        params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
        params.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);
        params.setSceneMode(Camera.Parameters.SCENE_MODE_LANDSCAPE);

        mCamera.setParameters(params);
        mCamera.startPreview();
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        mCamera.stopPreview();
        mCamera.release();
    }
}
