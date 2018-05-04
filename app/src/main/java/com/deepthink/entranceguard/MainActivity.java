package com.deepthink.entranceguard;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.deepthink.entranceguard.utils.MTCNNFaceInfos;
import com.deepthink.entranceguard.utils.MTCNNUtils;
import com.deepthink.entranceguard.utils.SimpleStorage;
import com.deepthink.entranceguard.view.FaceDetectView;

import java.util.Vector;

public class MainActivity extends Activity implements View.OnClickListener, FaceDetectView.OnCameraListener, FaceDetectView.OnPictureSaved {
    private final String TAG = this.getClass().getName();
    private final static int MSG_CATCH_FACE = 0x0000;
    private final static int MSG_MATCH_FACE = 0x0001;

    private FaceDetectView mCameraView = null;
    private ImageView mRegister = null;
    private ImageView mBorder = null;

    private SimpleStorage mData = null;
    private UIHandler mHandler = new UIHandler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mCameraView = (FaceDetectView) findViewById(R.id.detect_view);
        mCameraView.setCameraListener(this);
        mCameraView.setSavedCallback(this);

        mRegister = (ImageView) findViewById(R.id.register);
        mRegister.setOnClickListener(this);

        mBorder = (ImageView) findViewById(R.id.border);

        mData = new SimpleStorage((Application) getApplication());
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.register:
                mCameraView.takePicture();
                break;
        }
    }

    @Override
    public Object onPreview(Bitmap src, byte[] data, int width, int height) {
        Matrix matrix = new Matrix();
        matrix.setRotate(270);
        Bitmap bmp = Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), matrix, true);

        MTCNNFaceInfos faceInfos = MTCNNUtils.getInstance().faceDetect(bmp);
        Rect[] rects = new Rect[faceInfos.list.size()];

        int mainFaceIndex = -1;
        int faceArea = 0;
        for (int i = 0; i < faceInfos.list.size(); i++) {
            MTCNNFaceInfos.MTCNNFaceInfo faceInfo = faceInfos.list.get(i);
            rects[i] = new Rect(faceInfo.left, faceInfo.top, faceInfo.right, faceInfo.bottom);
            int area = rects[i].width() * rects[i].height();
            if (faceArea < area) {
                faceArea = area;
                mainFaceIndex = i;
            }
            Log.e(TAG, "onPreview: face angle " + faceInfo.angle);
        }

        if (mainFaceIndex >= 0) {
            Rect rect = rects[mainFaceIndex];
            Message msg = new Message();
            msg.what = MSG_CATCH_FACE;
            msg.arg1 = faceArea;
            if (faceArea > 0) msg.obj = rect;
            mHandler.sendMessage(msg);
        }

        int faceWidth = rects[mainFaceIndex].width();
        int faceHeight = rects[mainFaceIndex].height();
        Bitmap face_bitmap = Bitmap.createBitmap(faceWidth, faceHeight, Bitmap.Config.ARGB_8888);
        Canvas face_canvas = new Canvas(face_bitmap);
        face_canvas.drawBitmap(bmp, rects[mainFaceIndex], new Rect(0, 0, faceWidth, faceHeight), null);

        float[] features = MTCNNUtils.getInstance().getFaceFeatures(face_bitmap);

        Vector<SimpleStorage.FaceRegister> registers = mData.getFaces();
        int matchedIndex = 0;
        float matchedValue = 0;
        for (int i = 0; i < registers.size(); i++) {
            SimpleStorage.FaceRegister one = registers.get(i);
            float val = 0;
            for (int j = 0; j < features.length && j < one.features.length; j++) {
                val += features[j] * one.features[j];
            }
            if (matchedValue < val) {
                matchedValue = val;
                matchedIndex = i;
            }
        }
        if (matchedValue > 0.6) {
            Bitmap img = registers.get(matchedIndex).img;
            Message msg = new Message();
            msg.what = MSG_MATCH_FACE;
            msg.obj = img;
            mHandler.sendMessage(msg);
        }

        return null;
    }

    @Override
    public void onPictureSaved(Bitmap src) {
        Matrix matrix = new Matrix();
        matrix.setRotate(270);
        Bitmap bmp = Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), matrix, true);

        MTCNNFaceInfos faceInfos = MTCNNUtils.getInstance().faceDetect(bmp);
        if (faceInfos.list.size() > 0) {
            Rect[] rects = new Rect[faceInfos.list.size()];

            int mainFaceIndex = 0;
            int faceArea = 0;
            for (int i = 0; i < faceInfos.list.size(); i++) {
                MTCNNFaceInfos.MTCNNFaceInfo faceInfo = faceInfos.list.get(i);
                rects[i] = new Rect(faceInfo.left, faceInfo.top, faceInfo.right, faceInfo.bottom);
                int area = rects[i].width() * rects[i].height();
                if (faceArea < area) {
                    faceArea = area;
                    mainFaceIndex = i;
                }
                Log.e(TAG, "onPreview: face angle " + faceInfo.angle);
            }
            int faceWidth = rects[mainFaceIndex].width();
            int faceHeight = rects[mainFaceIndex].height();
            Bitmap face_bitmap = Bitmap.createBitmap(faceWidth, faceHeight, Bitmap.Config.ARGB_8888);
            Canvas face_canvas = new Canvas(face_bitmap);
            face_canvas.drawBitmap(bmp, rects[mainFaceIndex], new Rect(0, 0, faceWidth, faceHeight), null);

            float[] features = MTCNNUtils.getInstance().getFaceFeatures(face_bitmap);
            mData.save(face_bitmap, features);
        }
        else {
            Toast.makeText(MainActivity.this, "未检测到人脸", Toast.LENGTH_SHORT).show();
        }

    }

    class UIHandler extends Handler {
        @Override
        public void handleMessage(final Message msg) {
            super.handleMessage(msg);
            if (msg.what == MSG_CATCH_FACE) {
                float area = msg.arg1;
                if (area > 0) {
                    Rect rect = (Rect) msg.obj;
                    ViewGroup.MarginLayoutParams margin9 = new ViewGroup.MarginLayoutParams(
                            mBorder.getLayoutParams());
                    margin9.setMargins(rect.left, rect.top, 0, 0);
                    RelativeLayout.LayoutParams layoutParams9 = new RelativeLayout.LayoutParams(margin9);
                    layoutParams9.height = rect.height();
                    layoutParams9.width = rect.width();
                    mBorder.setLayoutParams(layoutParams9);
                    mBorder.setVisibility(View.VISIBLE);
                }
                else {
                    mBorder.setVisibility(View.INVISIBLE);
                }
            } else if (msg.what == MSG_MATCH_FACE) {

            }
        }
    }
}
