package com.deepthink.entranceguard.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.deepthink.entranceguard.MainActivity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Created by yanbo on 2018/5/3.
 */

public class FaceDetectView extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {

    private static final String TAG = "FaceDetectView";
    protected Context mContext;
    private SurfaceHolder mSurfaceHolder;
    private Camera mCamera;

    private OnPictureSaved onPictureSaved = null;
    private OnCameraListener onCameraListener = null;

    private boolean mTakePicture = false;

    public FaceDetectView(Context context) {
        super(context);
        initFaceDetectView(context);
    }

    public FaceDetectView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initFaceDetectView(context);
    }

    private void initFaceDetectView(Context context) {
        this.mContext = context;
        setFocusable(true);

        mCamera = getCameraInstance();
        mSurfaceHolder = getHolder();
        mSurfaceHolder.addCallback(this);
    }

    public static Camera getCameraInstance(){
//        Camera c = null;
//        Camera.CameraInfo info= new Camera.CameraInfo();
//        int count= Camera.getNumberOfCameras();
//        for (int i = 0; i < count; i++) {
//            Camera.getCameraInfo(i, info);
//            if (info.facing==Camera.CameraInfo.CAMERA_FACING_FRONT) {
//                try {
//                    c = Camera.open(i);
//                } catch (RuntimeException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
        Camera c = Camera.open();
        return c;
    }

    public void takePicture() {
        mTakePicture = true;
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        Log.d(TAG, "surfaceCreated");

        try {
            mCamera.setPreviewDisplay(surfaceHolder);
            mCamera.startPreview();
        } catch (IOException e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        Log.d(TAG, "surfaceChanged");
        if (mSurfaceHolder.getSurface() == null){
            return;
        }

        try {
            mCamera.stopPreview();
        } catch (Exception e){
            e.printStackTrace();
        }

        try {
            mCamera.setPreviewDisplay(mSurfaceHolder);
            mCamera.setDisplayOrientation(90);
            mCamera.setPreviewCallback(this);
            mCamera.startPreview();
        } catch (Exception e){
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        Camera.Size size = camera.getParameters().getPreviewSize();
        try{
            YuvImage image = new YuvImage(data, ImageFormat.NV21, size.width, size.height, null);
            if(image!=null){
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                image.compressToJpeg(new Rect(0, 0, size.width, size.height), 80, stream);
                Bitmap bmp = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());
                stream.close();
                if (onCameraListener != null) {
                    onCameraListener.onPreview(bmp, data, bmp.getWidth(), bmp.getHeight());
                }

                if (mTakePicture) {
                    if (onPictureSaved != null) {
                        onPictureSaved.onPictureSaved(bmp);
                    }
                    mTakePicture = false;
                }
            }
        }catch(Exception ex){
            Log.e("Sys","Error:"+ex.getMessage());
        }
    }

    public void setSavedCallback(OnPictureSaved cb) {
        onPictureSaved = cb;
    }

    public interface OnPictureSaved {
        void onPictureSaved(Bitmap bmp);
    }

    public void setCameraListener(OnCameraListener cb) {
        onCameraListener = cb;
    }

    public interface OnCameraListener {
        Object onPreview(Bitmap bmp, byte[] data, int width, int height);
    }
}
